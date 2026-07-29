package dev.shephard.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.shephard.player.player.ThemeModePreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun LambdaPlayerTheme(
    accentArgb: Int = 0xFF22C55E.toInt(),
    darkTheme: Boolean = false,
    themeMode: Int = if (darkTheme) ThemeModePreference.DARK else ThemeModePreference.LIGHT,
    dynamicColor: Boolean = false,
    cardAlpha: Float = 0.85f,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeModePreference.AUTO -> isSystemInDarkTheme()
        ThemeModePreference.DARK -> true
        else -> false
    }

    val accentColor = Color(accentArgb)
    val accentLuminance = 0.2126f * accentColor.red + 0.7152f * accentColor.green + 0.0722f * accentColor.blue
    val onAccent = if (accentLuminance > 0.35f) Color(0xFF111111) else Color.White

    val colors = if (useDarkTheme) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = onAccent,
            background = Color(0xFF000000),
            onBackground = Color(0xFFF5F5F7),
            surface = Color(0xFF101012),
            onSurface = Color(0xFFF5F5F7),
            surfaceVariant = Color(0xFF1C1C1E),
            surfaceContainer = Color(0xFF1C1C1E),
            surfaceContainerHigh = Color(0xFF222226),
            surfaceContainerHighest = Color(0xFF2A2A2E),
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = onAccent,
            background = Color(0xFFF7F7F8),
            onBackground = Color(0xFF111113),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF111113),
            surfaceVariant = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFFFFFFF),
            surfaceContainerHighest = Color(0xFFFFFFFF),
        )
    }

    MiuixTheme(colors = colors, content = content)
}
