package com.buildwclaude.alarm.ui.neu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neumorphic ("soft UI") palette, matching the Figma clock design: a warm off-white
 * surface where every element is the same colour as the background and depth is created
 * purely with a light highlight (top-left) and a soft dark shadow (bottom-right).
 */
object Neu {
    val Background = Color(0xFFECE9E6)   // warm light grey the shadows read against
    val Surface = Color(0xFFF3F0ED)      // raised element colour (slightly lighter)
    val Light = Color(0xFFFFFFFF)        // top-left highlight
    val Shadow = Color(0x40000000)       // bottom-right soft shadow
    val TextPrimary = Color(0xFF2B2A28)
    val TextSecondary = Color(0xFF8A8782)
    val Accent = Color(0xFFE0533D)       // warm red accent (clock hand / active)
    val TrackInset = Color(0xFFE4E0DC)
}

/**
 * Raised soft-UI effect: draws a blurred light shadow up-left and a blurred dark shadow
 * down-right behind the element, using the framework paint's shadow layer so it works on
 * every API level (no RenderEffect blur required).
 */
fun Modifier.neuRaised(
    cornerRadius: Dp = 22.dp,
    light: Color = Neu.Light,
    dark: Color = Neu.Shadow,
    surface: Color = Neu.Surface,
    offset: Dp = 7.dp,
    blur: Dp = 14.dp,
): Modifier = drawBehind {
    val radiusPx = cornerRadius.toPx()
    val offsetPx = offset.toPx()
    val blurPx = blur.toPx()

    drawIntoCanvas { canvas ->
        val paint = androidx.compose.ui.graphics.Paint()
        val frameworkPaint = paint.asFrameworkPaint()

        fun shadow(dx: Float, dy: Float, color: Color) {
            frameworkPaint.color = android.graphics.Color.TRANSPARENT
            frameworkPaint.setShadowLayer(blurPx, dx, dy, color.toArgb())
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height, radiusPx, radiusPx, frameworkPaint,
            )
        }
        // Dark shadow bottom-right, light highlight top-left.
        shadow(offsetPx, offsetPx, dark)
        shadow(-offsetPx, -offsetPx, light)
    }
    // The element's own surface fill on top of the shadows.
    drawRoundRectSurface(surface, radiusPx)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectSurface(
    surface: Color,
    radiusPx: Float,
) {
    drawRoundRect(
        color = surface,
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx),
    )
}

/**
 * Carved-in ("inset") soft-UI effect used for switch tracks: fills the surface a touch
 * darker than the background and draws a subtle inner edge so it reads as recessed. Kept
 * deliberately simple so it renders identically on every device.
 */
fun Modifier.neuInset(
    cornerRadius: Dp = 22.dp,
    surface: Color = Neu.TrackInset,
): Modifier = drawBehind {
    val radiusPx = cornerRadius.toPx()
    drawRoundRectSurface(surface, radiusPx)
    // Darker hairline along the top, lighter along the bottom -> looks pressed in.
    drawRoundRect(
        color = Neu.Shadow.copy(alpha = 0.10f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
    )
}

val CardPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
val RoundedNeuShape = RoundedCornerShape(22.dp)

@Composable
fun rememberNeuShape(radius: Dp = 22.dp) = remember(radius) { RoundedCornerShape(radius) }
