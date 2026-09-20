package dev.shephard.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import dev.shephard.player.ui.miuix.MiuixAppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MinimalSeekBar(
    progress: Float,
    onSeekPreview: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 6.dp,
    thumbRadius: Dp = 0.dp,
    activeColor: Color = MiuixAppTheme.colorScheme.primary,
    inactiveColor: Color = MiuixAppTheme.colorScheme.surfaceVariant
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val displayedFraction = if (dragging) dragFraction else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
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
                .height(24.dp)
        ) {
            val centerY = size.height / 2f
            val trackHeightPx = trackHeight.toPx()
            val startX = 0f
            val endX = size.width
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
        }
    }
}

@Composable
fun rememberSmoothProgressFraction(positionMs: Long, durationMs: Long, isPlaying: Boolean): Float {
    var smooth by remember { mutableFloatStateOf(0f) }
    var lastSamplePosMs by remember { mutableLongStateOf(0L) }
    var lastSampleAtMs by remember { mutableLongStateOf(0L) }
    val durMs = durationMs.coerceAtLeast(1L)

    LaunchedEffect(positionMs, durationMs, isPlaying) {
        val pos = positionMs.coerceAtLeast(0L)
        val now = android.os.SystemClock.elapsedRealtime()
        lastSamplePosMs = pos
        lastSampleAtMs = now
        smooth = (pos.toFloat() / durMs).coerceIn(0f, 1f)
        while (true) {
            withFrameNanos { }
            val elapsed = (android.os.SystemClock.elapsedRealtime() - lastSampleAtMs).coerceAtLeast(0L)
            val displayedPos = if (isPlaying) (lastSamplePosMs + elapsed).coerceAtMost(pos + 500L) else lastSamplePosMs
            smooth = (displayedPos.toFloat() / durMs).coerceIn(0f, 1f)
        }
    }
    return smooth
}
