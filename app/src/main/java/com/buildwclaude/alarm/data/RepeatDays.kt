package com.buildwclaude.alarm.data

import java.time.DayOfWeek

/**
 * Helpers for the [AlarmEntity.repeatMask] bitset.
 * Bit 0 = Sunday, bit 1 = Monday … bit 6 = Saturday — matching the "S M T W T F S" chip row.
 */
object RepeatDays {
    // Chip order shown in the editor, left to right.
    val ORDER: List<DayOfWeek> = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
    )

    // Single-letter labels for the chips.
    val LABELS: List<String> = listOf("S", "M", "T", "W", "T", "F", "S")

    private fun bit(day: DayOfWeek): Int = when (day) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }

    fun isSet(mask: Int, day: DayOfWeek): Boolean = (mask shr bit(day)) and 1 == 1

    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor (1 shl bit(day))

    fun isRepeating(mask: Int): Boolean = mask != 0

    /** Human summary like "Every day", "Weekdays", "Mon, Wed, Fri", or "Once". */
    fun summary(mask: Int): String {
        if (mask == 0) return "Once"
        val everyDay = 0b1111111
        val weekdays = 0b0111110   // Mon..Fri
        val weekend = 0b1000001    // Sun + Sat
        when (mask) {
            everyDay -> return "Every day"
            weekdays -> return "Weekdays"
            weekend -> return "Weekends"
        }
        val short = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return (0..6).filter { (mask shr it) and 1 == 1 }.joinToString(", ") { short[it] }
    }
}
