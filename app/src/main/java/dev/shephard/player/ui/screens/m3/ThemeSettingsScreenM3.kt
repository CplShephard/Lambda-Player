// SPDX-License-Identifier: GPL-3.0-only
// Material 3 Expressive - Theme Settings with Lineage + M3 Switchers
package dev.shephard.player.ui.screens.m3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.shephard.player.ui.components.m3.M3SmallTopBar
import dev.shephard.player.ui.components.m3.m3TopBarColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.twotone.ColorLens
import androidx.compose.material.icons.twotone.InvertColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.toPreferenceInt
import dev.shephard.player.theme.PaletteStyle
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.ThemeColorSpec
import dev.shephard.player.theme.ThemeMode
import dev.shephard.player.ui.components.m3.ColorSwatchPreview
import dev.shephard.player.ui.components.m3.DropDownMenuWidget
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.components.m3.SwitchWidget
import dev.shephard.player.ui.i18n.AllLanguages
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
    val predictiveBack by prefs.predictiveBackAnimation.collectAsState(initial = PredictiveBackAnimation.MIUIX)

    val navBarCornerRadius by prefs.m3NavBarCornerRadius.collectAsState(initial = 24)
    val language by prefs.language.collectAsState(initial = "en")

    var navBarCornerRadiusValue by remember { mutableFloatStateOf(navBarCornerRadius.toFloat()) }
    LaunchedEffect(navBarCornerRadius) { navBarCornerRadiusValue = navBarCornerRadius.toFloat() }

    // Fix #4: if dynamicColor true, accent grid should stay closed directly, no closing animation on enter/exit
    // Use null initial to avoid false->true->false flicker
    val dynamicColorNullable by prefs.dynamicColor.collectAsState(initial = null)
    val isDynamicColorOn = dynamicColorNullable == true
    val accentGridTarget = paletteStyle != PaletteStyle.Monochrome && (!isDynamicColorOn || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)

    val themeModeOptions = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
    val themeModeList = listOf(strings.lightMode, strings.darkMode, strings.autoMode)
    val themeModeIndex = themeModeOptions.indexOf(themeMode).coerceAtLeast(0)

    val paletteStyleOptions = PaletteStyle.entries
    val paletteIndex = paletteStyleOptions.indexOf(paletteStyle).coerceAtLeast(0)

    val isSpec2025Supported = paletteStyle.supportsSpec2025
    val availableSpecs = if (isSpec2025Supported) ThemeColorSpec.entries else listOf(ThemeColorSpec.SPEC_2021)
    val activeSpec = if (!isSpec2025Supported) ThemeColorSpec.SPEC_2021 else colorSpec
    val specIndex = availableSpecs.indexOf(activeSpec).coerceAtLeast(0)

            Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                M3SmallTopBar(
                    title = { Text(strings.themeSettings) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription) } },
                    colors = m3TopBarColors(),
                )
            },
        ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding(), bottom = paddingValues.calculateBottomPadding() + 80.dp)
        ) {
            // UI Style - M3 switcher restored
            item {
                SegmentedColumn(title = strings.themeUiStyleSection) {
                    item {
                        DropDownMenuWidget(
                            icon = Icons.TwoTone.ColorLens,
                            title = strings.uiEngine,
                            description = strings.uiEngineDescription,
                            choice = if (useMiuix) 0 else 1,
                            data = listOf(strings.miuixUi, strings.googleUi),
                            onChoiceChange = { idx -> scope.launch { prefs.setUseMiuix(idx == 0) } }
                        )
                    }
                }
            }

            // M3 UI Section - bring back switchers
            item {
                SegmentedColumn(title = strings.themeM3UiSection) {
                    item {
                        DropDownMenuWidget(
                            icon = Icons.Filled.DarkMode,
                            title = strings.themeMode,
                            choice = themeModeIndex,
                            data = themeModeList,
                            onChoiceChange = { idx -> scope.launch { prefs.setThemeMode(themeModeOptions[idx].toPreferenceInt()) } }
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
                    item {
                        DropDownMenuWidget(
                            icon = Icons.Filled.Palette,
                            title = strings.paletteStyle,
                            choice = paletteIndex,
                            data = paletteStyleOptions.map { it.displayName },
                            onChoiceChange = { idx -> scope.launch { prefs.setPaletteStyle(paletteStyleOptions[idx]) } }
                        )
                    }
                    item {
                        DropDownMenuWidget(
                            icon = Icons.Filled.Tune,
                            title = strings.colorSpec,
                            description = if (!isSpec2025Supported) strings.colorSpecOnly2021 else null,
                            choice = specIndex,
                            data = availableSpecs.map { it.displayName },
                            onChoiceChange = { idx -> scope.launch { prefs.setColorSpec(availableSpecs[idx]) } }
                        )
                    }
                }
            }

            // Accent color grid - fixed #4: no closing animation when dynamic color on
            if (dynamicColorNullable != null && accentGridTarget) {
                item {
                    SegmentedColumn(title = strings.accentColor) {
                        item {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                val itemMinWidth = 88.dp
                                val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)
                                val chunkedColors = PresetColors.chunked(columns)
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    chunkedColors.forEach { rowItems ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                            rowItems.forEach { rawColor ->
                                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                                    ColorSwatchPreview(
                                                        rawColor = rawColor,
                                                        currentStyle = paletteStyle,
                                                        colorSpec = colorSpec,
                                                        textStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                                        textColor = MaterialTheme.colorScheme.onSurface,
                                                        isSelected = seedColor == rawColor.color.toArgb() && !isDynamicColorOn,
                                                    ) { scope.launch { prefs.setSeedColor(rawColor.color.toArgb()) } }
                                                }
                                            }
                                            val remaining = columns - rowItems.size
                                            if (remaining > 0) repeat(remaining) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SegmentedColumn(title = strings.navBarCornerRadius) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(strings.navBarCornerRadius, style = MaterialTheme.typography.bodyMedium)
                                Text("${navBarCornerRadiusValue.toInt()} dp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = navBarCornerRadiusValue,
                                onValueChange = { navBarCornerRadiusValue = it },
                                onValueChangeFinished = { scope.launch { prefs.setM3NavBarCornerRadius(navBarCornerRadiusValue.toInt()) } },
                                valueRange = 0f..64f,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(topStart = navBarCornerRadiusValue.dp, topEnd = navBarCornerRadiusValue.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(strings.googleUi, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Predictive Back (InstallerX Revived)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                item {
                    SegmentedColumn(title = strings.predictiveBackTitle) {
                        item {
                            DropDownMenuWidget(
                                icon = Icons.TwoTone.ColorLens,
                                title = strings.predictiveBackTitle,
                                description = strings.predictiveBackDescription,
                                choice = PredictiveBackAnimation.entries.indexOf(predictiveBack).coerceAtLeast(0),
                                data = PredictiveBackAnimation.entries.map { predictiveBackDisplayName(it, strings) },
                                onChoiceChange = { idx -> scope.launch { prefs.setPredictiveBackAnimation(PredictiveBackAnimation.entries[idx]) } }
                            )
                        }
                    }
                }
            }

            // Language - switcher restored
            item {
                SegmentedColumn(title = strings.language) {
                    item {
                        val langIndex = AllLanguages.indexOfFirst { it.code == language }.coerceAtLeast(0)
                        DropDownMenuWidget(
                            icon = Icons.Filled.Translate,
                            title = strings.language,
                            choice = langIndex,
                            data = AllLanguages.map { it.displayName },
                            onChoiceChange = { idx -> scope.launch { prefs.setLanguage(AllLanguages[idx].code) } }
                        )
                    }
                }
            }
        }
        }
}

private fun predictiveBackDisplayName(animation: PredictiveBackAnimation, strings: dev.shephard.player.ui.i18n.Strings): String = when (animation) {
    PredictiveBackAnimation.NONE -> strings.predictiveBackNone
    PredictiveBackAnimation.AOSP -> strings.predictiveBackAosp
    PredictiveBackAnimation.MIUIX -> strings.predictiveBackMiuix
    PredictiveBackAnimation.CLASSIC -> strings.predictiveBackClassic
    PredictiveBackAnimation.SCALE -> strings.predictiveBackScale
}
