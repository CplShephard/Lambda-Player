// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalFoundationApi::class, dev.shephard.player.ui.miuix.ExperimentalMaterial3Api::class)

package dev.shephard.player.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.R
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.LyricsAutoSearchMode
import dev.shephard.player.player.VolumeNormalization
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.ThemeModePreference
import dev.shephard.player.theme.PaletteStyle
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.ThemeColorSpec
import dev.shephard.player.ui.components.captureForTopBarBlur
import dev.shephard.player.ui.components.m3.ColorSwatchPreview
import dev.shephard.player.ui.components.comradeEasterEggTrigger
import dev.shephard.player.ui.components.miuixWidgetClick
import dev.shephard.player.ui.components.overScrollVertical
import dev.shephard.player.ui.glass.bgeffect.BgEffectBackground
import dev.shephard.player.ui.i18n.AllLanguages
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.Card
import dev.shephard.player.ui.miuix.CardDefaults
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import dev.shephard.player.ui.theme.material.PresetColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import top.yukonga.miuix.kmp.basic.Card as MiuixNativeCard
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
fun SettingsScreen(
    playerViewModel: PlayerViewModel = viewModel(),
    onOpenThemeSettings: () -> Unit = {},
    onOpenPlayerSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenStats: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val strings = LocalStrings.current

    val topBarState = dev.shephard.player.ui.components.rememberCollapsingTopBarState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            dev.shephard.player.ui.components.MiuixTopBar(
                title = strings.settings,
                state = topBarState
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .captureForTopBarBlur(topBarState)
                    .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = innerPadding.calculateTopPadding() + 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TotalListeningTimeCard(playerViewModel = playerViewModel, onClick = onOpenStats)

                SettingsNavigationCard(
                    icon = Icons.Filled.ColorLens,
                    title = strings.themeSettings,
                    summary = strings.themeSettingsSummary,
                    onClick = onOpenThemeSettings
                )
                SettingsNavigationCard(
                    icon = Icons.Filled.MusicNote,
                    title = strings.playbackSettings,
                    summary = strings.playbackSettingsSummary,
                    onClick = onOpenPlayerSettings
                )
                SettingsNavigationCard(
                    icon = Icons.Filled.Info,
                    title = strings.aboutLambdaPlayerTitle,
                    summary = strings.aboutLambdaPlayerSummary,
                    onClick = onOpenAbout
                )

                Spacer(Modifier.height(110.dp))
            }
        }
    }
}

@Composable
fun ThemeSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    val useMiuix by prefs.useMiuix.collectAsState(initial = true)
    val themeMode by prefs.themeMode.collectAsState(initial = ThemeModePreference.LIGHT)
    val liquidGlassEnabled by prefs.liquidGlassEnabled.collectAsState(initial = false)
    val appleFloatingBar by prefs.useAppleFloatingBar.collectAsState(initial = false)

    // Fix custom colors re-animation: use nullable initial to avoid false->true flicker on enter
    val useMiuixMonetNullable by prefs.useMiuixMonet.collectAsState(initial = null as Boolean?)
    val useMiuixMonet = useMiuixMonetNullable ?: false
    val miuixGreenAccent by prefs.miuixGreenAccent.collectAsState(initial = false)
    val dynamicColorNullable by prefs.dynamicColor.collectAsState(initial = null as Boolean?)
    val dynamicColor = dynamicColorNullable ?: false
    val paletteStyle by prefs.paletteStyle.collectAsState(initial = PaletteStyle.TonalSpot)
    val colorSpec by prefs.colorSpec.collectAsState(initial = ThemeColorSpec.SPEC_2025)
    val seedColor by prefs.seedColor.collectAsState(initial = PresetColors.first().color.toArgb())

    val language by prefs.language.collectAsState(initial = "en")
    val musicsLayout by prefs.musicsLayout.collectAsState(initial = LayoutMode.LIST)
    val playlistsLayout by prefs.playlistsLayout.collectAsState(initial = LayoutMode.LIST)
    val predictiveBack by prefs.predictiveBackAnimation.collectAsState(initial = PredictiveBackAnimation.MIUIX)

    SettingsPageScaffold(title = strings.themeSettings, onBack = onBack) {
        // ── Group 1: UI Style (identical to InstallerX Revived) ──────────────
        SmallTitle(
            text = strings.themeUiStyleSection,
            insideMargin = PaddingValues(16.dp, 8.dp),
        )
        SectionCard(contentPadding = PaddingValues(0.dp)) {
            val engineOptions = listOf(true to strings.miuixUi, false to strings.googleUi)
            val engineSelectedIndex = if (useMiuix) 0 else 1
            MiuixSpinnerRow(
                title = strings.uiEngine,
                summary = strings.uiEngineDescription,
                items = engineOptions.map { it.second },
                selectedIndex = engineSelectedIndex,
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setUseMiuix(engineOptions[index].first) }
                }
            )

            Spacer(Modifier.height(8.dp))
            MiuixSwitchRow(label = strings.appleFloatingBar, checked = appleFloatingBar, description = strings.appleFloatingBarDescription) { enabled ->
                scope.launch { prefs.setUseAppleFloatingBar(enabled) }
            }
        }

        // ── Group 2: Miuix UI + Miuix Custom Colors (InstallerX layout) ──────
        SmallTitle(
            text = strings.themeMiuixUiSection,
            insideMargin = PaddingValues(16.dp, 8.dp),
        )
        SectionCard(contentPadding = PaddingValues(0.dp)) {
            val themeModeOptions = listOf(
                ThemeModePreference.LIGHT to strings.lightMode,
                ThemeModePreference.DARK to strings.darkMode,
                ThemeModePreference.AUTO to strings.autoMode,
            )
            val themeModeSelectedIndex = themeModeOptions.indexOfFirst { it.first == themeMode }.coerceAtLeast(0)
            MiuixSpinnerRow(
                title = strings.themeMode,
                items = themeModeOptions.map { it.second },
                selectedIndex = themeModeSelectedIndex,
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setThemeMode(themeModeOptions[index].first) }
                }
            )

            // Hide blur toggle on < Android 12 (S) per spec
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(Modifier.height(8.dp))
                MiuixSwitchRow(label = strings.blurEffect, checked = liquidGlassEnabled, description = strings.blurEffectDescription) { enabled ->
                    scope.launch { prefs.setLiquidGlassEnabled(enabled) }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Green instead of the stock blue accent; only applies to the stock palette, so it is
            // greyed out while custom colors generate the palette from the seed color.
            MiuixSwitchRow(
                label = strings.miuixGreenAccent,
                checked = miuixGreenAccent,
                description = strings.miuixGreenAccentDescription,
                enabled = !useMiuixMonet,
            ) { enabled ->
                scope.launch { prefs.setMiuixGreenAccent(enabled) }
            }

            Spacer(Modifier.height(8.dp))
            MiuixSwitchRow(label = strings.miuixCustomColors, checked = useMiuixMonet, description = strings.miuixCustomColorsDescription) { enabled ->
                scope.launch { prefs.setUseMiuixMonet(enabled) }
            }

            // Fixed re-animation: snap to target on first load to avoid enter animation on re-enter
            if (useMiuixMonetNullable != null) {
                val monetMenuState = remember(useMiuixMonetNullable) { MutableTransitionState(useMiuixMonet) }
                LaunchedEffect(useMiuixMonet) {
                    monetMenuState.targetState = useMiuixMonet
                }
                // Snap immediately on first load – use targetState only, initial state already equals useMiuixMonet
                LaunchedEffect(useMiuixMonetNullable) {
                    monetMenuState.targetState = useMiuixMonet
                }
                AnimatedVisibility(
                    visibleState = monetMenuState,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                Column {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(Modifier.height(8.dp))
                        MiuixSwitchRow(label = strings.dynamicColor, checked = dynamicColor, description = strings.dynamicColorDescription) { enabled ->
                            scope.launch { prefs.setDynamicColor(enabled) }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    val paletteStyleOptions = PaletteStyle.entries
                    val paletteSelectedIndex = paletteStyleOptions.indexOf(paletteStyle).coerceAtLeast(0)
                    MiuixSpinnerRow(
                        title = strings.paletteStyle,
                        items = paletteStyleOptions.map { it.displayName },
                        selectedIndex = paletteSelectedIndex,
                        onSelectedIndexChange = { index ->
                            val newStyle = paletteStyleOptions[index]
                            if (newStyle != paletteStyle) {
                                scope.launch { prefs.setPaletteStyle(newStyle) }
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    val isSpec2025Supported = paletteStyle.supportsSpec2025
                    val availableSpecs = if (isSpec2025Supported) ThemeColorSpec.entries else listOf(ThemeColorSpec.SPEC_2021)
                    val activeSpec = if (!isSpec2025Supported) ThemeColorSpec.SPEC_2021 else colorSpec
                    val specSelectedIndex = availableSpecs.indexOf(activeSpec).coerceAtLeast(0)
                    MiuixSpinnerRow(
                        title = strings.colorSpec,
                        summary = if (!isSpec2025Supported) strings.colorSpecOnly2021 else null,
                        items = availableSpecs.map { it.displayName },
                        selectedIndex = specSelectedIndex,
                        onSelectedIndexChange = { index ->
                            val selectedSpec = availableSpecs[index]
                            if (selectedSpec != colorSpec) {
                                scope.launch { prefs.setColorSpec(selectedSpec) }
                            }
                        }
                    )
                }
            }
            }
        }

        // ── Grid/list layout preferences (kept in Theme Settings for Miuix) ──
        SmallTitle(
            text = strings.layout,
            insideMargin = PaddingValues(16.dp, 8.dp),
        )
        SectionCard(contentPadding = PaddingValues(0.dp)) {
            val layoutModes = listOf(LayoutMode.LIST, LayoutMode.GRID)
            val layoutLabels = listOf(strings.list, strings.grid)
            MiuixSpinnerRow(
                title = strings.musicsLayout,
                items = layoutLabels,
                selectedIndex = layoutModes.indexOf(musicsLayout).coerceAtLeast(0),
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setMusicsLayout(layoutModes[index]) }
                }
            )
            Spacer(Modifier.height(8.dp))
            MiuixSpinnerRow(
                title = strings.playlistsLayout,
                items = layoutLabels,
                selectedIndex = layoutModes.indexOf(playlistsLayout).coerceAtLeast(0),
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setPlaylistsLayout(layoutModes[index]) }
                }
            )
        }

        // Fixed: avoid re-animation on enter/exit, only show when data loaded, snap on first load
        val accentGridTarget = useMiuixMonet &&
            paletteStyle != PaletteStyle.Monochrome &&
            (!dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        val accentGridVisible = useMiuixMonetNullable != null && dynamicColorNullable != null && accentGridTarget
        val accentGridState = remember(useMiuixMonetNullable, dynamicColorNullable) { MutableTransitionState(accentGridVisible) }
        LaunchedEffect(accentGridVisible) {
            accentGridState.targetState = accentGridVisible
        }
        AnimatedVisibility(
            visibleState = accentGridState,
            enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
        ) {
            Column {
                SmallTitle(
                    text = strings.accentColor,
                    insideMargin = PaddingValues(16.dp, 8.dp),
                )
                // Miuix card exactly like InstallerX: no inner card padding,
                // 12dp/16dp inner spacing, 88dp minimum swatch width.
                MiuixNativeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
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
                                                textStyle = MiuixAppTheme.typography.labelSmall,
                                                textColor = MiuixAppTheme.colorScheme.onSurface,
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

        // ── Predictive Back (exactly like InstallerX Revived Miuix page) ─────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            SmallTitle(
                text = strings.predictiveBackTitle,
                insideMargin = PaddingValues(16.dp, 8.dp),
            )
            SectionCard(contentPadding = PaddingValues(0.dp)) {
                val predictiveBackOptions = listOf(
                    PredictiveBackAnimation.NONE to strings.predictiveBackNone,
                    PredictiveBackAnimation.AOSP to strings.predictiveBackAosp,
                    PredictiveBackAnimation.MIUIX to strings.predictiveBackMiuix,
                    PredictiveBackAnimation.CLASSIC to strings.predictiveBackClassic,
                    PredictiveBackAnimation.SCALE to strings.predictiveBackScale,
                )
                val predictiveBackIndex = predictiveBackOptions.indexOfFirst { it.first == predictiveBack }.coerceAtLeast(0)
                MiuixSpinnerRow(
                    title = strings.predictiveBackTitle,
                    summary = strings.predictiveBackDescription,
                    items = predictiveBackOptions.map { it.second },
                    selectedIndex = predictiveBackIndex,
                    onSelectedIndexChange = { index ->
                        val newAnimation = predictiveBackOptions[index].first
                        if (newAnimation != predictiveBack) {
                            scope.launch { prefs.setPredictiveBackAnimation(newAnimation) }
                        }
                    }
                )
            }
        }

        // Language uses the same library spinner (and library padding) as every other switcher.
        SmallTitle(
            text = strings.language,
            insideMargin = PaddingValues(16.dp, 8.dp),
        )
        SectionCard(contentPadding = PaddingValues(0.dp)) {
            val languageIndex = AllLanguages.indexOfFirst { it.code == language }.coerceAtLeast(0)
            MiuixSpinnerRow(
                title = strings.language,
                items = AllLanguages.map { it.displayName },
                selectedIndex = languageIndex,
                onSelectedIndexChange = { index ->
                    val code = AllLanguages[index].code
                    if (code != language) {
                        scope.launch { prefs.setLanguage(code) }
                    }
                }
            )
        }

        Spacer(Modifier.height(110.dp))
    }
}

@Composable
fun PlayerSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val crossfade by prefs.crossfadeEnabled.collectAsState(initial = false)
    val gapless by prefs.gaplessEnabled.collectAsState(initial = true)
    val playWith by prefs.playWithOthers.collectAsState(initial = false)
    val lyricsAutoSearch by prefs.lyricsAutoSearchMode.collectAsState(initial = LyricsAutoSearchMode.OFF)
    val volumeNormalization by prefs.volumeNormalization.collectAsState(initial = VolumeNormalization.NONE)

    SettingsPageScaffold(title = strings.playbackSettings, onBack = onBack) {
        SmallTitle(
            text = strings.playbackSettings,
            insideMargin = PaddingValues(16.dp, 8.dp),
        )
        SectionCard(contentPadding = PaddingValues(0.dp)) {
            MiuixSwitchRow(label = strings.crossfade, checked = crossfade) { scope.launch { prefs.setCrossfadeEnabled(it) } }
            Spacer(Modifier.height(8.dp))
            MiuixSwitchRow(label = strings.gapless, checked = gapless) { scope.launch { prefs.setGaplessEnabled(it) } }
            Spacer(Modifier.height(8.dp))
            MiuixSwitchRow(label = strings.playWithOthers, checked = playWith) { scope.launch { prefs.setPlayWithOthers(it) } }
            Spacer(Modifier.height(8.dp))
            val lyricsModes = LyricsAutoSearchMode.entries
            MiuixSpinnerRow(
                title = strings.lyricsAutoSearchTitle,
                summary = strings.lyricsAutoSearchDescription,
                items = lyricsModes.map { lyricsAutoSearchLabel(it, strings) },
                selectedIndex = lyricsModes.indexOf(lyricsAutoSearch),
                onSelectedIndexChange = { idx -> scope.launch { prefs.setLyricsAutoSearchMode(lyricsModes[idx]) } }
            )
            Spacer(Modifier.height(8.dp))
            val normalizationModes = VolumeNormalization.entries
            MiuixSpinnerRow(
                title = strings.volumeNormalizationTitle,
                summary = strings.volumeNormalizationDescription,
                items = normalizationModes.map { volumeNormalizationLabel(it, strings) },
                selectedIndex = normalizationModes.indexOf(volumeNormalization),
                onSelectedIndexChange = { idx -> scope.launch { prefs.setVolumeNormalization(normalizationModes[idx]) } }
            )
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
fun AboutSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
        catch (_: PackageManager.NameNotFoundException) { "" }
    }

    var showLicenses by remember { mutableStateOf(false) }
    if (showLicenses) MiuixLicensesDialog(onDismiss = { showLicenses = false })
    var showComradeEasterEgg by remember { mutableStateOf(false) }
    if (showComradeEasterEgg) {
        dev.shephard.player.ui.components.ComradeEasterEggDialog(useMiuix = true, onDismiss = { showComradeEasterEgg = false })
    }
    val density = LocalDensity.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val appBarHeight = 56.dp
    val headerTopPadding = statusBarPadding + appBarHeight + 40.dp

    val lazyListState = rememberLazyListState()
    val topBarState = dev.shephard.player.ui.components.rememberCollapsingTopBarState()
    var logoHeightPx by remember { mutableIntStateOf(0) }
    var headerHeightDp by remember { mutableStateOf(190.dp) }
    var appIconBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val sizePx = with(density) { 88.dp.roundToPx() }
                val shrink = context.applicationInfo.loadIcon(context.packageManager) is android.graphics.drawable.AdaptiveIconDrawable
                val loader = AppIconLoader(sizePx, shrink, context)
                appIconBitmap = loader.loadIcon(context.applicationInfo, false)
            } catch (_: Exception) {
                appIconBitmap = null
            }
        }
    }

    val scrollProgress by remember {
        derivedStateOf {
            if (logoHeightPx <= 0) {
                0f
            } else {
                val index = lazyListState.firstVisibleItemIndex
                val offset = lazyListState.firstVisibleItemScrollOffset
                if (index > 0) 1f else (offset.toFloat() / logoHeightPx).coerceIn(0f, 1f)
            }
        }
    }

    val isDarkTheme = MiuixAppTheme.colorScheme.background.luminance() < 0.5f
    val aboutContent: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = headerTopPadding)
                .onSizeChanged { size ->
                    with(density) { headerHeightDp = size.height.toDp() }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)
                        alpha = 1 - iconProgress
                        scaleX = 1 - (iconProgress * 0.05f)
                        scaleY = 1 - (iconProgress * 0.05f)
                    }
                    .clip(RoundedCornerShape(30.dp))
            ) {
                val bitmap = appIconBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = strings.appName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_preview),
                        contentDescription = strings.appName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Text(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 5.dp)
                    .graphicsLayer {
                        val projectNameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
                        alpha = 1 - projectNameProgress
                        scaleX = 1 - (projectNameProgress * 0.05f)
                        scaleY = 1 - (projectNameProgress * 0.05f)
                    },
                text = strings.appName,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp,
                color = MiuixAppTheme.colorScheme.onBackground
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val versionProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                        alpha = 1 - versionProgress
                        scaleX = 1 - (versionProgress * 0.05f)
                        scaleY = 1 - (versionProgress * 0.05f)
                    },
                text = "${strings.version} $versionName",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MiuixAppTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .captureForTopBarBlur(topBarState)
                .overScrollVertical()
                .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
        ) {
            item(key = "logoSpacer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(headerTopPadding + headerHeightDp + 48.dp)
                        .onSizeChanged { size -> logoHeightPx = size.height }
                ) {
                    // This list is drawn above the header, so the app icon itself never gets
                    // taps. This transparent 88dp target sits exactly over the icon instead.
                    // Hidden pop-up: tap the icon quickly several times (Chinese UI only).
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = headerTopPadding)
                            .size(88.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .comradeEasterEggTrigger { showComradeEasterEgg = true }
                    )
                }
            }
            item(key = "about_content") {
                Column(modifier = Modifier.fillParentMaxHeight()) {
                    // Title and the three link cards sit 120dp lower than before.
                    Spacer(Modifier.height(120.dp))
                    SmallTitle(strings.aboutSectionTitle)
                    MiuixNativeCard(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        ArrowPreference(
                            title = strings.github,
                            summary = "CplShephard",
                            onClick = { uriHandler.openUri("https://github.com/CplShephard") }
                        )
                        ArrowPreference(
                            title = strings.sourceCode,
                            summary = "github.com/CplShephard/Lambda-Player",
                            onClick = { uriHandler.openUri("https://github.com/CplShephard/Lambda-Player") }
                        )
                        ArrowPreference(
                            title = strings.licensesTitle,
                            summary = strings.licensesSummary,
                            onClick = { showLicenses = true }
                        )
                    }
                    Spacer(Modifier.height(110.dp))
                }
            }
        }

        dev.shephard.player.ui.components.SubmenuTopBar(
            title = strings.aboutSectionTitle,
            onBack = onBack,
            state = topBarState,
            collapseFraction = scrollProgress,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BgEffectBackground(
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize(),
                isFullSize = true,
                surface = Color.Black,
                alpha = { 1f - scrollProgress * 0.85f }
            ) {
                aboutContent()
            }
        } else {
            // Solid fallback on <13: gradient fails, use black/white per dark mode
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) Color.Black else Color.White)
            ) {
                aboutContent()
            }
        }
}

@Composable
private fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val strings = LocalStrings.current
    val cs = MiuixAppTheme.colorScheme
    // Use the page content as a backdrop when Miuix blur is enabled; otherwise
    // SubmenuTopBar keeps its normal solid, scroll-collapsing background.
    val topBarState = dev.shephard.player.ui.components.rememberCollapsingTopBarState()
    val scrollState = rememberScrollState()
    val scrollProgress by dev.shephard.player.ui.components.rememberScrollFraction(scrollState)

            Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = cs.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                dev.shephard.player.ui.components.SubmenuTopBar(
                    title = title,
                    state = topBarState,
                    collapseFraction = scrollProgress,
                    onBack = onBack
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .captureForTopBarBlur(topBarState)
                    .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                    .overScrollVertical()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp)
                    .padding(top = innerPadding.calculateTopPadding() + 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MiuixAppTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.onBackground,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .graphicsLayer {
                            alpha = 1f - scrollProgress
                            scaleX = 1f - scrollProgress * 0.05f
                            scaleY = 1f - scrollProgress * 0.05f
                        }
                )
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
}

@Composable
private fun SettingsNavigationCard(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    SectionCard(
        modifier = Modifier.miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixAppTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MiuixAppTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MiuixAppTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MiuixAppTheme.colorScheme.onBackground)
                Text(summary, style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun TotalListeningTimeCard(playerViewModel: PlayerViewModel, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val totalMs by playerViewModel.totalListeningMsLive.collectAsState()
    SectionCard(modifier = Modifier.miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }) {
        Text(
            text = strings.totalListeningTime,
            style = MiuixAppTheme.typography.titleMedium,
            color = MiuixAppTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatListeningTime(totalMs, strings),
            style = MiuixAppTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MiuixAppTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MiuixAppTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(contentPadding)) { content() }
    }
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

/**
 * Miuix switch row as in InstallerX (SwitchPreference: title, optional summary, library padding).
 * Used inside cards that have zero content padding, so the row sits at the same inset as InstallerX.
 */
@Composable
private fun MiuixSwitchRow(label: String, checked: Boolean, description: String? = null, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    top.yukonga.miuix.kmp.preference.SwitchPreference(
        checked = checked,
        onCheckedChange = onChange,
        title = label,
        summary = description,
        enabled = enabled,
    )
}

private fun formatListeningTime(
    ms: Long,
    strings: dev.shephard.player.ui.i18n.Strings
): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d%s %02d%s %02d%s".format(
        h, strings.hourShort,
        m, strings.minuteShort,
        s, strings.secondShort
    ) else "%d%s %02d%s".format(
        m, strings.minuteShort,
        s, strings.secondShort
    )
}

@Composable
private fun MiuixLicensesDialog(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    dev.shephard.player.ui.miuix.MiuixDialog(
        onDismissRequest = onDismiss,
        title = strings.licensesTitle,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AppLicenses.forEach { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri(entry.url) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = entry.name,
                            color = MiuixAppTheme.colorScheme.onBackground,
                            style = MiuixAppTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${entry.license} \u00B7 ${entry.author}",
                            color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                            style = MiuixAppTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        buttons = {
            top.yukonga.miuix.kmp.basic.TextButton(
                text = strings.close,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
