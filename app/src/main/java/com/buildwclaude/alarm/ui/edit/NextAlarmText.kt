package com.buildwclaude.alarm.ui.edit

import com.buildwclaude.alarm.alarm.AlarmTime
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.ui.band.TimeBand
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The plain-language safety-net line under the picker. Step 3 upgrades this to include the
 * time-of-day band ("Tomorrow morning · in 8 hr 42 min"); Step 2 already gives the count.
 */
object NextAlarmText {

    fun describe(
        hour: Int,
        minute: Int,
        repeatMask: Int,
        now: LocalDateTime = LocalDateTime.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val draft = AlarmEntity(hour = hour, minute = minute, repeatMask = repeatMask)
        val triggerMillis = AlarmTime.nextTrigger(draft, now, zone)
        val trigger = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(triggerMillis), zone,
        )

        val totalMinutes = ChronoUnit.MINUTES.between(now, trigger).coerceAtLeast(0)
        val h = totalMinutes / 60
        val m = totalMinutes % 60

        val dayWord = when (trigger.toLocalDate()) {
            now.toLocalDate() -> "Today"
            now.toLocalDate().plusDays(1) -> "Tomorrow"
            else -> dayName(trigger.toLocalDate())
        }

        val phrase = TimeBand.phraseForMinutes(hour * 60 + minute)

        val inPart = when {
            totalMinutes == 0L -> "in under a minute"
            h == 0L -> "in $m min"
            m == 0L -> "in $h hr"
            else -> "in $h hr $m min"
        }
        return "$dayWord $phrase · $inPart"
    }

    private fun dayName(date: LocalDate): String = date.dayOfWeek.name
        .lowercase().replaceFirstChar { it.uppercase() }
}
