// Lambda Player — blurred surface modifiers built on the real Miuix blur pipeline.
//
// These replace the app's removed hand-rolled `LiquidGlass.kt`, which only blurred its own
// fill layer and therefore never actually frosted the content behind it. Every modifier here
// samples the shared [LocalAppBackdrop] (a GraphicsLayer capture of the app content) and runs
// it through Miuix's AGSL blur shaders, so what you see behind a card really is blurred.
//
// All of them degrade gracefully: when blur is switched off in Settings, or the device is
// below API 31, they fall back to an opaque Miuix surface, so call sites never branch.
package dev.shephard.player.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.shephard.player.ui.miuix.MiuixAppTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Tint of a blurred surface: neutral surface colour, or the theme accent. */
enum class GlassTint { SURFACE, ACCENT }

/**
 * Standard blurred surface — cards, headers, panels.
 *
 * @param enabled Pass the user preference; `false` renders a plain opaque surface.
 * @param shape Clipping shape of the blurred region.
 * @param tint Neutral or accent-tinted glass.
 */
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

/** Compact variant for icon buttons, chips and dock items (smaller default corner radius). */
@Composable
fun Modifier.blurSurfaceCompact(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(16.dp),
    tint: GlassTint = GlassTint.SURFACE,
): Modifier = blurSurface(enabled = enabled, shape = shape, tint = tint)

/**
 * Blur for sheet / dialog surfaces. Apply to the sheet's root container while keeping the
 * host component's own `containerColor` transparent.
 */
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
