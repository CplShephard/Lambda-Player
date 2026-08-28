package dev.shephard.player.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.TiltFeedback
import top.yukonga.miuix.kmp.utils.pressable

fun Modifier.miuixWidgetClick(
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") pressScale: Float = 0.94f,
    maxTiltDegrees: Float = 8f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val tiltFeedback = remember(maxTiltDegrees) { TiltFeedback(tiltAmount = maxTiltDegrees) }

    this
        .pressable(
            interactionSource = interactionSource,
            indication = tiltFeedback,
            enabled = enabled,
            delay = null
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

fun Modifier.miuixSinkClick(
    enabled: Boolean = true,
    sinkAmount: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val sinkFeedback = remember(sinkAmount) { SinkFeedback(sinkAmount = sinkAmount) }

    this
        .pressable(
            interactionSource = interactionSource,
            indication = sinkFeedback,
            enabled = enabled,
            delay = null
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

fun Modifier.pressScaleClick(
    enabled: Boolean = true,
    pressScale: Float = 0.92f,
    onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 650f
        ),
        label = "pressScaleClick"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled, onClick) {
            detectTapGestures(
                onPress = {
                    if (enabled) {
                        pressed = true
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released) onClick()
                    }
                }
            )
        }
}
