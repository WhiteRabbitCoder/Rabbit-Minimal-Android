package dev.mslalith.focuslauncher.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt

private const val PIX_GRID_SIZE = 24

@Composable
fun PixRabbitPixelIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawPixIcon()
    }
}

private fun DrawScope.drawPixIcon() {
    val mint = Color(0xFF7FE9B2)
    val white = Color(0xFFFFFFFF)
    val pink = Color(0xFFFF9BC2)
    val grid = PIX_GRID_SIZE
    val px = min(size.width, size.height) / grid.toFloat()
    val xOffset = (size.width - (grid * px)) / 2f
    val yOffset = (size.height - (grid * px)) / 2f

    fun pixel(x: Int, y: Int, color: Color) {
        drawRect(
            color = color,
            topLeft = Offset(xOffset + (x * px), yOffset + (y * px)),
            size = androidx.compose.ui.geometry.Size(px, px)
        )
    }

    val center = 11.5f
    val radius = 10.5f
    repeat(grid) { y ->
        repeat(grid) { x ->
            val dx = x - center
            val dy = y - center
            val distance = sqrt((dx * dx) + (dy * dy))
            if (abs(distance - radius) <= 0.6f) {
                pixel(x, y, mint)
            }
        }
    }

    val rabbitWhitePixels = listOf(
        7 to 16, 8 to 16, 9 to 16, 10 to 16, 11 to 16, 12 to 16, 13 to 16,
        6 to 15, 7 to 15, 8 to 15, 9 to 15, 10 to 15, 11 to 15, 12 to 15, 13 to 15,
        6 to 14, 7 to 14, 8 to 14, 9 to 14, 10 to 14, 11 to 14, 12 to 14, 13 to 14,
        7 to 13, 8 to 13, 9 to 13, 10 to 13, 11 to 13, 12 to 13, 13 to 13,
        8 to 12, 9 to 12, 10 to 12, 11 to 12, 12 to 12,
        9 to 11, 10 to 11, 11 to 11, 12 to 11,
        10 to 10, 11 to 10,
        9 to 9, 10 to 9, 11 to 9,
        8 to 8, 9 to 8, 10 to 8,
        7 to 7, 8 to 7, 9 to 7,
        7 to 6, 8 to 6,
        11 to 9, 12 to 9, 13 to 9,
        12 to 8, 13 to 8,
        13 to 7, 14 to 7
    )
    rabbitWhitePixels.forEach { (x, y) -> pixel(x, y, white) }

    val rabbitPinkPixels = listOf(
        8 to 7, 9 to 8, 10 to 9,
        12 to 9, 13 to 8
    )
    rabbitPinkPixels.forEach { (x, y) -> pixel(x, y, pink) }

    val baseSegments = listOf(
        4 to 20, 5 to 20, 6 to 20, 7 to 20,
        9 to 20, 10 to 20, 11 to 20, 12 to 20,
        14 to 20, 15 to 20, 16 to 20, 17 to 20, 18 to 20
    )
    baseSegments.forEach { (x, y) -> pixel(x, y, mint) }
}
