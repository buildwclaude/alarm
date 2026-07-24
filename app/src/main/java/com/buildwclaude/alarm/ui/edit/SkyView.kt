package com.buildwclaude.alarm.ui.edit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws the animated sky: stars for dark bands, and a sun or moon that moves along an arc
 * according to the selected time. The whole thing is a single [Canvas]; the gradient behind
 * it is drawn by the screen. Everything is a pure function of [minutesOfDay], so as the user
 * scrolls the picker the body glides across the sky.
 */
@Composable
fun SkyView(
    minutesOfDay: Int,
    starAlpha: Float,
    modifier: Modifier = Modifier,
) {
    // Precomputed star field (stable across recompositions).
    val stars = remember {
        val rnd = Random(7)
        List(48) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat() * 0.72f,
                r = rnd.nextFloat() * 1.6f + 0.6f,
                twinkle = rnd.nextFloat() * 0.5f + 0.5f,
            )
        }
    }

    // Animate the body's arc fraction and altitude so it slides rather than jumps.
    val (isSun, targetFrac) = bodyProgress(minutesOfDay)
    val frac by animateFloatAsState(targetFrac, tween(300), label = "arcFrac")
    val animStarAlpha by animateFloatAsState(starAlpha, tween(300), label = "stars")
    val sunFactor by animateFloatAsState(if (isSun) 1f else 0f, tween(300), label = "sun")

    Canvas(modifier = modifier) {
        if (animStarAlpha > 0.01f) drawStars(stars, animStarAlpha)

        val altitude = sin(PI * frac).toFloat().coerceIn(0f, 1f)
        val topY = size.height * 0.14f
        val horizonY = size.height * 0.9f
        val bodyX = (0.12f + 0.76f * frac) * size.width
        val bodyY = topY + (1f - altitude) * (horizonY - topY)
        val radius = size.minDimension * 0.11f

        if (sunFactor > 0.5f) drawSun(Offset(bodyX, bodyY), radius)
        else drawMoon(Offset(bodyX, bodyY), radius)
    }
}

private data class Star(val x: Float, val y: Float, val r: Float, val twinkle: Float)

/** Returns (isSun, fraction 0..1 along the arc) for the given minute of day. */
private fun bodyProgress(minutesOfDay: Int): Pair<Boolean, Float> {
    val m = ((minutesOfDay % 1440) + 1440) % 1440
    val sunStart = 300f   // 05:00
    val sunEnd = 1200f    // 20:00
    return if (m in sunStart.toInt() until sunEnd.toInt()) {
        true to ((m - sunStart) / (sunEnd - sunStart)).coerceIn(0f, 1f)
    } else {
        // Moon travels 20:00 -> 05:00 (540 minutes).
        val mm = ((m - sunEnd) + 1440f) % 1440f
        false to (mm / 540f).coerceIn(0f, 1f)
    }
}

private fun DrawScope.drawStars(stars: List<Star>, alpha: Float) {
    stars.forEach { s ->
        drawCircle(
            color = Color.White.copy(alpha = (alpha * s.twinkle).coerceIn(0f, 1f)),
            radius = s.r,
            center = Offset(s.x * size.width, s.y * size.height),
        )
    }
}

private fun DrawScope.drawSun(center: Offset, radius: Float) {
    // Soft glow.
    drawCircle(
        color = Color(0xFFFFE7A6).copy(alpha = 0.35f),
        radius = radius * 2.1f,
        center = center,
    )
    drawCircle(color = Color(0xFFFFDE7A), radius = radius, center = center)
}

private fun DrawScope.drawMoon(center: Offset, radius: Float) {
    val moon = Color(0xFFE9EDF7)
    drawCircle(color = moon.copy(alpha = 0.25f), radius = radius * 1.8f, center = center)
    drawCircle(color = moon, radius = radius, center = center)
    // Carve a crescent by overlaying an offset disc in a dark shade.
    drawCircle(
        color = Color(0xFF0B1024),
        radius = radius * 0.92f,
        center = Offset(center.x + radius * 0.55f, center.y - radius * 0.18f),
    )
}
