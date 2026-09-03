
package dev.shephard.player.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.shephard.player.ui.miuix.MiuixAppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import dev.shephard.player.ui.theme.LocalUseMiuix

val LocalBlurEnabled = staticCompositionLocalOf { false }

val LocalAppBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

val LocalContentBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Foreground colour to use for text that is drawn *directly on top of the
 * wallpaper* (headings, section titles, labels — anything without a card
 * behind it). `MainContainer` provides a value derived from the wallpaper
 * brightness whenever a wallpaper is set, so text stays readable whether the
 * wallpaper is bright or dark. When no wallpaper is configured this stays
 * [Color.Unspecified] and callers fall back to the theme's normal text colour.
 */
val LocalWallpaperContentColor = staticCompositionLocalOf<Color> { Color.Unspecified }

/**
 * Returns the wallpaper-aware foreground colour for text that sits directly on
 * the wallpaper. Falls back to the theme's [MiuixAppTheme.colorScheme.onBackground]
 * when no wallpaper is set.
 */
@Composable
fun wallpaperAdaptiveTextColor(fallback: Color = MiuixAppTheme.colorScheme.onBackground): Color {
    val wallpaperColor = LocalWallpaperContentColor.current
    return if (wallpaperColor != Color.Unspecified) wallpaperColor else fallback
}

val isBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val isLiquidGlassSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Remembers a page-level backdrop for top-bar blur, using exactly the same
 * recipe as InstallerX Revived's `rememberMiuixBlurBackdrop`: the backdrop
 * paints a solid theme-surface base BEFORE capturing the page content. The
 * base fill is what keeps small top bars from smearing/streaking the content
 * while the page scrolls underneath the glass.
 */
@Composable
fun rememberMiuixPageBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixAppTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Apply the InstallerX Revived Miuix top-bar blur to a SmallTopAppBar /
 * TopAppBar modifier: InstallerX values — 25dp gaussian radius blended with
 * the theme surface at 80% opacity (its `installerMiuixBlurEffect` defaults).
 *
 * @param backdrop The page backdrop captured from the content underneath.
 * @param blurRadius Gaussian blur radius in pixels (25f = InstallerX default).
 * @param tintAlpha Surface tint opacity (0.8f = InstallerX default).
 */
@Composable
fun Modifier.miuixTopBarBlur(
    backdrop: LayerBackdrop?,
    blurRadius: Float = 25f,
    tintAlpha: Float = 0.8f,
): Modifier {
    if (backdrop == null) return this

    val blendColor = MiuixAppTheme.colorScheme.surface.copy(alpha = tintAlpha)
    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor),
                ),
            ),
        ),
    )
}

/**
 * Remembers the global app blur backdrop.
 *
 * The backdrop's draw block paints a base surface fill followed by the captured
 * content of the modifier that owns the backdrop (e.g. wallpaper, content pages,
 * mini player, etc.). To make sure glass effects (Apple dock, mini player, top
 * bars) actually see the wallpaper, callers should attach the resulting backdrop
 * to a modifier that wraps both the wallpaper layer AND the content.
 */
@Composable
fun rememberAppBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isLiquidGlassSupported || !isRenderEffectSupported()) return null
    val useMiuix = LocalUseMiuix.current
    val surfaceColor = if (useMiuix) {
        top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
    }
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Remembers a dedicated wallpaper blur backdrop. The wallpaper itself is
 * rendered by the caller as a normal composable (an `AsyncImage` in a `Box`)
 * wrapped by `Modifier.layerBackdrop(<this>)`. By keeping the draw block here
 * minimal (just the base surface + `drawContent()`) we let miuix's layer
 * capture the wallpaper together with the darkening overlay so any
 * liquid-glass surface (Apple dock, mini player pop-up) bound to the same
 * backdrop can sample the wallpaper underneath.
 */
@Composable
fun rememberWallpaperBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isLiquidGlassSupported || !isRenderEffectSupported()) return null
    val useMiuix = LocalUseMiuix.current
    val surfaceColor = if (useMiuix) {
        top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
    }
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun Modifier.miuixBlurSurface(
    backdrop: LayerBackdrop? = LocalAppBackdrop.current,
    shape: Shape = RoundedCornerShape(20.dp),
    blurRadius: Float = 25f,
    tintAlpha: Float = 0.8f,
    fallbackColor: Color = MiuixAppTheme.colorScheme.surfaceVariant,
): Modifier {
    if (backdrop == null) return this.background(fallbackColor, shape)

    val blendColor = MiuixAppTheme.colorScheme.surface.copy(alpha = tintAlpha)
    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(BlendColorEntry(color = blendColor)),
            ),
        )
    )
}

@Composable
fun Modifier.miuixBlurAccent(
    backdrop: LayerBackdrop? = LocalAppBackdrop.current,
    shape: Shape = RoundedCornerShape(16.dp),
    blurRadius: Float = 20f,
    tintAlpha: Float = 0.55f,
    fallbackColor: Color = MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f),
): Modifier {
    if (backdrop == null) return this.background(fallbackColor, shape)

    val blendColor = MiuixAppTheme.colorScheme.primary.copy(alpha = tintAlpha)
    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(BlendColorEntry(color = blendColor)),
            ),
        )
    )
}

@Composable
fun Modifier.miuixBlurSheet(
    backdrop: LayerBackdrop? = LocalAppBackdrop.current,
    shape: Shape = RectangleShape,
    blurRadius: Float = 30f,
    tintAlpha: Float = 0.85f,
    fallbackColor: Color = MiuixAppTheme.colorScheme.surface,
): Modifier = miuixBlurSurface(
    backdrop = backdrop,
    shape = shape,
    blurRadius = blurRadius,
    tintAlpha = tintAlpha,
    fallbackColor = fallbackColor,
)

@Composable
fun LayerBackdrop?.appBarColor(): Color =
    this?.let { Color.Transparent } ?: MiuixAppTheme.colorScheme.surface
