package com.buildwclaude.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.buildwclaude.alarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the AlarmManager broadcast when an alarm (or its snooze) is due, re-arms the
 * next occurrence, then starts the foreground ringing service.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmContract.EXTRA_ALARM_ID, -1)
        if (id == -1) return
        val isSnooze = intent.getBooleanExtra(AlarmContract.EXTRA_IS_SNOOZE, false)
        val autoSnoozeCount = intent.getIntExtra(AlarmContract.EXTRA_AUTO_SNOOZE_COUNT, 0)
        Log.i(TAG, "Alarm fired: id=$id snooze=$isSnooze autoSnoozeCount=$autoSnoozeCount")

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val repo = AlarmRepository.get(context)
                val alarm = repo.getById(id)
                if (alarm == null) {
                    Log.w(TAG, "Alarm $id no longer exists; ignoring")
                    return@launch
                }
                // A normal (non-snooze) firing re-arms repeats and disables one-offs.
                if (!isSnooze) {
                    if (!alarm.enabled) {
                        Log.w(TAG, "Alarm $id is disabled; ignoring")
                        return@launch
                    }
                    val scheduler = AlarmScheduler(context)
                    if (alarm.repeatMask != 0) {
                        scheduler.schedule(alarm) // computes the next selected weekday
                    } else {
                        repo.setEnabled(id, false) // one-off: turn it off in the list
                    }
                }
                startRinging(context, id, autoSnoozeCount)
            } catch (t: Throwable) {
                Log.e(TAG, "Error handling alarm $id", t)
            } finally {
                pending.finish()
            }
        }
    }

    private fun startRinging(context: Context, id: Int, autoSnoozeCount: Int) {
        val svc = Intent(context, AlarmService::class.java).apply {
            action = AlarmContract.ACTION_FIRE
            putExtra(AlarmContract.EXTRA_ALARM_ID, id)
            putExtra(AlarmContract.EXTRA_AUTO_SNOOZE_COUNT, autoSnoozeCount)
        }
        try {
            ContextCompat.startForegroundService(context, svc)
        } catch (t: Throwable) {
            // On Android 12+ FGS starts can be restricted; alarm-triggered starts are
            // normally exempt, but never let a failure here crash the process.
            Log.e(TAG, "Could not start ringing service for $id", t)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.e(TAG, "FGS start may have been blocked by the system")
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
