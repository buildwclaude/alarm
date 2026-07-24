package com.buildwclaude.alarm.alarm

/**
 * Shared constants for intents, extras and notification channels used across the
 * scheduler, receivers, ringing service and full-screen alarm activity.
 */
object AlarmContract {
    // Intent actions
    const val ACTION_FIRE = "com.buildwclaude.alarm.action.FIRE"
    const val ACTION_DISMISS = "com.buildwclaude.alarm.action.DISMISS"
    const val ACTION_SNOOZE = "com.buildwclaude.alarm.action.SNOOZE"

    // Extras
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_IS_SNOOZE = "is_snooze"
    const val EXTRA_AUTO_SNOOZE_COUNT = "auto_snooze_count"

    // Notification channels
    const val CHANNEL_RINGING = "alarm_ringing"       // full-screen, high importance
    const val CHANNEL_STATUS = "alarm_status"         // quiet info (permissions, etc.)

    const val RINGING_NOTIFICATION_ID = 42

    // PendingIntent request-code spaces: main schedule uses the alarm id directly;
    // snooze uses id + this offset so the two never clobber each other.
    const val SNOOZE_REQUEST_OFFSET = 5_000_000

    // If nobody solves the riddle within this window, auto-snooze once, then give up.
    const val RING_TIMEOUT_MS = 10 * 60 * 1000L
    const val MAX_AUTO_SNOOZES = 1
}
