package dev.shephard.player.ui.glass.bgeffect

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import kotlin.math.floor

/**
 * MADDE 9 — About sayfasının arkasındaki dinamik ışık.
 *
 * InstallerX Revived'ın `BgEffectBackground`'unun Lambda'ya uyarlanmış hâli:
 *  * cihaz tipi ayrımı (PHONE/PAD) kaldırıldı — telefon yerleşimi kullanılıyor,
 *  * [surface] `Color.Transparent` verilebiliyor; böylece efekt, Lambda'nın duvar
 *    kağıdı/arka planının ÜZERİNE biniyor, onu gizlemiyor,
 *  * renk paleti morumsu/mavimsi yerine yeşilimsi/açık mavimsi (bkz. [BgEffectConfig]).
 *
 * AGSL runtime shader gerektirir (Android 13 / API 33+). Desteklenmeyen cihazlarda
 * içerik hiçbir efekt olmadan aynen çizilir — yani sessizce devre dışı kalır.
 */
@Composable
fun BgEffectBackground(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    dynamicBackground: Boolean = true,
    effectBackground: Boolean = true,
    isFullSize: Boolean = false,
    surface: Color = Color.Transparent,
    alpha: () -> Float = { 1f },
    content: @Composable (BoxScope.() -> Unit),
) {
    val shaderSupported = remember { isRuntimeShaderSupported() }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !shaderSupported) {
        Box(modifier = modifier, content = content)
        return
    }

    Box(modifier = modifier) {
        val painter = remember { BgEffectPainter() }
        val preset = remember(isDarkTheme) { BgEffectConfig.get(isDarkTheme) }

        // Üç renk kümesi arasında yavaşça gezinen "sahne" sayacı.
        val colorStage = remember { Animatable(0f) }

        LaunchedEffect(dynamicBackground, preset) {
            if (!dynamicBackground) return@LaunchedEffect
            val animatesColors =
                preset.colors1 !== preset.colors2 || preset.colors2 !== preset.colors3
            if (!animatesColors) return@LaunchedEffect

            var targetStage = floor(colorStage.value) + 1f
            while (isActive) {
                delay((preset.colorInterpPeriod * 500).toLong())
                colorStage.animateTo(
                    targetValue = targetStage,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 35f),
                )
                targetStage += 1f
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .bgEffectDraw(
                    painter = painter,
                    preset = preset,
                    isDarkTheme = isDarkTheme,
                    surface = surface,
                    effectBackground = effectBackground,
                    isFullSize = isFullSize,
                    playing = dynamicBackground,
                    colorStage = { colorStage.value },
                    alpha = alpha,
                ),
        )
        content()
    }
}
