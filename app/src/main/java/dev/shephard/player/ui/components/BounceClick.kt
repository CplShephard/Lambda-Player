package dev.shephard.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Reusable tap-feedback Modifier: shrinks on press-down, then springs back
 * overshooting slightly, then settles. Drop-in replacement for [clickable]
 * anywhere we want a tactile, "alive" feel.
 *
 * Behavior matches the spring animation originally used on the brand logo
 * but exposed as a Modifier extension so every clickable surface in the app
 * can opt in by wrapping `Modifier.bounceClick(onClick = ...)` around it.
 *
 * Usage:
 *   IconButton(
 *       onClick = { ... },
 *       modifier = Modifier.bounceClick { /* action */ }
 *   )
 *
 * Or, on any clickable surface:
 *   Box(modifier = Modifier.bounceClick { /* action */ }) { ... }
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressScale: Float = 0.86f,
    overshoot: Float = 1.12f,
    damping: Float = 0.45f,
    stiffness: Float = 600f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    this
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                scope.launch {
                    scale.animateTo(pressScale, tween(80))
                    scale.animateTo(overshoot, spring(dampingRatio = damping, stiffness = stiffness))
                    scale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = stiffness))
                }
                onClick()
            }
        )
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}

/**
 * Convenience composable: a 48dp circular icon button with the bounce-on-tap
 * animation applied. Drop-in replacement for Material3 [androidx.compose.material3.IconButton]
 * — same accessibility semantics (it's still a Button role), same tap target,
 * plus the spring scale on press.
 *
 * We implement it on top of a Box rather than Material3 IconButton so the
 * bounceClick modifier handles the click event (avoiding the double-fire
 * that would happen if we wrapped Material3 IconButton with bounceClick).
 */
@Composable
fun BouncyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconSize: Dp = 24.dp,
    backgroundColor: Color? = null
) {
    val boxModifier = modifier
        .size(48.dp)
        .let { if (backgroundColor != null) it.background(backgroundColor) else it }
        .bounceClick(enabled = enabled, onClick = onClick)
    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.3f),
            modifier = Modifier.size(iconSize)
        )
    }
}
