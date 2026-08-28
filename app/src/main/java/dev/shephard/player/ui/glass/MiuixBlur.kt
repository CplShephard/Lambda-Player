
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

val isBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val isLiquidGlassSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

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
