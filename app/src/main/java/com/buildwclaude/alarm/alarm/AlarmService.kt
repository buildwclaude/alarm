package com.buildwclaude.alarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.data.AlarmRepository
import com.buildwclaude.alarm.data.Riddle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Foreground service that makes noise until the riddle is solved. It plays the alarm tone
 * on the alarm stream (looping, unaffected by silent mode), vibrates, holds a wake lock,
 * shows the full-screen notification, keeps the alarm screen up (anti-cheat), and
 * auto-snoozes once after 10 minutes of no interaction.
 *
 * Every external entry point is wrapped so an exception can never silence the alarm.
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentAlarmId: Int = -1
    private var autoSnoozeCount: Int = 0
    private var snoozeMinutes: Int = 5
    private var soundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AlarmContract.ACTION_SNOOZE -> { doSnooze(userInitiated = true); return START_NOT_STICKY }
            AlarmContract.ACTION_DISMISS -> { doDismiss(); return START_NOT_STICKY }
            AlarmContract.ACTION_SILENCE -> { silenceButStayAlive(); return START_STICKY }
            AlarmContract.ACTION_RELAUNCH -> { relaunchActivitySoon(); return START_STICKY }
            else -> startRinging(intent)
        }
        return START_STICKY
    }

    private fun startRinging(intent: Intent?) {
        // Resume from persisted state if this is a sticky restart with no intent.
        val id = intent?.getIntExtra(AlarmContract.EXTRA_ALARM_ID, -1)?.takeIf { it != -1 }
            ?: RingState.id(this)
        autoSnoozeCount = intent?.getIntExtra(AlarmContract.EXTRA_AUTO_SNOOZE_COUNT, -1)
            ?.takeIf { it != -1 } ?: RingState.autoSnoozeCount(this)
        currentAlarmId = id

        // Must call startForeground quickly; placeholder label first, refined after DB read.
        goForeground(id, "Alarm")

        if (id == -1) {
            Log.w(TAG, "No alarm id to ring; stopping")
            stopEverything()
            return
        }

        RingState.set(this, id, autoSnoozeCount)
        Active.begin(id)

        scope.launch {
            val alarm = try {
                AlarmRepository.get(applicationContext).getById(id)
            } catch (t: Throwable) {
                Log.e(TAG, "Could not load alarm $id", t); null
            }
            snoozeMinutes = alarm?.snoozeMinutes ?: 5
            Active.snoozeMinutes = snoozeMinutes
            withMain {
                goForeground(id, alarm?.label ?: "Alarm")
                if (!soundStarted) {
                    startSoundAndVibration(alarm)
                    acquireWakeLock()
                    armTimeout()
                    soundStarted = true
                }
                launchAlarmActivity(id)
            }
        }
    }

    private fun goForeground(id: Int, label: String) {
        val notification = AlarmNotifications.buildRingingNotification(this, id, label)
        try {
            ServiceCompat.startForeground(
                this,
                AlarmContract.RINGING_NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
        }
    }

    private fun startSoundAndVibration(alarm: AlarmEntity?) {
        val key = alarm?.soundKey ?: AlarmSound.DEFAULT_KEY
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                val afd = resources.openRawResourceFd(AlarmSound.resIdFor(key))
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra"); true
                }
                prepare()
                start()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start alarm sound", t)
        }

        if (alarm?.vibrate != false) startVibration()
    }

    private fun startVibration() {
        try {
            val vib = obtainVibrator()
            vibrator = vib
            val pattern = longArrayOf(0, 600, 400) // buzz 0.6s, pause 0.4s, repeat
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Vibration failed", t)
        }
    }

    private fun obtainVibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RiddleAlarm::ringing")
                .apply { acquire(AlarmContract.RING_TIMEOUT_MS + 60_000L) }
        } catch (t: Throwable) {
            Log.e(TAG, "Wake lock failed", t)
        }
    }

    private fun launchAlarmActivity(id: Int) {
        try {
            val i = Intent(this, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(AlarmContract.EXTRA_ALARM_ID, id)
            }
            startActivity(i)
        } catch (t: Throwable) {
            Log.e(TAG, "Could not launch alarm activity (full-screen intent is the fallback)", t)
        }
    }

    private fun relaunchActivitySoon() {
        if (!Active.isActive) return
        Handler(Looper.getMainLooper()).postDelayed({
            if (Active.isActive) launchAlarmActivity(currentAlarmId)
        }, 400)
    }

    private fun armTimeout() {
        timeoutHandler.removeCallbacksAndMessages(null)
        timeoutHandler.postDelayed({ onTimeout() }, AlarmContract.RING_TIMEOUT_MS)
    }

    private fun onTimeout() {
        if (autoSnoozeCount < AlarmContract.MAX_AUTO_SNOOZES) {
            Log.i(TAG, "Ring timeout — auto-snoozing alarm $currentAlarmId")
            doSnooze(userInitiated = false)
        } else {
            Log.i(TAG, "Ring timeout — giving up on alarm $currentAlarmId")
            doDismiss()
        }
    }

    /** Riddle solved (or skipped): stop the noise but keep the service + screen alive. */
    private fun silenceButStayAlive() {
        timeoutHandler.removeCallbacksAndMessages(null)
        stopSound()
        Active.markSolved()
        // Safety net: if the user never taps Snooze/Dismiss, clean up rather than linger.
        timeoutHandler.postDelayed({ doDismiss() }, 2 * 60 * 1000L)
    }

    private fun doSnooze(userInitiated: Boolean) {
        val id = currentAlarmId
        val minutes = snoozeMinutes
        val nextCount = if (userInitiated) autoSnoozeCount else autoSnoozeCount + 1
        try {
            val triggerAt = LocalDateTime.now().plusMinutes(minutes.toLong())
                .withSecond(0).withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            AlarmScheduler(applicationContext).scheduleSnooze(id, triggerAt, nextCount)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to schedule snooze for $id", t)
        }
        stopEverything()
    }

    private fun doDismiss() {
        // Repeats were already re-armed at fire time; one-offs were disabled. Just stop.
        stopEverything()
    }

    private fun stopSound() {
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun stopEverything() {
        Active.end()
        RingState.clear(this)
        timeoutHandler.removeCallbacksAndMessages(null)
        stopSound()
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // User swiped the app away while an alarm is active — re-show the alarm screen.
        if (Active.isActive) relaunchActivitySoon()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        runCatching { mediaPlayer?.release() }
        runCatching { vibrator?.cancel() }
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
    }

    private fun withMain(block: () -> Unit) =
        Handler(Looper.getMainLooper()).post {
            runCatching { block() }.onFailure { Log.e(TAG, "main block", it) }
        }

    /**
     * Process-wide state about the current firing so the UI (and relaunch logic) can read it.
     * [isActive] stays true from fire until snooze/dismiss — including after the riddle is
     * solved — so the alarm screen can't be escaped by leaving the app.
     */
    object Active {
        @Volatile var alarmId: Int = -1; private set
        @Volatile var isActive: Boolean = false; private set
        @Volatile var solved: Boolean = false; private set
        @Volatile var ringStartElapsed: Long = 0L; private set
        @Volatile var snoozeMinutes: Int = 5

        /** The riddle chosen for this firing, kept stable across activity relaunches. */
        @Volatile var riddle: Riddle? = null

        val isRinging: Boolean get() = isActive && !solved

        fun begin(id: Int) {
            alarmId = id; isActive = true; solved = false
            ringStartElapsed = SystemClock.elapsedRealtime(); riddle = null
        }

        fun markSolved() { solved = true }

        fun end() {
            isActive = false; solved = false; alarmId = -1; riddle = null
        }
    }

    companion object {
        private const val TAG = "AlarmService"

        fun sendAction(context: Context, action: String) {
            val i = Intent(context, AlarmService::class.java).apply { this.action = action }
            runCatching { context.startService(i) }
        }
    }
}
