package com.buildwclaude.alarm.alarm

import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.data.RepeatDays
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Pure time math for figuring out when an alarm should next go off.
 * Kept free of Android types so it is easy to reason about and unit-test.
 */
object AlarmTime {

    /**
     * Epoch-millis of the next time [alarm] should fire, at or after [from].
     * For a one-off alarm (no repeat days) this is the next occurrence of hour:minute.
     * For a repeating alarm it is the next selected weekday at hour:minute.
     */
    fun nextTrigger(
        alarm: AlarmEntity,
        from: LocalDateTime = LocalDateTime.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        var candidate = from.withHour(alarm.hour).withMinute(alarm.minute)
            .withSecond(0).withNano(0)

        if (!RepeatDays.isRepeating(alarm.repeatMask)) {
            // One-off: today if still in the future, otherwise tomorrow.
            if (!candidate.isAfter(from)) candidate = candidate.plusDays(1)
            return candidate.atZone(zone).toInstant().toEpochMilli()
        }

        // Repeating: scan forward up to 8 days for the first selected weekday.
        for (offset in 0..7) {
            val day = candidate.plusDays(offset.toLong())
            val onThisDay = RepeatDays.isSet(alarm.repeatMask, day.dayOfWeek)
            val isFuture = offset > 0 || day.isAfter(from)
            if (onThisDay && isFuture) {
                return day.atZone(zone).toInstant().toEpochMilli()
            }
        }
        // Fallback (should not happen): a week out.
        return candidate.plusDays(7).atZone(zone).toInstant().toEpochMilli()
    }
}
