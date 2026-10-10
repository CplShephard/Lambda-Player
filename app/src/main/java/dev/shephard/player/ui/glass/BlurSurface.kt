
package dev.shephard.player.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.shephard.player.ui.miuix.MiuixAppTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class GlassTint { SURFACE, ACCENT }

/**
 * Solid "glass" surface used by small controls. These surfaces used to blur the custom app
 * wallpaper; with the wallpaper feature removed there is nothing behind them to sample, so they
 * always use their solid theme colours. [enabled] is kept so existing call sites stay unchanged.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun Modifier.blurSurface(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: GlassTint = GlassTint.SURFACE,
): Modifier = this.background(
    color = when (tint) {
        GlassTint.SURFACE -> MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        GlassTint.ACCENT -> MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f)
    },
    shape = shape,
)

@Composable
fun Modifier.blurSurfaceCompact(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(16.dp),
    tint: GlassTint = GlassTint.SURFACE,
): Modifier = blurSurface(enabled = enabled, shape = shape, tint = tint)

@Suppress("UNUSED_PARAMETER")
@Composable
fun Modifier.blurSheetSurface(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
): Modifier = this.background(MiuixAppTheme.colorScheme.surface, shape)
