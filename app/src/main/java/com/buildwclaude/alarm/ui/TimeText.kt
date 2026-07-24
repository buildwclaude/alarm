package com.buildwclaude.alarm.ui

/** Formatting helpers shared by the list and editor. Always 12-hour with a clear AM/PM. */
object TimeText {
    fun hourMinute(hour: Int, minute: Int): String {
        val h12 = when (hour % 12) { 0 -> 12; else -> hour % 12 }
        return "%d:%02d".format(h12, minute)
    }

    fun amPm(hour: Int): String = if (hour < 12) "AM" else "PM"
}
