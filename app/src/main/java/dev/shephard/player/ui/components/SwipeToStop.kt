// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf

/** Swipe the mini player down to close the entire playback session. */
fun Modifier.swipeDownToStop(onStop: () -> Unit): Modifier = composed {
    val scope = rememberCoroutineScope()
    val currentOnStop by rememberUpdatedState(onStop)
    var offset by remember { mutableFloatStateOf(0f) }
    var job by remember { mutableStateOf<Job?>(null) }
    fun animateBack() {
        job?.cancel()
        job = scope.launch {
            animate(offset, 0f, animationSpec = tween(180, easing = FastOutSlowInEasing)) { value, _ -> offset = value }
        }
    }
    this.graphicsLayer {
        translationY = offset
        alpha = 1f - (offset / (size.height.coerceAtLeast(1f) * 2f)).coerceIn(0f, .65f)
    }.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = { job?.cancel() },
            onDragCancel = { animateBack() },
            onDragEnd = {
                if (offset >= 56.dp.toPx()) {
                    job?.cancel()
                    job = scope.launch {
                        animate(offset, size.height.toFloat().coerceAtLeast(80.dp.toPx()), animationSpec = tween(160)) { value, _ -> offset = value }
                        currentOnStop()
                        offset = 0f
                    }
                } else animateBack()
            },
            onVerticalDrag = { change, dragAmount ->
                if (dragAmount > 0 || offset > 0) {
                    change.consume()
                    offset = (offset + dragAmount).coerceAtLeast(0f)
                }
            },
        )
    }
}
