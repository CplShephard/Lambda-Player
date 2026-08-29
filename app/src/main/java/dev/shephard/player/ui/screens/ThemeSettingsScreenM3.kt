// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package dev.shephard.player.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.twotone.ColorLens
import androidx.compose.material.icons.twotone.InvertColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.toPreferenceInt
import dev.shephard.player.theme.PaletteStyle
import dev.shephard.player.theme.ThemeColorSpec
import dev.shephard.player.theme.ThemeMode
import dev.shephard.player.ui.components.m3.BaseItemContainer
import dev.shephard.player.ui.components.m3.BaseWidget
import dev.shephard.player.ui.components.m3.ColorSwatchPreview
import dev.shephard.player.ui.components.m3.RadioButtonWidget
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.components.m3.SwitchWidget
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.theme.material.PresetColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreenM3(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    val themeMode by prefs.themeModeEnum.collectAsState(initial = ThemeMode.SYSTEM)
    val useMiuix by prefs.useMiuix.collectAsState(initial = true)
    val paletteStyle by prefs.paletteStyle.collectAsState(initial = PaletteStyle.TonalSpot)
    val colorSpec by prefs.colorSpec.collectAsState(initial = ThemeColorSpec.SPEC_2025)
    val dynamicColor by prefs.dynamicColor.collectAsState(initial = false)
    val seedColor by prefs.seedColor.collectAsState(initial = PresetColors.first().color.toArgb())
    val blurEnabled by prefs.liquidGlassEnabled.collectAsState(initial = false)
    val appleFloatingBar by prefs.useAppleFloatingBar.collectAsState(initial = false)

    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showPaletteStyleDialog by remember { mutableStateOf(false) }
    var showColorSpecDialog by remember { mutableStateOf(false) }

    if (showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            title = { Text(strings.themeMode) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    listOf(
                        ThemeMode.LIGHT to strings.lightMode,
                        ThemeMode.DARK to strings.darkMode,
                        ThemeMode.SYSTEM to strings.autoMode,
                    ).forEach { (mode, label) ->
                        M3OptionRow(
                            label = label,
                            selected = mode == themeMode,
                            onClick = {
                                scope.launch { prefs.setThemeMode(mode.toPreferenceInt()) }
                                showThemeModeDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeModeDialog = false }) { Text(strings.close) }
            },
        )
    }

    if (showPaletteStyleDialog) {
        AlertDialog(
            onDismissRequest = { showPaletteStyleDialog = false },
            title = { Text(strings.paletteStyle) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    PaletteStyle.entries.forEach { style ->
                        M3OptionRow(
                            label = style.displayName,
                            selected = style == paletteStyle,
                            onClick = {
                                scope.launch { prefs.setPaletteStyle(style) }
                                showPaletteStyleDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaletteStyleDialog = false }) { Text(strings.close) }
            },
        )
    }

    if (showColorSpecDialog) {
        val isSpec2025Supported = paletteStyle.supportsSpec2025
        val availableSpecs = if (isSpec2025Supported) ThemeColorSpec.entries else listOf(ThemeColorSpec.SPEC_2021)
        AlertDialog(
            onDismissRequest = { showColorSpecDialog = false },
            title = { Text(strings.colorSpec) },
            text = {
                Column {
                    availableSpecs.forEach { spec ->
                        M3OptionRow(
                            label = spec.displayName,
                            selected = spec == colorSpec,
                            onClick = {
                                scope.launch { prefs.setColorSpec(spec) }
                                showColorSpecDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorSpecDialog = false }) { Text(strings.close) }
            },
        )
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(strings.themeSettings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding() + 40.dp
            )
        ) {
            // Group 1: UI Engine & Navigation Style
            item {
                SegmentedColumn(title = strings.uiEngine) {
                    item {
                        RadioButtonWidget(
                            title = strings.googleUi,
                            description = "Material 3 Expressive UI",
                            iconPlaceholder = false,
                            selected = !useMiuix,
                            onClick = { scope.launch { prefs.setUseMiuix(false) } }
                        )
                    }
                    item {
                        RadioButtonWidget(
                            title = strings.miuixUi,
                            description = "Xiaomi HyperOS Miuix UI",
                            iconPlaceholder = false,
                            selected = useMiuix,
                            onClick = { scope.launch { prefs.setUseMiuix(true) } }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = Icons.Default.ViewCarousel,
                            title = strings.appleFloatingBar,
                            description = strings.appleFloatingBarDescription,
                            checked = appleFloatingBar,
                            onCheckedChange = { scope.launch { prefs.setUseAppleFloatingBar(it) } }
                        )
                    }
                }
            }

            // Group 2: Google UI / Material 3 Theme Options
            item {
                SegmentedColumn(title = strings.googleUi) {
                    item {
                        SwitchWidget(
                            title = strings.blurEffect,
                            description = strings.blurEffectDescription,
                            checked = blurEnabled,
                            onCheckedChange = { scope.launch { prefs.setLiquidGlassEnabled(it) } }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Default.DarkMode,
                            title = strings.themeMode,
                            description = when (themeMode) {
                                ThemeMode.LIGHT -> strings.lightMode
                                ThemeMode.DARK -> strings.darkMode
                                ThemeMode.SYSTEM -> strings.autoMode
                            },
                            onClick = { showThemeModeDialog = true }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Default.Palette,
                            title = strings.paletteStyle,
                            description = paletteStyle.displayName,
                            onClick = { showPaletteStyleDialog = true }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Default.Tune,
                            title = strings.colorSpec,
                            description = colorSpec.displayName,
                            onClick = { showColorSpecDialog = true }
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        item {
                            SwitchWidget(
                                icon = Icons.TwoTone.InvertColors,
                                title = strings.dynamicColor,
                                description = strings.dynamicColorDescription,
                                checked = dynamicColor,
                                onCheckedChange = { scope.launch { prefs.setDynamicColor(it) } }
                            )
                        }
                    }
                }
            }

            // Group 3: Monet Theme Presets (18 Swatches)
            item {
                AnimatedVisibility(
                    visible = !dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                        expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                        shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                ) {
                    SegmentedColumn(title = strings.accentColor) {
                        item {
                            BaseItemContainer {
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 16.dp)
                                ) {
                                    val itemMinWidth = 88.dp
                                    val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)
                                    val chunkedColors = PresetColors.chunked(columns)

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        chunkedColors.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                rowItems.forEach { rawColor ->
                                                    Box(
                                                        modifier = Modifier.weight(1f),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        ColorSwatchPreview(
                                                            rawColor = rawColor,
                                                            currentStyle = paletteStyle,
                                                            colorSpec = colorSpec,
                                                            textStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                                            textColor = MaterialTheme.colorScheme.onSurface,
                                                            isSelected = seedColor == rawColor.color.toArgb() &&
                                                                !(dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
                                                        ) {
                                                            scope.launch { prefs.setSeedColor(rawColor.color.toArgb()) }
                                                        }
                                                    }
                                                }

                                                val remaining = columns - rowItems.size
                                                if (remaining > 0) {
                                                    repeat(remaining) {
                                                        Spacer(Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun M3OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
