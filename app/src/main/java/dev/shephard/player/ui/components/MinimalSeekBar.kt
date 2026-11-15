package dev.shephard.player.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.rememberUpdatedState
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
    inactiveColor: Color = MiuixAppTheme.colorScheme.surfaceVariant,
    expandOnPress: Boolean = false,
) {
    var dragging by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    // Pointer handlers survive recomposition; always seek using the latest duration/callbacks.
    val currentOnSeekPreview by rememberUpdatedState(onSeekPreview)
    val currentOnSeekFinished by rememberUpdatedState(onSeekFinished)
    val animatedTrackHeight by animateDpAsState(
        targetValue = if (expandOnPress && (pressed || dragging)) trackHeight + 4.dp else trackHeight,
        animationSpec = tween(durationMillis = if (pressed || dragging) 140 else 200),
        label = "seekBarPressHeight",
    )

    val displayedFraction = if (dragging) dragFraction else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(expandOnPress) {
                detectTapGestures(
                    onPress = {
                        if (expandOnPress) {
                            pressed = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                pressed = false
                            }
                        }
                    },
                    onTap = { offset ->
                        val fraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        currentOnSeekFinished(fraction)
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        currentOnSeekPreview(dragFraction)
                    },
                    onDrag = { change, _ ->
                        val fraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        dragFraction = fraction
                        currentOnSeekPreview(fraction)
                    },
                    onDragEnd = {
                        dragging = false
                        currentOnSeekFinished(dragFraction)
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
            val trackHeightPx = animatedTrackHeight.toPx()
            // Inset just the extra round-cap growth, so the animation expands vertically
            // around the center without lengthening the bar at either end.
            val extraInset = ((animatedTrackHeight - trackHeight).toPx() / 2f).coerceAtLeast(0f)
            val startX = extraInset.coerceAtMost(size.width / 2f)
            val endX = (size.width - extraInset).coerceAtLeast(startX)
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
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            withFrameNanos { }
            val elapsed = (android.os.SystemClock.elapsedRealtime() - lastSampleAtMs).coerceAtLeast(0L)
            val displayedPos = (lastSamplePosMs + elapsed).coerceAtMost(pos + 500L)
            smooth = (displayedPos.toFloat() / durMs).coerceIn(0f, 1f)
        }
    }
    return smooth
}
