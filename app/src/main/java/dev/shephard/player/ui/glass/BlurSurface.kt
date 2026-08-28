
package dev.shephard.player.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.shephard.player.ui.miuix.MiuixAppTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class GlassTint { SURFACE, ACCENT }

@Composable
fun Modifier.blurSurface(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: GlassTint = GlassTint.SURFACE,
): Modifier {
    val backdrop = LocalAppBackdrop.current
    if (!enabled || backdrop == null) {
        return this.background(
            color = when (tint) {
                GlassTint.SURFACE -> MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                GlassTint.ACCENT -> MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f)
            },
            shape = shape,
        )
    }
    return when (tint) {
        GlassTint.SURFACE -> this.miuixBlurSurface(backdrop = backdrop, shape = shape)
        GlassTint.ACCENT -> this.miuixBlurAccent(backdrop = backdrop, shape = shape)
    }
}

@Composable
fun Modifier.blurSurfaceCompact(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(16.dp),
    tint: GlassTint = GlassTint.SURFACE,
): Modifier = blurSurface(enabled = enabled, shape = shape, tint = tint)

@Composable
fun Modifier.blurSheetSurface(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
): Modifier {
    val backdrop = LocalAppBackdrop.current
    if (!enabled || backdrop == null) {
        return this.background(MiuixAppTheme.colorScheme.surface, shape)
    }
    return this.miuixBlurSurface(
        backdrop = backdrop,
        shape = shape,
        blurRadius = 30f,
        tintAlpha = 0.85f,
    )
}
