// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.toPreferenceInt
import dev.shephard.player.theme.PaletteStyle
import dev.shephard.player.theme.ThemeColorSpec
import dev.shephard.player.theme.ThemeMode
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.theme.material.PresetColors
import kotlin.math.abs
import kotlinx.coroutines.launch

private val M3SeedPalette = PresetColors.map { it.color.toArgb() }

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
    val seedColor by prefs.seedColor.collectAsState(initial = M3SeedPalette.first())
    val blurEnabled by prefs.liquidGlassEnabled.collectAsState(initial = false)
    val appleFloatingBar by prefs.useAppleFloatingBar.collectAsState(initial = false)

    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showPaletteStyleDialog by remember { mutableStateOf(false) }
    var showColorSpecDialog by remember { mutableStateOf(false) }
    var customPickerOpen by remember { mutableStateOf(false) }

    val dynamicColorLocked = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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

    if (customPickerOpen && !dynamicColorLocked) {
        CustomColorPickerDialogM3(
            onDismiss = { customPickerOpen = false },
            onColorPicked = { argb ->
                scope.launch { prefs.setSeedColor(argb) }
            },
            initialArgb = seedColor,
            title = strings.customColorTitle,
            hexPlaceholder = strings.hexPlaceholder
        )
    }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item { M3SectionTitle(strings.uiEngine) }
            item {
                M3OptionRow(
                    label = strings.miuixUi,
                    selected = useMiuix,
                    onClick = { scope.launch { prefs.setUseMiuix(true) } }
                )
                M3OptionRow(
                    label = strings.googleUi,
                    selected = !useMiuix,
                    onClick = { scope.launch { prefs.setUseMiuix(false) } }
                )
            }

            item { M3SectionTitle(strings.themeMode) }
            item {
                M3ClickRow(
                    title = strings.themeMode,
                    value = when (themeMode) {
                        ThemeMode.LIGHT -> strings.lightMode
                        ThemeMode.DARK -> strings.darkMode
                        ThemeMode.SYSTEM -> strings.autoMode
                    },
                    onClick = { showThemeModeDialog = true }
                )
            }

            item { M3SectionTitle(strings.appearance) }
            item {
                M3SwitchRow(strings.blurEffect, blurEnabled) { scope.launch { prefs.setLiquidGlassEnabled(it) } }
                M3SwitchRow(strings.appleFloatingBar, appleFloatingBar) { scope.launch { prefs.setUseAppleFloatingBar(it) } }
            }

            item { M3SectionTitle(strings.paletteStyle) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    M3SwitchRow(strings.dynamicColor, dynamicColor) { scope.launch { prefs.setDynamicColor(it) } }
                }
            }
            item {
                M3ClickRow(
                    title = strings.paletteStyle,
                    value = paletteStyle.displayName,
                    onClick = { showPaletteStyleDialog = true }
                )
            }
            item {
                M3ClickRow(
                    title = strings.colorSpec,
                    value = colorSpec.displayName,
                    onClick = { showColorSpecDialog = true }
                )
            }

            item { M3SectionTitle(strings.customColorTitle) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    M3SeedPalette.forEach { argb ->
                        val selected = argb == seedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .clickable(enabled = !dynamicColorLocked) {
                                    scope.launch { prefs.setSeedColor(argb) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFFACC15),
                                        Color(0xFF22C55E), Color(0xFF14B8A6), Color(0xFF3B82F6),
                                        Color(0xFF8B5CF6), Color(0xFFEF4444)
                                    )
                                )
                            )
                            .clickable(enabled = !dynamicColorLocked) { customPickerOpen = true }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun M3SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun M3SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun M3ClickRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

@Composable
fun CustomColorPickerDialogM3(
    onDismiss: () -> Unit,
    onColorPicked: (Int) -> Unit,
    initialArgb: Int = 0xFF4A672D.toInt(),
    title: String = "Pick a custom color",
    hexPlaceholder: String = "#RRGGBB",
) {
    val strings = LocalStrings.current
    val initialHsv = remember(initialArgb) { colorToHsv(Color(initialArgb)) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf("") }

    val currentColor = hsvToColor(hue, sat, value)
    val hueColor = hsvToColor(hue, 1f, 1f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                )
                Spacer(Modifier.height(16.dp))
                GradientSlider(
                    colors = listOf(
                        Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000),
                    ),
                    fraction = hue,
                    onFractionChange = { hue = it }
                )
                Spacer(Modifier.height(12.dp))
                GradientSlider(
                    colors = listOf(Color.White, hueColor),
                    fraction = sat,
                    onFractionChange = { sat = it }
                )
                Spacer(Modifier.height(12.dp))
                GradientSlider(
                    colors = listOf(Color.Black, hsvToColor(hue, sat, 1f)),
                    fraction = value,
                    onFractionChange = { value = it }
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        val cleaned = input.filter { it in "0123456789abcdefABCDEF#" }.take(7)
                        hexText = cleaned
                        if (cleaned.length == 7 && cleaned[0] == '#') {
                            runCatching {
                                val rgb = Color(android.graphics.Color.parseColor(cleaned))
                                val parsed = colorToHsv(rgb)
                                hue = parsed[0]
                                sat = parsed[1]
                                value = parsed[2]
                            }
                        }
                    },
                    label = { Text(hexPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorPicked(currentColor.toArgb())
                    onDismiss()
                }
            ) { Text(strings.apply) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        },
    )
}

@Composable
private fun GradientSlider(
    colors: List<Color>,
    fraction: Float,
    onFractionChange: (Float) -> Unit,
) {
    val thumbSize = 24.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(colors))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onFractionChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onFractionChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - thumbSize) * fraction.coerceIn(0f, 1f))
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hue = h.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1f - abs((hue * 6f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        hue < 1f / 6f -> Triple(c, x, 0f)
        hue < 2f / 6f -> Triple(x, c, 0f)
        hue < 3f / 6f -> Triple(0f, c, x)
        hue < 4f / 6f -> Triple(0f, x, c)
        hue < 5f / 6f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

private fun colorToHsv(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val maxV = maxOf(r, g, b)
    val minV = minOf(r, g, b)
    val d = maxV - minV
    val h = if (d == 0f) {
        0f
    } else {
        when (maxV) {
            r -> (g - b) / d + if (g < b) 6f else 0f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } / 6f
    }
    val s = if (maxV == 0f) 0f else d / maxV
    return floatArrayOf(h, s, maxV)
}
