package dev.mslalith.focuslauncher.core.ui.extensions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged

private const val SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC = 600f

inline fun Modifier.modifyIf(
    predicate: () -> Boolean,
    block: Modifier.() -> Modifier
): Modifier = if (predicate()) this.then(other = block()) else this

fun Modifier.clickableNoRipple(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    this then Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}

inline fun Modifier.onSwipeDown(
    enabled: Boolean = true,
    crossinline action: () -> Unit
) = composed {
    var yStart = 0f
    var yDrag = 0f

    this then Modifier.draggable(
        enabled = enabled,
        orientation = Orientation.Vertical,
        onDragStarted = {
            yStart = it.y
            yDrag = yStart
        },
        state = rememberDraggableState { delta ->
            yDrag += delta
        },
        onDragStopped = { velocity ->
            if (yStart < yDrag && velocity > SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC) {
                action()
            }
        }
    )
}

inline fun Modifier.onSwipeUp(
    enabled: Boolean = true,
    crossinline action: () -> Unit
) = composed {
    var yStart = 0f
    var yDrag = 0f

    this then Modifier.draggable(
        enabled = enabled,
        orientation = Orientation.Vertical,
        onDragStarted = {
            yStart = it.y
            yDrag = yStart
        },
        state = rememberDraggableState { delta ->
            yDrag += delta
        },
        onDragStopped = { velocity ->
            if (yStart > yDrag && velocity < -SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC) {
                action()
            }
        }
    )
}

inline fun Modifier.onHorizontalSwipe(
    enabled: Boolean = true,
    crossinline onSwipeLeft: () -> Unit = {},
    crossinline onSwipeRight: () -> Unit = {}
) = composed {
    var xStart = 0f
    var xDrag = 0f

    this then Modifier.draggable(
        enabled = enabled,
        orientation = Orientation.Horizontal,
        onDragStarted = {
            xStart = it.x
            xDrag = xStart
        },
        state = rememberDraggableState { delta ->
            xDrag += delta
        },
        onDragStopped = { velocity ->
            if (xStart > xDrag && velocity < -SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC) {
                onSwipeLeft()
            } else if (xStart < xDrag && velocity > SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC) {
                onSwipeRight()
            }
        }
        )
}

/**
 * Detects fast horizontal swipes that start near either screen edge.
 * Calls [onSwipeFromLeft] for left-edge rightward swipes and [onSwipeFromRight] for right-edge leftward swipes.
 */
inline fun Modifier.onEdgeHorizontalSwipe(
    enabled: Boolean = true,
    edgeWidth: Dp = 32.dp,
    crossinline onSwipeFromLeft: () -> Unit = {},
    crossinline onSwipeFromRight: () -> Unit = {}
) = composed {
    val edgeWidthPx = with(LocalDensity.current) { edgeWidth.toPx() }
    var xStart = 0f
    var xDrag = 0f
    var layoutWidth = 0f

    this
        .onSizeChanged { layoutWidth = it.width.toFloat() }
        .then(
            Modifier.draggable(
                enabled = enabled,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    xStart = it.x
                    xDrag = xStart
                },
                state = rememberDraggableState { delta ->
                    xDrag += delta
                },
                onDragStopped = { velocity ->
                    val isLeftEdgeSwipe = xStart <= edgeWidthPx &&
                        xDrag > xStart &&
                        velocity > SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC
                    val isRightEdgeSwipe = layoutWidth > 0f &&
                        xStart >= (layoutWidth - edgeWidthPx) &&
                        xDrag < xStart &&
                        velocity < -SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC

                    when {
                        isLeftEdgeSwipe -> onSwipeFromLeft()
                        isRightEdgeSwipe -> onSwipeFromRight()
                    }
                }
            )
        )
}
