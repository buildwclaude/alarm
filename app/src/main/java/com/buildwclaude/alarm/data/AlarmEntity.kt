package com.buildwclaude.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One alarm, as stored in the Room database.
 *
 * [repeatMask] is a 7-bit set of weekdays: bit 0 = Sunday … bit 6 = Saturday.
 * A mask of 0 means a one-off alarm (fires once, then turns itself off).
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int = 7,          // 0..23
    val minute: Int = 0,        // 0..59
    val label: String = "",
    val repeatMask: Int = 0,    // see RepeatDays
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 5,
    val soundKey: String = "default",
)
