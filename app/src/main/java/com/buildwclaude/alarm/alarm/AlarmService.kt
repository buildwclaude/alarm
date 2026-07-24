package com.buildwclaude.alarm.alarm

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Foreground service that actually makes noise. It plays the alarm tone on the alarm
 * stream (looping, unaffected by silent mode), vibrates, holds a wake lock, shows the
 * full-screen notification, and auto-snoozes once after 10 minutes of no interaction.
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AlarmContract.ACTION_SNOOZE -> { doSnooze(userInitiated = true); return START_NOT_STICKY }
            AlarmContract.ACTION_DISMISS -> { doDismiss(); return START_NOT_STICKY }
            else -> startRinging(intent)
        }
        return START_STICKY
    }

    private fun startRinging(intent: Intent?) {
        val id = intent?.getIntExtra(AlarmContract.EXTRA_ALARM_ID, -1) ?: -1
        autoSnoozeCount = intent?.getIntExtra(AlarmContract.EXTRA_AUTO_SNOOZE_COUNT, 0) ?: 0
        currentAlarmId = id

        // Must call startForeground quickly; use a placeholder label first, refine after DB read.
        goForeground(id, "Alarm")

        Active.set(id)

        scope.launch {
            val alarm = try {
                AlarmRepository.get(applicationContext).getById(id)
            } catch (t: Throwable) {
                Log.e(TAG, "Could not load alarm $id", t); null
            }
            snoozeMinutes = alarm?.snoozeMinutes ?: 5
            withMain {
                goForeground(id, alarm?.label ?: "Alarm")
                startSoundAndVibration(alarm)
                acquireWakeLock()
                launchAlarmActivity(id)
                armTimeout()
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
            // 0.4s off, 0.6s buzz, repeating.
            val pattern = longArrayOf(0, 600, 400)
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
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RiddleAlarm::ringing",
            ).apply { acquire(AlarmContract.RING_TIMEOUT_MS + 60_000L) }
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
            // Full-screen intent on the notification is the fallback path.
            Log.e(TAG, "Could not launch alarm activity", t)
        }
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

    private fun doSnooze(userInitiated: Boolean) {
        val id = currentAlarmId
        val minutes = snoozeMinutes
        val nextCount = if (userInitiated) autoSnoozeCount else autoSnoozeCount + 1
        try {
            val triggerAt = LocalDateTime.now().plusMinutes(minutes.toLong())
                .withSecond(0).withNano(0)
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
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

    private fun stopEverything() {
        Active.clear()
        timeoutHandler.removeCallbacksAndMessages(null)
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        // Belt and braces: make sure nothing keeps running.
        runCatching { mediaPlayer?.release() }
        runCatching { vibrator?.cancel() }
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        Active.clear()
    }

    private fun withMain(block: () -> Unit) =
        Handler(Looper.getMainLooper()).post { runCatching { block() }.onFailure { Log.e(TAG, "main block", it) } }

    /** Tiny process-wide flag so the UI knows an alarm is currently ringing. */
    object Active {
        @Volatile var alarmId: Int = -1
            private set
        val isRinging: Boolean get() = alarmId != -1
        fun set(id: Int) { alarmId = id }
        fun clear() { alarmId = -1 }
    }

    companion object {
        private const val TAG = "AlarmService"

        fun sendAction(context: android.content.Context, action: String) {
            val i = Intent(context, AlarmService::class.java).apply { this.action = action }
            runCatching { context.startService(i) }
        }
    }
}
