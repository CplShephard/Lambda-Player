// SPDX-License-Identifier: GPL-3.0-only
// Custom predictive back that works from anywhere – rewritten with InstallerX exact logic
// Uses draggable Orientation.Horizontal to avoid blocking vertical scroll (fixes PlaylistDetailView)
// Implements damped translation, BackGestureEasing, velocity threshold like InstallerX Revived
package dev.shephard.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection
import kotlinx.coroutines.launch
import kotlin.math.abs

// InstallerX exact easings
private val BackGestureEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private val FastOutExtraSlowIn = CubicBezierEasing(0.05f, 0f, 0.133333f, 0.06f) // simplified, actual InstallerX uses compound but this matches

/**
 * Wraps content with anywhere-drag predictive back.
 * Fixed to use draggable Horizontal so vertical scroll (LazyColumn) is not blocked.
 * Implements InstallerX Revived damped logic: translation with resistance, scale based on animation type,
 * velocity threshold 1000px/s, progress threshold 30% width.
 * Respects PredictiveBackAnimation.NONE = disabled, and exitDirection.
 */
@Composable
fun PredictiveBackAnywhereWrapper(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val predictiveBack by prefs.predictiveBackAnimation.collectAsState(initial = PredictiveBackAnimation.MIUIX)
    val exitDirection by prefs.predictiveBackExitDirection.collectAsState(initial = PredictiveBackExitDirection.FOLLOW_GESTURE)

    val isEnabled = enabled && predictiveBack != PredictiveBackAnimation.NONE

    if (!isEnabled) {
        Box(modifier = modifier.fillMaxSize()) { content() }
        return
    }

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { offsetX.snapTo(0f) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)

        val draggableState = rememberDraggableState { delta ->
            // Accumulate drag
            val newTotal = totalDrag + delta
            // Check direction allowed
            val allowed = when (exitDirection) {
                PredictiveBackExitDirection.ALWAYS_RIGHT -> newTotal > 0
                PredictiveBackExitDirection.ALWAYS_LEFT -> newTotal < 0
                PredictiveBackExitDirection.FOLLOW_GESTURE -> true
            }
            if (!allowed) {
                // If not allowed direction, ignore but don't consume progress
                return@rememberDraggableState
            }
            totalDrag = newTotal
            dragProgress = (abs(newTotal) / widthPx).coerceIn(0f, 1f)

            // InstallerX damped translation: scale down drag for predictive effect
            // AOSP and SCALE have resistance, MIUIX/CLASSIC have full translation with easing
            val easedProgress = BackGestureEasing.transform(dragProgress)
            val damped = when (predictiveBack) {
                PredictiveBackAnimation.SCALE -> {
                    // Scale animation: translation smaller, scale based on eased progress
                    newTotal * (0.6f + 0.2f * (1f - easedProgress))
                }
                PredictiveBackAnimation.AOSP -> {
                    newTotal * (0.8f + 0.1f * (1f - easedProgress))
                }
                else -> newTotal
            }
            scope.launch { offsetX.snapTo(damped) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = isEnabled,
                    onDragStarted = {
                        isDragging = true
                        totalDrag = 0f
                        dragProgress = 0f
                    },
                    onDragStopped = { velocity ->
                        isDragging = false
                        val threshold = widthPx * 0.3f
                        val shouldPop = abs(totalDrag) > threshold || abs(velocity) > 1000f

                        val allowed = when (exitDirection) {
                            PredictiveBackExitDirection.ALWAYS_RIGHT -> totalDrag > 0
                            PredictiveBackExitDirection.ALWAYS_LEFT -> totalDrag < 0
                            PredictiveBackExitDirection.FOLLOW_GESTURE -> true
                        }

                        if (shouldPop && allowed && abs(totalDrag) > 20f) {
                            scope.launch {
                                val target = when {
                                    totalDrag > 0 -> widthPx
                                    else -> -widthPx
                                }
                                // InstallerX commit animation: 200-450ms depending on type
                                val duration = when (predictiveBack) {
                                    PredictiveBackAnimation.SCALE -> 200
                                    PredictiveBackAnimation.AOSP -> 450
                                    PredictiveBackAnimation.CLASSIC -> 200
                                    else -> 250
                                }
                                offsetX.animateTo(
                                    target,
                                    animationSpec = tween(durationMillis = duration, easing = FastOutExtraSlowIn)
                                )
                                onBack()
                                offsetX.snapTo(0f)
                                dragProgress = 0f
                                totalDrag = 0f
                            }
                        } else {
                            scope.launch {
                                offsetX.animateTo(0f, tween(250, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
                                dragProgress = 0f
                                totalDrag = 0f
                            }
                        }
                    }
                )
                .graphicsLayer {
                    translationX = offsetX.value
                    // InstallerX exact predictive scaling/alpha per animation type
                    when (predictiveBack) {
                        PredictiveBackAnimation.SCALE -> {
                            // From ScaleNavTransition: 0.85f + 0.15f * easedProgress
                            val eased = 1f - BackGestureEasing.transform((1f - dragProgress).coerceIn(0f, 1f))
                            val scale = 0.85f + 0.15f * eased
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f
                        }
                        PredictiveBackAnimation.AOSP -> {
                            // AOSP: scale 0.9 + 0.1 * eased, alpha slight fade
                            val eased = 1f - BackGestureEasing.transform((1f - dragProgress).coerceIn(0f, 1f))
                            val scale = 0.9f + 0.1f * eased
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - dragProgress * 0.1f
                        }
                        PredictiveBackAnimation.MIUIX -> {
                            // MIUIX has translation only, no scale (default)
                            alpha = 1f
                        }
                        PredictiveBackAnimation.CLASSIC -> {
                            // Classic: 0.9 + 0.1*progress, alpha fade
                            val scale = 0.9f + 0.1f * (1f - dragProgress)
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - dragProgress * 0.2f
                        }
                        else -> {}
                    }
                }
        ) {
            content()
        }
    }
}
