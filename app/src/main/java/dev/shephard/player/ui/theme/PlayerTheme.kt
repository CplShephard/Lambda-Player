// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package dev.shephard.player.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import dev.shephard.player.theme.PaletteStyle
import dev.shephard.player.theme.ThemeColorSpec
import dev.shephard.player.theme.ThemeMode
import dev.shephard.player.theme.ThemeState
import dev.shephard.player.ui.theme.material.animateAsState
import dev.shephard.player.ui.theme.material.dynamicColorScheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec as MiuixColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle as MiuixPaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

private val LocalIsDark = staticCompositionLocalOf { false }
private val LocalPaletteStyle = staticCompositionLocalOf { PaletteStyle.Expressive }
private val LocalThemeColorSpec = staticCompositionLocalOf { ThemeColorSpec.SPEC_2025 }
private val LocalSeedColor = staticCompositionLocalOf { Color.Unspecified }
private val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
private val LocalUseMiuixMonet = staticCompositionLocalOf { false }
private val LocalUseDynamicColor = staticCompositionLocalOf { false }

val LocalPlayerColorScheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }

val LocalUseMiuix = staticCompositionLocalOf { true }

object PlayerTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalPlayerColorScheme.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDark.current

    val seedColor: Color
        @Composable @ReadOnlyComposable
        get() = LocalSeedColor.current

    val paletteStyle: PaletteStyle
        @Composable @ReadOnlyComposable
        get() = LocalPaletteStyle.current

    val colorSpec: ThemeColorSpec
        @Composable @ReadOnlyComposable
        get() = LocalThemeColorSpec.current

    val themeMode: ThemeMode
        @Composable @ReadOnlyComposable
        get() = LocalThemeMode.current

    val useMiuixMonet: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalUseMiuixMonet.current

    val useDynamicColor: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalUseDynamicColor.current
}

@Composable
fun PlayerTheme(state: ThemeState, content: @Composable () -> Unit) {
    PlayerTheme(
        useMiuix = state.useMiuix,
        themeMode = state.themeMode,
        paletteStyle = state.paletteStyle,
        colorSpec = state.colorSpec,
        useDynamicColor = state.useDynamicColor,
        useMiuixMonet = state.useMiuixMonet,
        seedColor = Color(state.seedColor),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerTheme(
    useMiuix: Boolean,
    themeMode: ThemeMode,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    useDynamicColor: Boolean,
    useMiuixMonet: Boolean,
    seedColor: Color,
    content: @Composable () -> Unit,
) {
    val preservedContent = remember {
        movableContentOf<@Composable () -> Unit> { targetContent ->
            targetContent()
        }
    }

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(id = android.R.color.system_accent1_500)
    } else {
        seedColor
    }

    val baseColorScheme = remember(keyColor, isDark, paletteStyle, colorSpec) {
        dynamicColorScheme(
            keyColor = keyColor,
            isDark = isDark,
            style = paletteStyle,
            colorSpec = colorSpec,
        )
    }

    val animatedColorScheme = baseColorScheme.animateAsState()

    CompositionLocalProvider(
        LocalIsDark provides isDark,
        LocalPaletteStyle provides paletteStyle,
        LocalSeedColor provides seedColor,
        LocalPlayerColorScheme provides animatedColorScheme,
        LocalThemeMode provides themeMode,
        LocalUseMiuixMonet provides useMiuixMonet,
        LocalUseDynamicColor provides useDynamicColor,
        LocalThemeColorSpec provides colorSpec,
        LocalUseMiuix provides useMiuix,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NavigationBarContrastHandler()
        }

        if (useMiuix) {
            PlayerMiuixTheme(
                darkTheme = isDark,
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
                useMiuixMonet = useMiuixMonet,
                seedColor = seedColor,
                paletteStyle = paletteStyle,
                colorSpec = colorSpec,
            ) {
                preservedContent(content)
            }
        } else {
            PlayerMaterialExpressiveTheme(
                darkTheme = isDark,
                colorScheme = animatedColorScheme,
            ) {
                preservedContent(content)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerMaterialExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme,
    compatStatusBarColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content,
    )
}

@Composable
fun PlayerMiuixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: ThemeMode,
    useMiuixMonet: Boolean,
    useDynamicColor: Boolean = false,
    compatStatusBarColor: Boolean = true,
    seedColor: Color,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    content: @Composable () -> Unit,
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    if (useMiuixMonet) {
        val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            colorResource(id = android.R.color.system_accent1_500)
        } else {
            seedColor
        }

        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            ThemeMode.DARK -> ColorSchemeMode.MonetDark
        }

        val style = when (paletteStyle) {
            PaletteStyle.TonalSpot -> MiuixPaletteStyle.TonalSpot
            PaletteStyle.Neutral -> MiuixPaletteStyle.Neutral
            PaletteStyle.Vibrant -> MiuixPaletteStyle.Vibrant
            PaletteStyle.Expressive -> MiuixPaletteStyle.Expressive
            PaletteStyle.Rainbow -> MiuixPaletteStyle.Rainbow
            PaletteStyle.FruitSalad -> MiuixPaletteStyle.FruitSalad
            PaletteStyle.Monochrome -> MiuixPaletteStyle.Monochrome
            PaletteStyle.Fidelity -> MiuixPaletteStyle.Fidelity
            PaletteStyle.Content -> MiuixPaletteStyle.Content
        }

        val colorSpecVersion = when (colorSpec) {
            ThemeColorSpec.SPEC_2025 -> if (paletteStyle.supportsSpec2025) MiuixColorSpec.Spec2025 else MiuixColorSpec.Spec2021
            ThemeColorSpec.SPEC_2021 -> MiuixColorSpec.Spec2021
        }

        val controller = remember(colorSchemeMode, keyColor, paletteStyle, colorSpecVersion, darkTheme) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = keyColor,
                paletteStyle = style,
                colorSpec = colorSpecVersion,
                isDark = darkTheme,
            )
        }

        MiuixTheme(
            controller = controller,
            content = content,
        )
    } else {
        // --- Default Miuix Theme Path (identical to InstallerX Revived) ---
        // When Miuix Custom Colors are disabled we hand the theme over to
        // Miuix's own default scheme via ThemeController(System/Light/Dark)
        // instead of a hand-rolled ColorScheme. This is what keeps the app
        // looking exactly like stock Miuix: white switch thumbs, MIUI blue
        // accent (#3482FF), stock cards/surfaces, etc.
        //
        // Only the page background is overridden back to Lambda's original
        // colors so the cards keep their contrast against the page:
        //   dark  -> pure black  #000000
        //   light -> #F7F7F8 (not pure white)
        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.System
            ThemeMode.LIGHT -> ColorSchemeMode.Light
            ThemeMode.DARK -> ColorSchemeMode.Dark
        }

        val lightColors = lightColorScheme(
            background = Color(0xFFF7F7F8),
            onBackground = Color(0xFF111113),
        )
        val darkColors = darkColorScheme(
            background = Color(0xFF000000),
            onBackground = Color(0xFFF5F5F7),
        )

        val controller = remember(colorSchemeMode, darkTheme) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                lightColors = lightColors,
                darkColors = darkColors,
                isDark = darkTheme,
            )
        }

        MiuixTheme(
            controller = controller,
            content = content,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun NavigationBarContrastHandler() {
    val configuration = LocalConfiguration.current
    val activity = LocalActivity.current

    DisposableEffect(configuration) {
        val window = activity?.window
        window?.isNavigationBarContrastEnforced = false

        onDispose { }
    }
}
