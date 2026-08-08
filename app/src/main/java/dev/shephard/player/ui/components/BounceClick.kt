package dev.shephard.player.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shephard.player.ui.glass.GlassTint
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.blurSurfaceCompact

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
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (enabled && isPressed) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = stiffness
        ),
        label = "pressScale"
    )

    this
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/**
 * Convenience composable: a 48dp circular icon button with the bounce-on-tap
 * animation applied. Drop-in replacement for Material3 [dev.shephard.player.ui.miuix.IconButton]
 * — same accessibility semantics (it's still a Button role), same tap target,
 * plus the spring scale on press.
 *
 * We implement it on top of a Box rather than Material3 IconButton so the
 * bounceClick modifier handles the click event (avoiding the double-fire
 * that would happen if we wrapped Material3 IconButton with bounceClick).
 */
/**
 * Convenience composable: a 48dp circular icon button with the bounce-on-tap
 * animation applied. Drop-in replacement for Material3 [dev.shephard.player.ui.miuix.IconButton]
 * — same accessibility semantics (it's still a Button role), same tap target,
 * plus the spring scale on press.
 *
 * We implement it on top of a Box rather than Material3 IconButton so the
 * bounceClick modifier handles the click event (avoiding the double-fire
 * that would happen if we wrapped Material3 IconButton with bounceClick).
 *
 * Liquid Glass entegrasyonu: [LocalBlurEnabled] açıkken ve çağıran taraf bir
 * `backgroundColor` verdiyse (yani dolu bir daire/buton isteniyorsa), düz renk yerine
 * buzlu cam dolgusu + parlak kenarlık uygulanır. Bu sayede uygulamadaki her
 * BouncyIconButton çağrısı (Now Playing sheet, playlist ekranı, dock vb.) tek bir
 * yerden otomatik olarak yeni görünüme geçer.
 */
@Composable
fun BouncyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    contentDescription: String? = null,
    tint: Color = MiuixAppTheme.colorScheme.onSurface,
    iconSize: Dp = 24.dp,
    backgroundColor: Color? = null,
    glassTint: GlassTint = GlassTint.SURFACE
) {
    val liquidGlassOn = LocalBlurEnabled.current
    val boxModifier = modifier
        .size(48.dp)
        .let {
            when {
                backgroundColor != null && liquidGlassOn ->
                    it.blurSurfaceCompact(enabled = true, shape = androidx.compose.foundation.shape.CircleShape, tint = glassTint)
                backgroundColor != null -> it.background(backgroundColor)
                else -> it
            }
        }
        .bounceClick(enabled = enabled, onClick = onClick)
    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = 0.3f),
                modifier = Modifier.size(iconSize)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = 0.3f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
