package dev.mslalith.focuslauncher.feature.homepage.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.mslalith.focuslauncher.core.ui.extensions.clickableNoRipple
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mslalith.focuslauncher.core.data.repository.MediaState

@Composable
fun MediaPlayerWidget(
    state: MediaState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.showWidget,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 },
        exit = fadeOut(tween(500)) + slideOutVertically(tween(500)) { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { onOpenApp() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.artist,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[<<]",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickableNoRipple { onPrevClick() }
                    )

                    Row(
                        modifier = Modifier.clickableNoRipple { onPlayPauseClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[",
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                        AnimatedPlayPauseIcon(
                            isPlaying = state.isPlaying,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "]",
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "[>>]",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickableNoRipple { onNextClick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedPlayPauseIcon(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(isPlaying, label = "PlayPause")

    val line1Start by transition.animateOffset(
        transitionSpec = { spring(dampingRatio = 0.6f, stiffness = 400f) },
        label = "line1Start"
    ) { playing ->
        if (playing) Offset(0.3f, 0f) else Offset(0.2f, 0.1f)
    }
    val line1End by transition.animateOffset(
        transitionSpec = { spring(dampingRatio = 0.6f, stiffness = 400f) },
        label = "line1End"
    ) { playing ->
        if (playing) Offset(0.3f, 1f) else Offset(0.8f, 0.5f)
    }

    val line2Start by transition.animateOffset(
        transitionSpec = { spring(dampingRatio = 0.6f, stiffness = 400f) },
        label = "line2Start"
    ) { playing ->
        if (playing) Offset(0.7f, 0f) else Offset(0.2f, 0.9f)
    }
    val line2End by transition.animateOffset(
        transitionSpec = { spring(dampingRatio = 0.6f, stiffness = 400f) },
        label = "line2End"
    ) { playing ->
        if (playing) Offset(0.7f, 1f) else Offset(0.8f, 0.5f)
    }

    Canvas(
        modifier = modifier
            .width(10.dp)
            .height(14.dp)
    ) {
        val w = size.width
        val h = size.height

        drawLine(
            color = color,
            start = Offset(line1Start.x * w, line1Start.y * h),
            end = Offset(line1End.x * w, line1End.y * h),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(line2Start.x * w, line2Start.y * h),
            end = Offset(line2End.x * w, line2End.y * h),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
