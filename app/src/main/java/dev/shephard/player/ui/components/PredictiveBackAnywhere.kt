// SPDX-License-Identifier: GPL-3.0-only
// Custom predictive back that works from anywhere (InstallerX Revived style)
package dev.shephard.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Wraps content with anywhere-drag predictive back.
 * When user drags horizontally from anywhere, shows predictive animation and triggers onBack if threshold exceeded.
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

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var dragDirection by remember { mutableStateOf(0) } // 1 = right, -1 = left
    val velocityTracker = remember { VelocityTracker() }

    // Threshold: 30% of width or 100dp
    LaunchedEffect(Unit) { offsetX.snapTo(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(exitDirection, predictiveBack) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        velocityTracker.resetTracking()
                        dragDirection = 0
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        // Only handle if horizontal drag dominates
                        // Check if we should allow this direction based on exitDirection
                        val absDrag = abs(dragAmount)
                        if (absDrag < 1f) return@detectHorizontalDragGestures

                        val newTotal = totalDrag + dragAmount
                        totalDrag = newTotal

                        // Determine direction
                        val currentDir = if (newTotal > 0) 1 else -1
                        if (dragDirection == 0) dragDirection = currentDir

                        // Respect exitDirection for allowed swipe
                        val allowed = when (exitDirection) {
                            PredictiveBackExitDirection.ALWAYS_RIGHT -> newTotal > 0
                            PredictiveBackExitDirection.ALWAYS_LEFT -> newTotal < 0
                            PredictiveBackExitDirection.FOLLOW_GESTURE -> true
                        }

                        if (!allowed) {
                            // Don't consume if not allowed direction, let parent handle
                            return@detectHorizontalDragGestures
                        }

                        // Apply resistance and update offset
                        // Scale down drag for predictive effect
                        val damped = when (predictiveBack) {
                            PredictiveBackAnimation.SCALE -> newTotal * 0.6f
                            PredictiveBackAnimation.AOSP -> newTotal * 0.8f
                            else -> newTotal
                        }

                        scope.launch { offsetX.snapTo(damped) }
                        dragProgress = (abs(newTotal) / size.width.toFloat()).coerceIn(0f, 1f)

                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity().x
                        val width = size.width.toFloat()
                        val threshold = width * 0.3f
                        val shouldPop = abs(totalDrag) > threshold || abs(velocity) > 1000f

                        if (shouldPop && abs(totalDrag) > 20f) {
                            // Check direction allowed again
                            val allowed = when (exitDirection) {
                                PredictiveBackExitDirection.ALWAYS_RIGHT -> totalDrag > 0
                                PredictiveBackExitDirection.ALWAYS_LEFT -> totalDrag < 0
                                PredictiveBackExitDirection.FOLLOW_GESTURE -> true
                            }
                            if (allowed) {
                                scope.launch {
                                    val target = when {
                                        totalDrag > 0 -> width
                                        else -> -width
                                    }
                                    offsetX.animateTo(
                                        target,
                                        animationSpec = tween(durationMillis = 250)
                                    )
                                    onBack()
                                    offsetX.snapTo(0f)
                                    dragProgress = 0f
                                }
                            } else {
                                scope.launch {
                                    offsetX.animateTo(0f, tween(250))
                                    dragProgress = 0f
                                }
                            }
                        } else {
                            scope.launch {
                                offsetX.animateTo(0f, tween(250))
                                dragProgress = 0f
                            }
                        }
                        totalDrag = 0f
                        dragDirection = 0
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f, tween(250))
                            dragProgress = 0f
                        }
                        totalDrag = 0f
                        dragDirection = 0
                    }
                )
            }
            .graphicsLayer {
                translationX = offsetX.value
                // Add predictive scaling/alpha based on animation type
                when (predictiveBack) {
                    PredictiveBackAnimation.SCALE -> {
                        val scale = 1f - (dragProgress * 0.08f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (dragProgress * 0.15f)
                    }
                    PredictiveBackAnimation.AOSP -> {
                        alpha = 1f - (dragProgress * 0.1f)
                    }
                    PredictiveBackAnimation.MIUIX -> {
                        // MIUIX has translation only, no scale
                    }
                    PredictiveBackAnimation.CLASSIC -> {
                        alpha = 1f - (dragProgress * 0.2f)
                    }
                    else -> {}
                }
            }
    ) {
        content()
    }
}
