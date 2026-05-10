package dev.mslalith.focuslauncher.feature.appdrawerpage.apps.list

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.exp

/**
 * Full-screen overlay alphabet index with a Gaussian bow curve and bubble indicator.
 *
 * The drawing Canvas fills the entire parent so the bubble can render to the left of the
 * strip. The pointer input lives on a separate right-edge Box that is exactly STRIP_DP wide,
 * so it never overlaps the app list and never interferes with list-item click detection.
 */
@Composable
internal fun AlphabetIndex(
    characters: List<Char>,
    onCharacterTap: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    if (characters.isEmpty()) return

    var touchY by remember { mutableStateOf<Float?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }

    val textMeasurer = rememberTextMeasurer()
    val letterColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleColor = Color(0xFF1A1A1A)

    Box(modifier = modifier.fillMaxSize()) {
        // Drawing layer — full-screen so the bubble can appear to the left of the strip.
        // No pointer input here; touches pass straight through to the app list.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIndexOverlay(
                characters = characters,
                state = IndexDrawState(touchY, isDragging, selectedIndex),
                textMeasurer = textMeasurer,
                colors = IndexColors(letterColor, bubbleColor)
            )
        }

        // Input layer — only the right STRIP_DP strip. Nothing in the list area is blocked.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(STRIP_DP.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(characters, onCharacterTap) {
                    detectStripSlide(characters, onCharacterTap) { ty, dragging, selIdx ->
                        touchY = ty
                        isDragging = dragging
                        selectedIndex = selIdx
                    }
                }
        )
    }
}

// ─── Gesture detection ────────────────────────────────────────────────────────

private suspend fun PointerInputScope.detectStripSlide(
    characters: List<Char>,
    onCharacterTap: (Char) -> Unit,
    onUpdate: (touchY: Float?, isDragging: Boolean, selectedIndex: Int) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        var lastIdx = -1

        fun processY(y: Float) {
            val clamped = y.coerceIn(0f, size.height.toFloat())
            val idx = charIndexAt(clamped, size.height.toFloat(), characters.size)
            onUpdate(clamped, true, idx)
            if (idx != lastIdx) {
                lastIdx = idx
                onCharacterTap(characters[idx])
            }
        }

        processY(down.position.y)

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null || !change.pressed) break
            change.consume()
            processY(change.position.y)
        }

        onUpdate(null, false, -1)
    }
}

// ─── Drawing ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawIndexOverlay(
    characters: List<Char>,
    state: IndexDrawState,
    textMeasurer: TextMeasurer,
    colors: IndexColors
) {
    val baseX = size.width - (STRIP_DP / 2f).dp.toPx()
    val maxBow = size.width * BOW_FRACTION
    val sigma = size.height * SIGMA_FRACTION
    val dragging = state.isDragging && state.touchY != null

    characters.forEachIndexed { i, char ->
        val letterY = (i + 0.5f) / characters.size * size.height
        val weight = if (dragging) gaussianWeight(letterY, state.touchY!!, sigma) else 0f
        drawLetterAt(
            info = LetterInfo(char, baseX - maxBow * weight, letterY, weight, dragging && i == state.selectedIndex),
            textMeasurer = textMeasurer,
            letterColor = colors.letter
        )
    }

    if (dragging && state.selectedIndex >= 0) {
        val bubbleX = baseX - maxBow - BUBBLE_RADIUS_DP.dp.toPx() - 6.dp.toPx()
        drawBubble(characters[state.selectedIndex], state.touchY!!, textMeasurer, colors.bubble, bubbleX)
    }
}

private fun DrawScope.drawLetterAt(
    info: LetterInfo,
    textMeasurer: TextMeasurer,
    letterColor: Color
) {
    val active = info.weight > 0f
    val alpha = if (active) (0.25f + 0.75f * info.weight).coerceIn(0.25f, 1f) else 1f
    val style = TextStyle(
        fontSize = if (active) (9f + 5f * info.weight).sp else 10.sp,
        fontWeight = if (info.isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (info.isSelected) Color.White else letterColor.copy(alpha = alpha)
    )
    val measured = textMeasurer.measure(info.char.toString(), style)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            x = info.x - measured.size.width / 2f,
            y = info.y - measured.size.height / 2f
        )
    )
}

private fun DrawScope.drawBubble(
    char: Char,
    touchY: Float,
    textMeasurer: TextMeasurer,
    bubbleColor: Color,
    bubbleX: Float
) {
    val radius = BUBBLE_RADIUS_DP.dp.toPx()
    val bubbleY = touchY.coerceIn(radius, size.height - radius)
    drawCircle(color = bubbleColor, radius = radius, center = Offset(bubbleX, bubbleY))
    val style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
    val measured = textMeasurer.measure(char.toString(), style)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            x = bubbleX - measured.size.width / 2f,
            y = bubbleY - measured.size.height / 2f
        )
    )
}

// ─── Models & helpers ─────────────────────────────────────────────────────────

private data class IndexDrawState(val touchY: Float?, val isDragging: Boolean, val selectedIndex: Int)

private data class IndexColors(val letter: Color, val bubble: Color)

private data class LetterInfo(
    val char: Char,
    val x: Float,
    val y: Float,
    val weight: Float,
    val isSelected: Boolean
)

private fun gaussianWeight(letterY: Float, touchY: Float, sigma: Float): Float {
    val d = (letterY - touchY) / sigma
    return exp(-(d * d) / 2.0).toFloat()
}

private fun charIndexAt(y: Float, height: Float, n: Int): Int =
    ((y / height) * n).toInt().coerceIn(0, n - 1)

private const val STRIP_DP = 48f
private const val BOW_FRACTION = 0.44f
private const val SIGMA_FRACTION = 0.18f
private const val BUBBLE_RADIUS_DP = 42f
