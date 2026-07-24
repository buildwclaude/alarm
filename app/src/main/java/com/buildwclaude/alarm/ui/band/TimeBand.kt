package com.buildwclaude.alarm.ui.band

import androidx.compose.ui.graphics.Color

/**
 * Everything the adaptive picker needs to know to paint itself as a given time of day.
 * A [palette] carries a three-stop sky gradient, a contrasting [BandPalette.onColor] for
 * text/icons, an [BandPalette.isDark] flag (light-on-dark vs dark-on-light), and a
 * plain-language [BandPalette.phrase] for the confirmation line.
 */
enum class TimeBand(val displayName: String) {
    NIGHT("night"),
    DAWN("dawn"),
    MORNING("morning"),
    AFTERNOON("afternoon"),
    EVENING("evening"),
    DUSK("dusk");

    fun palette(): BandPalette = when (this) {
        NIGHT -> BandPalette(
            skyTop = Color(0xFF05070F), skyMid = Color(0xFF0C1330), skyBottom = Color(0xFF1B2247),
            onColor = Color(0xFFEAF0FF), isDark = true, phrase = "at night",
        )
        DAWN -> BandPalette(
            skyTop = Color(0xFF232159), skyMid = Color(0xFF8A5A8E), skyBottom = Color(0xFFF3B171),
            onColor = Color(0xFFFFF3E6), isDark = true, phrase = "at dawn",
        )
        MORNING -> BandPalette(
            skyTop = Color(0xFF3E9BE0), skyMid = Color(0xFF8FCBF0), skyBottom = Color(0xFFDDEFFB),
            onColor = Color(0xFF0C2A3E), isDark = false, phrase = "in the morning",
        )
        AFTERNOON -> BandPalette(
            skyTop = Color(0xFF5C97C9), skyMid = Color(0xFFAFC6D6), skyBottom = Color(0xFFF3CD86),
            onColor = Color(0xFF23180A), isDark = false, phrase = "in the afternoon",
        )
        EVENING -> BandPalette(
            skyTop = Color(0xFFF2743E), skyMid = Color(0xFFC03E77), skyBottom = Color(0xFF5A2A72),
            onColor = Color(0xFFFFF0E8), isDark = true, phrase = "in the evening",
        )
        DUSK -> BandPalette(
            skyTop = Color(0xFF3A2A66), skyMid = Color(0xFF241C4A), skyBottom = Color(0xFF10122C),
            onColor = Color(0xFFEDE9FF), isDark = true, phrase = "at dusk",
        )
    }

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

        fun phraseForMinutes(minutesOfDay: Int): String = forMinutes(minutesOfDay).palette().phrase
    }
}

/** Colours and words for one time-of-day band. */
data class BandPalette(
    val skyTop: Color,
    val skyMid: Color,
    val skyBottom: Color,
    val onColor: Color,
    val isDark: Boolean,
    val phrase: String,
)
