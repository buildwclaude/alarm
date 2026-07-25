package com.buildwclaude.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receives the AlarmManager broadcast when an alarm (or its snooze) is due and starts the
 * ringing service *immediately and synchronously*.
 *
 * Why synchronous: on Android 12+ an app only gets a brief window to start a foreground
 * service after an exact alarm fires. Doing a database read first (as an earlier version
 * did) could push the service start outside that window and silently drop the alarm. All
 * database work (re-arming repeats, disabling one-offs) now happens inside the long-lived
 * service instead.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmContract.EXTRA_ALARM_ID, -1)
        if (id == -1) return
        val isSnooze = intent.getBooleanExtra(AlarmContract.EXTRA_IS_SNOOZE, false)
        val autoSnoozeCount = intent.getIntExtra(AlarmContract.EXTRA_AUTO_SNOOZE_COUNT, 0)
        Log.i(TAG, "Alarm fired: id=$id snooze=$isSnooze autoSnoozeCount=$autoSnoozeCount")

        val svc = Intent(context, AlarmService::class.java).apply {
            action = AlarmContract.ACTION_FIRE
            putExtra(AlarmContract.EXTRA_ALARM_ID, id)
            putExtra(AlarmContract.EXTRA_IS_SNOOZE, isSnooze)
            putExtra(AlarmContract.EXTRA_AUTO_SNOOZE_COUNT, autoSnoozeCount)
        }
        try {
            ContextCompat.startForegroundService(context, svc)
        } catch (t: Throwable) {
            Log.e(TAG, "Could not start ringing service for $id", t)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
