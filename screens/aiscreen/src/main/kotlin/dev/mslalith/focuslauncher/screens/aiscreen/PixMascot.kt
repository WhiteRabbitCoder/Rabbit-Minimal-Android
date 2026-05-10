package dev.mslalith.focuslauncher.screens.aiscreen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
internal fun PixMascot(
    pixState: PixState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pix")

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (pixState == PixState.IDLE) 6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val opacity by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (pixState == PixState.THINKING) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity"
    )

    val earRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (pixState == PixState.RESPONDING) 8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ear"
    )

    Canvas(
        modifier = modifier.size(80.dp)
    ) {
        val yOffset = bounceOffset
        drawPix(
            yOffset = yOffset,
            alpha = opacity,
            earRotation = earRotation
        )
    }
}

private fun DrawScope.drawPix(
    yOffset: Float,
    alpha: Float,
    earRotation: Float
) {
    val white = Color.White.copy(alpha = alpha)
    val black = Color.Black.copy(alpha = alpha)
    val green = Color(0xFF4ADE80).copy(alpha = alpha)

    val cx = size.width / 2f
    val cy = size.height / 2f + yOffset

    // Left ear
    rotate(degrees = -earRotation, pivot = Offset(cx - 18f, cy - 28f)) {
        drawRoundRect(
            color = white,
            topLeft = Offset(cx - 26f, cy - 52f),
            size = Size(14f, 26f),
            cornerRadius = CornerRadius(7f)
        )
    }

    // Right ear
    rotate(degrees = earRotation, pivot = Offset(cx + 18f, cy - 28f)) {
        drawRoundRect(
            color = white,
            topLeft = Offset(cx + 12f, cy - 52f),
            size = Size(14f, 26f),
            cornerRadius = CornerRadius(7f)
        )
    }

    // Body (head — round rectangle)
    drawRoundRect(
        color = white,
        topLeft = Offset(cx - 28f, cy - 26f),
        size = Size(56f, 50f),
        cornerRadius = CornerRadius(20f)
    )

    // Left eye
    drawCircle(color = black, radius = 5f, center = Offset(cx - 10f, cy - 6f))
    // Left eye shine
    drawCircle(color = Color.White.copy(alpha = alpha), radius = 1.5f, center = Offset(cx - 8f, cy - 8f))

    // Right eye
    drawCircle(color = black, radius = 5f, center = Offset(cx + 10f, cy - 6f))
    // Right eye shine
    drawCircle(color = Color.White.copy(alpha = alpha), radius = 1.5f, center = Offset(cx + 12f, cy - 8f))

    // Nose (small green dot)
    drawCircle(color = green, radius = 3f, center = Offset(cx, cy + 4f))

    // Cheeks (blush)
    drawCircle(
        color = Color(0xFFFFB3B3).copy(alpha = alpha * 0.5f),
        radius = 6f,
        center = Offset(cx - 18f, cy + 6f)
    )
    drawCircle(
        color = Color(0xFFFFB3B3).copy(alpha = alpha * 0.5f),
        radius = 6f,
        center = Offset(cx + 18f, cy + 6f)
    )
}
