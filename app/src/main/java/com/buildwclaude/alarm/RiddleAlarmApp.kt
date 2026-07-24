package com.buildwclaude.alarm

import android.app.Application
import android.util.Log
import com.buildwclaude.alarm.alarm.AlarmNotifications

/**
 * Creates the notification channels on process start. Kept defensive so a failure here
 * can never stop the app from launching.
 */
class RiddleAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { AlarmNotifications.createChannels(this) }
            .onFailure { Log.e("RiddleAlarmApp", "Failed to create notification channels", it) }
    }
}
