package com.buildwclaude.alarm.ui.band

import androidx.compose.ui.graphics.Color

/**
 * The six times-of-day the picker paints itself as. Step 2 only needs [forHour] and a
 * subtle [cardTint] for the list; Step 3 extends this with full gradients and the
 * sun/moon arc.
 */
enum class TimeBand(val displayName: String) {
    NIGHT("night"),
    DAWN("dawn"),
    MORNING("morning"),
    AFTERNOON("afternoon"),
    EVENING("evening"),
    DUSK("dusk");

    /** Low-saturation colour used to tint alarm-list cards by their time of day. */
    fun cardTint(): Color = when (this) {
        NIGHT -> Color(0xFF22243A)
        DAWN -> Color(0xFF3A3357)
        MORNING -> Color(0xFFDCEBFA)
        AFTERNOON -> Color(0xFFF6E7CB)
        EVENING -> Color(0xFFF4D6CC)
        DUSK -> Color(0xFF35304F)
    }

    companion object {
        fun forHour(hour: Int): TimeBand = forMinutes(hour * 60)

        /** Minute-accurate band lookup so the picker can cross-fade mid-hour. */
        fun forMinutes(minutesOfDay: Int): TimeBand {
            val m = ((minutesOfDay % 1440) + 1440) % 1440
            return when (m) {
                in 5 * 60 until 8 * 60 -> DAWN
                in 8 * 60 until 12 * 60 -> MORNING
                in 12 * 60 until 17 * 60 -> AFTERNOON
                in 17 * 60 until 20 * 60 -> EVENING
                in 20 * 60 until 22 * 60 -> DUSK
                else -> NIGHT // 22:00–04:59
            }
        }
    }
}
