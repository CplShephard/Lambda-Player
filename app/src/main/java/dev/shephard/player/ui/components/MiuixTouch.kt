package dev.shephard.player.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

/**
 * InstallerX/Miuix-like tactile press feedback for big widgets/cards.
 *
 * Unlike the old bounceClick, this does not overshoot. The surface gently shrinks and bends
 * toward the pressed corner while the finger is down, then springs back when released.
 */
fun Modifier.miuixWidgetClick(
    enabled: Boolean = true,
    pressScale: Float = 0.965f,
    maxTiltDegrees: Float = 2.8f,
    onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val progress by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 520f
        ),
        label = "miuixWidgetPress"
    )

    this
        .onSizeChanged { size = it }
        .graphicsLayer {
            val width = size.width.takeIf { it > 0 } ?: 1
            val height = size.height.takeIf { it > 0 } ?: 1
            val px = (pressOffset.x / width).coerceIn(0f, 1f)
            val py = (pressOffset.y / height).coerceIn(0f, 1f)
            transformOrigin = TransformOrigin(px, py)
            scaleX = 1f - (1f - pressScale) * progress
            scaleY = 1f - (1f - pressScale) * progress
            rotationY = (0.5f - px) * maxTiltDegrees * progress
            rotationX = (py - 0.5f) * maxTiltDegrees * progress
            cameraDistance = 28f * density.density
        }
        .pointerInput(enabled, onClick) {
            detectTapGestures(
                onPress = { offset ->
                    if (enabled) {
                        pressOffset = offset
                        pressed = true
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released) onClick()
                    }
                }
            )
        }
}

/** Simple non-bending press feedback for compact controls such as MiniPlayer buttons. */
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
