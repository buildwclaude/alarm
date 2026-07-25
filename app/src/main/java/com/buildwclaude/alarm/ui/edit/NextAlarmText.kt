package com.buildwclaude.alarm.ui.edit

import com.buildwclaude.alarm.alarm.AlarmTime
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.ui.TimeText
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
            now.toLocalDate() -> "today"
            now.toLocalDate().plusDays(1) -> "tomorrow"
            else -> dayName(trigger.toLocalDate())
        }

        val timeStr = "${TimeText.hourMinute(hour, minute)} ${TimeText.amPm(hour)}"

        val inPart = when {
            totalMinutes == 0L -> "in under a minute"
            h == 0L -> "in $m min"
            m == 0L -> "in $h hr"
            else -> "in $h hr $m min"
        }
        // e.g. "Rings today at 10:48 AM · in 1 min"
        return "Rings $dayWord at $timeStr · $inPart"
    }

    private fun dayName(date: LocalDate): String = date.dayOfWeek.name
        .lowercase().replaceFirstChar { it.uppercase() }
}
