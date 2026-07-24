package com.buildwclaude.alarm.alarm

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * The wake-up flash: solid full-screen blue → red → green, three cycles (nine flashes),
 * ~400 ms per colour with a soft fade between. The whole thing runs in ~4 s.
 *
 * IMPORTANT — photosensitive safety: the colour changes roughly every 400 ms, i.e. about
 * 2.5 Hz, which is well under the 3 Hz seizure-risk threshold. Do NOT shorten these timings.
 * The flash is skipped entirely when the user disables it in Settings or has the system
 * "Remove animations" accessibility setting on (see AlarmActivity).
 */

private val FLASH_BLUE = Color(0xFF1565FF)
private val FLASH_RED = Color(0xFFE01B1B)
private val FLASH_GREEN = Color(0xFF15C24A)

const val FLASH_STEP_MS = 400L
const val FLASH_CYCLES = 3

@Composable
fun FlashScreen(onFinished: () -> Unit) {
    val sequence = remember {
        buildList {
            repeat(FLASH_CYCLES) { addAll(listOf(FLASH_BLUE, FLASH_RED, FLASH_GREEN)) }
        }
    }
    var target by remember { mutableStateOf(sequence.first()) }

    // ~180 ms fade keeps each transition soft while staying under 3 Hz.
    val color by animateColorAsState(target, tween(180), label = "flash")

    LaunchedEffect(Unit) {
        for (c in sequence) {
            target = c
            delay(FLASH_STEP_MS)
        }
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(color))
}
