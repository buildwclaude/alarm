package com.buildwclaude.alarm.ui.neu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

/**
 * A live neumorphic analog clock, echoing the soft embossed clock face from the design.
 * Hour/minute hands are dark; the second hand is the warm-red accent.
 */
@Composable
fun AnalogClock(modifier: Modifier = Modifier, diameter: Dp = 260.dp) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedTicker { now = LocalTime.now() }

    Box(
        modifier
            .size(diameter)
            .neuRaised(cornerRadius = diameter / 2, offset = 9.dp, blur = 20.dp),
    ) {
        // Inner recessed ring for a two-layer soft look.
        Box(
            Modifier
                .padding(28.dp)
                .size(diameter - 56.dp)
                .neuInset(cornerRadius = (diameter - 56.dp) / 2, surface = Neu.Surface),
        )
        Canvas(Modifier.padding(24.dp).size(diameter - 48.dp)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Tick marks: longer at the quarters.
            for (i in 0 until 12) {
                val angle = Math.toRadians(i * 30.0)
                val outer = r * 0.96f
                val inner = if (i % 3 == 0) r * 0.82f else r * 0.89f
                val sinA = sin(angle).toFloat()
                val cosA = cos(angle).toFloat()
                drawLine(
                    color = Neu.TextSecondary,
                    start = Offset(c.x + sinA * inner, c.y - cosA * inner),
                    end = Offset(c.x + sinA * outer, c.y - cosA * outer),
                    strokeWidth = if (i % 3 == 0) 5f else 2.5f,
                    cap = StrokeCap.Round,
                )
            }

            val hour = now.hour % 12
            val minute = now.minute
            val second = now.second

            fun hand(fraction: Float, length: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
                val angle = Math.toRadians(fraction * 360.0)
                drawLine(
                    color = color,
                    start = c,
                    end = Offset(c.x + sin(angle).toFloat() * length, c.y - cos(angle).toFloat() * length),
                    strokeWidth = width,
                    cap = StrokeCap.Round,
                )
            }

            hand((hour + minute / 60f) / 12f, r * 0.5f, 10f, Neu.TextPrimary)
            hand((minute + second / 60f) / 60f, r * 0.72f, 7f, Neu.TextPrimary)
            hand(second / 60f, r * 0.8f, 3f, Neu.Accent)

            drawCircle(Neu.Accent, radius = 7f, center = c)
        }
    }
}

/** Ticks roughly once a second to advance the clock. */
@Composable
private fun LaunchedTicker(onTick: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            onTick()
            delay(1000)
        }
    }
}
