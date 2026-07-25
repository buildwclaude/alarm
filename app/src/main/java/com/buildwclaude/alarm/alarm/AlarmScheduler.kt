package com.buildwclaude.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.buildwclaude.alarm.MainActivity
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.data.AlarmRepository
import java.time.LocalDateTime

/**
 * Schedules and cancels alarms with [AlarmManager.setAlarmClock] — the most reliable
 * option on One UI, and the one that shows the status-bar alarm icon. Everything here is
 * defensive: a missing permission or a thrown exception is logged, never crashes the app.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** True unless the user denied the Android 12+ "exact alarm" permission. */
    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

    fun schedule(alarm: AlarmEntity) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val triggerAt = AlarmTime.nextTrigger(alarm)
        scheduleAt(alarm.id, triggerAt, isSnooze = false, autoSnoozeCount = 0)
        Log.i(TAG, "Scheduled alarm ${alarm.id} for $triggerAt")
    }

    fun scheduleSnooze(alarmId: Int, triggerAt: Long, autoSnoozeCount: Int) {
        scheduleAt(alarmId, triggerAt, isSnooze = true, autoSnoozeCount = autoSnoozeCount)
        Log.i(TAG, "Snooze scheduled for alarm $alarmId at $triggerAt")
    }

    /**
     * Re-arm the next occurrence of a repeating alarm right after it has fired. Uses a base
     * time one minute in the future so we can never accidentally reschedule (and instantly
     * re-fire) the occurrence that just went off.
     */
    fun scheduleNextAfterFire(alarm: AlarmEntity) {
        if (!alarm.enabled || alarm.repeatMask == 0) return
        val triggerAt = AlarmTime.nextTrigger(alarm, from = LocalDateTime.now().plusMinutes(1))
        scheduleAt(alarm.id, triggerAt, isSnooze = false, autoSnoozeCount = 0)
        Log.i(TAG, "Re-armed repeating alarm ${alarm.id} for $triggerAt")
    }

    private fun scheduleAt(alarmId: Int, triggerAt: Long, isSnooze: Boolean, autoSnoozeCount: Int) {
        try {
            val operation = firePendingIntent(alarmId, isSnooze, autoSnoozeCount)
            val show = PendingIntent.getActivity(
                context, alarmId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val info = AlarmManager.AlarmClockInfo(triggerAt, show)
            alarmManager.setAlarmClock(info, operation)
        } catch (se: SecurityException) {
            // Exact-alarm permission was revoked between the check and now.
            Log.e(TAG, "No permission to schedule exact alarm $alarmId", se)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to schedule alarm $alarmId", t)
        }
    }

    fun cancel(alarmId: Int) {
        runCatching {
            alarmManager.cancel(firePendingIntent(alarmId, isSnooze = false, autoSnoozeCount = 0))
            alarmManager.cancel(firePendingIntent(alarmId, isSnooze = true, autoSnoozeCount = 0))
        }.onFailure { Log.e(TAG, "Failed to cancel alarm $alarmId", it) }
    }

    /** Re-arm every enabled alarm. Called after boot, time-zone change and app update. */
    suspend fun rescheduleAll() {
        val repo = AlarmRepository.get(context)
        repo.getEnabled().forEach { schedule(it) }
        Log.i(TAG, "Rescheduled all enabled alarms")
    }

    private fun firePendingIntent(
        alarmId: Int,
        isSnooze: Boolean,
        autoSnoozeCount: Int,
    ): PendingIntent {
        val requestCode = if (isSnooze) alarmId + AlarmContract.SNOOZE_REQUEST_OFFSET else alarmId
        val kind = if (isSnooze) "snooze" else "fire"
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmContract.ACTION_FIRE
            // Unique data per (alarm, kind) so PendingIntents don't collide.
            data = Uri.parse("riddle-alarm://$kind/$alarmId")
            putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmContract.EXTRA_IS_SNOOZE, isSnooze)
            putExtra(AlarmContract.EXTRA_AUTO_SNOOZE_COUNT, autoSnoozeCount)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "AlarmScheduler"
    }
}
