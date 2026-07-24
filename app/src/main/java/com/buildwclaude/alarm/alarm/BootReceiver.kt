package com.buildwclaude.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms all enabled alarms after the phone reboots, the time zone changes, or the app
 * is updated. Alarms scheduled with AlarmManager are cleared by all three events, so this
 * is essential for reliability.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.i(TAG, "Rescheduling alarms after ${intent.action}")
                val pending = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        AlarmScheduler(appContext).rescheduleAll()
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to reschedule alarms", t)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
