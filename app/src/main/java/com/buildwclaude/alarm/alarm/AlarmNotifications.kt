package com.buildwclaude.alarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.buildwclaude.alarm.R

/**
 * Builds the notification channels and the full-screen "alarm ringing" notification that
 * launches [AlarmActivity] over the lock screen.
 */
object AlarmNotifications {

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val ringing = NotificationChannel(
            AlarmContract.CHANNEL_RINGING,
            "Alarm ringing",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Shown full-screen while an alarm is going off."
            setBypassDnd(true)
            enableLights(true)
            enableVibration(false) // vibration is handled by the service so we can control the pattern
            setSound(null, null)   // sound is played by the service on the alarm stream
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val status = NotificationChannel(
            AlarmContract.CHANNEL_STATUS,
            "Setup & status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Quiet reminders about permissions and setup."
        }

        nm.createNotificationChannel(ringing)
        nm.createNotificationChannel(status)
    }

    /** Full-screen notification whose [PendingIntent] opens the ringing screen. */
    fun buildRingingNotification(context: Context, alarmId: Int, label: String): Notification {
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, alarmId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (label.isBlank()) "Alarm" else label
        // Deliberately NO action buttons: the alarm can ONLY be stopped by solving the
        // riddle (then watching the flash) in the full-screen screen. The notification just
        // re-opens that screen if tapped.
        return NotificationCompat.Builder(context, AlarmContract.CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Tap to solve the riddle and stop the alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPi)
            .setFullScreenIntent(fullScreenPi, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}
