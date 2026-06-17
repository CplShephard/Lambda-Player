package dev.shephard.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A minimal, single-line seek bar: thin track with a circular thumb,
 * matching a clean modern player aesthetic.
 *
 * [progress] is the current playback fraction (0f..1f), shown only while
 * the user is not actively dragging. While dragging, the thumb follows the
 * gesture directly for a fluid feel, and [onSeekPreview]/[onSeekFinished]
 * report the dragged fraction.
 */
@Composable
fun MinimalSeekBar(
    progress: Float,
    onSeekPreview: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 3.dp,
    thumbRadius: Dp = 7.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val displayedFraction = if (dragging) dragFraction else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbRadius * 2)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeekFinished(fraction)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekPreview(dragFraction)
                    },
                    onDrag = { change, _ ->
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onSeekPreview(fraction)
                    },
                    onDragEnd = {
                        dragging = false
                        onSeekFinished(dragFraction)
                    },
                    onDragCancel = {
                        dragging = false
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbRadius * 2)
        ) {
            val centerY = size.height / 2f
            val thumbRadiusPx = thumbRadius.toPx()
            val trackHeightPx = trackHeight.toPx()
            val startX = thumbRadiusPx
            val endX = size.width - thumbRadiusPx
            val activeX = startX + (endX - startX) * displayedFraction

            drawLine(
                color = inactiveColor,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = trackHeightPx,
                cap = StrokeCap.Round
            )

            drawLine(
                color = activeColor,
                start = Offset(startX, centerY),
                end = Offset(activeX, centerY),
                strokeWidth = trackHeightPx,
                cap = StrokeCap.Round
            )

            drawCircle(
                color = activeColor,
                radius = thumbRadiusPx,
                center = Offset(activeX, centerY)
            )
        }
    }
}
