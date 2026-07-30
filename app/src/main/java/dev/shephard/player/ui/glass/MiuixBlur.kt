// Lambda Player — Miuix blur surface layer.
//
// Replaces the app's previous hand-rolled `LiquidGlass.kt` (which only blurred its *own*
// fill layer) with the real Miuix backdrop pipeline: content behind a surface is captured
// into a GraphicsLayer and genuinely blurred/refracted through AGSL shaders.
//
// Mirrors InstallerX Revived's `ui/theme/Backdrop.kt` approach, adapted to Material 3.
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

/**
 * Whether the user enabled the blur ("Miuix Blur") appearance in Settings.
 * Provided from [dev.shephard.player.MainActivity] out of DataStore.
 */
val LocalBlurEnabled = staticCompositionLocalOf { false }

/**
 * The app-wide backdrop that blurred surfaces sample from. `null` when blur is off or the
 * device can't render it, in which case every call site falls back to an opaque surface.
 */
val LocalAppBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Backdrop recorded from the *page content* (the NavHost). ONLY composables that are drawn
 * OUTSIDE that content — the floating dock, the mini player, the brand header — may sample
 * this one.
 *
 * Sampling it from inside the content (e.g. a card in SettingsScreen) makes the layer both
 * the source and a consumer of itself, which is exactly what made the app crash the moment
 * the "Liquid Glass" switch was turned on. Everything inside the content must use
 * [LocalAppBackdrop] (the background-only backdrop) instead.
 */
val LocalContentBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** True when this device can actually run the Miuix blur pipeline (RenderEffect, API 31+). */
val isBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * True when the full "Liquid Glass" dock (AGSL RuntimeShader: lens refraction, chromatic
 * aberration, bloom highlight) can run. Requires API 33+, exactly like InstallerX.
 */
val isLiquidGlassSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Remembers the [LayerBackdrop] used as the blur source, backed by a solid surface colour so
 * translucent content doesn't produce alpha-blending artifacts.
 *
 * @return a backdrop when blur is enabled and supported, otherwise `null`.
 */
@Composable
fun rememberAppBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    // miuix-blur itself declares minSdk 33 and its AGSL shader paths need RuntimeShader
    // (API 33). The manifest overrides that with tools:overrideLibrary, so on API 31/32 the
    // classes load but the shader entry points blow up at draw time. Gate on API 33 as well
    // as on RenderEffect support so those devices simply get the opaque fallback.
    if (!enableBlur || !isLiquidGlassSupported || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixAppTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Applies the standard Miuix glassmorphism blur to a surface (cards, sheets, dialogs, headers).
 *
 * When [backdrop] is `null` (blur off / unsupported) this falls back to the opaque
 * [fallbackColor] so the caller never has to branch itself.
 *
 * @param backdrop The backdrop to sample; usually [LocalAppBackdrop].
 * @param shape Clipping shape of the blurred area.
 * @param blurRadius Gaussian blur radius in pixels.
 * @param tintAlpha Opacity of the surface colour blended over the blur.
 */
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

/**
 * Accent-tinted variant of [miuixBlurSurface] — used for selected/active chips and buttons
 * where the previous implementation used `GlassTint.ACCENT`.
 */
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

/**
 * Blur for full-bleed sheet/dialog surfaces (no corner rounding assumptions, squared edges
 * by default since the host component already clips).
 */
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

/** Background colour for bars/headers: transparent when a backdrop paints them, else solid. */
@Composable
fun LayerBackdrop?.appBarColor(): Color =
    this?.let { Color.Transparent } ?: MiuixAppTheme.colorScheme.surface
