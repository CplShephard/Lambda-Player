// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalFoundationApi::class, dev.shephard.player.ui.miuix.ExperimentalMaterial3Api::class)

package dev.shephard.player.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.ThemeModePreference
import dev.shephard.player.theme.PaletteStyle
import dev.shephard.player.theme.ThemeColorSpec
import dev.shephard.player.ui.components.CustomColorPickerDialog
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.captureForTopBarBlur
import dev.shephard.player.ui.components.miuixWidgetClick
import dev.shephard.player.ui.components.overScrollVertical
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.miuixBlurSurface
import dev.shephard.player.ui.i18n.AllLanguages
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.Card
import dev.shephard.player.ui.miuix.CardDefaults
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Slider
import dev.shephard.player.ui.miuix.Switch
import dev.shephard.player.ui.miuix.SwitchDefaults
import dev.shephard.player.ui.miuix.Text
import dev.shephard.player.ui.theme.material.PresetColors
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

private val SeedPalette = PresetColors.map { it.color.toArgb() }

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
            dev.shephard.player.ui.components.InstallerXTopBar(
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
    var showRemoveWallpaperConfirm by remember { mutableStateOf(false) }
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    val seedColor by prefs.seedColor.collectAsState(initial = SeedPalette.first())
    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = PreferencesManager.cachedWallpaperBrightness)
    val cardAlpha by prefs.cardAlpha.collectAsState(initial = 0.85f)
    val language by prefs.language.collectAsState(initial = "en")
    val themeMode by prefs.themeMode.collectAsState(initial = ThemeModePreference.LIGHT)
    val dynamicColor by prefs.dynamicColor.collectAsState(initial = false)
    val useMiuix by prefs.useMiuix.collectAsState(initial = true)
    val useMiuixMonet by prefs.useMiuixMonet.collectAsState(initial = false)
    val paletteStyle by prefs.paletteStyle.collectAsState(initial = PaletteStyle.TonalSpot)
    val colorSpec by prefs.colorSpec.collectAsState(initial = ThemeColorSpec.SPEC_2025)
    val appleFloatingBar by prefs.useAppleFloatingBar.collectAsState(initial = false)
    val liquidGlassEnabled by prefs.liquidGlassEnabled.collectAsState(initial = false)
    val playlistsLayout by prefs.playlistsLayout.collectAsState(initial = LayoutMode.LIST)
    val musicsLayout by prefs.musicsLayout.collectAsState(initial = LayoutMode.LIST)

    var customPickerOpen by remember { mutableStateOf(false) }
    var wallpaperBrightnessValue by remember { mutableFloatStateOf(PreferencesManager.cachedWallpaperBrightness) }
    LaunchedEffect(wallpaperBrightness) {
        wallpaperBrightnessValue = wallpaperBrightness
    }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        wallpaperBrightnessValue = wallpaperBrightness
    }

    var wallpaperCropOutputUri by remember { mutableStateOf<Uri?>(null) }
    val wallpaperCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = wallpaperCropOutputUri
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null) {
            scope.launch { prefs.setWallpaperUri(output.toString()) }
        }
    }

    fun launchWallpaperCrop(sourceUri: Uri) {
        val dir = java.io.File(context.filesDir, "persisted_wallpaper").apply { mkdirs() }
        val file = java.io.File(dir, "wallpaper_${System.currentTimeMillis()}.jpg")
        val outputUri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        wallpaperCropOutputUri = outputUri

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val cropIntent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(sourceUri, "image/*")
            putExtra("crop", "true")
            putExtra("scale", "true")
            putExtra("aspectX", screenWidth)
            putExtra("aspectY", screenHeight)
            putExtra("outputX", screenWidth)
            putExtra("outputY", screenHeight)
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, outputUri)
            putExtra("outputFormat", android.graphics.Bitmap.CompressFormat.JPEG.toString())
            putExtra("return-data", false)
            putExtra("noFaceDetection", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "wallpaper", sourceUri)
        }
        val resolved = context.packageManager.queryIntentActivities(cropIntent, 0)
        for (info in resolved) {
            val pkg = info.activityInfo?.packageName ?: continue
            try {
                context.grantUriPermission(
                    pkg, outputUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
        }
        if (resolved.isNotEmpty()) {
            wallpaperCropLauncher.launch(cropIntent)
        } else {
            scope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistWallpaper(context, sourceUri)
                prefs.setWallpaperUri((persisted ?: sourceUri).toString())
            }
        }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            launchWallpaperCrop(uri)
        }
    }

    SettingsPageScaffold(title = strings.themeSettings, onBack = onBack) {
        SectionCard {
            Text(strings.uiEngine, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            val uiEngineOptions = listOf(strings.miuixUi, strings.googleUi)
            val uiEngineSelectedIndex = if (useMiuix) 0 else 1
            top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                title = strings.uiEngine,
                summary = strings.uiEngineDescription,
                items = uiEngineOptions.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it) },
                selectedIndex = uiEngineSelectedIndex,
                modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                onSelectedIndexChange = { index ->
                    if ((index == 0) != useMiuix) {
                        scope.launch { prefs.setUseMiuix(index == 0) }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(label = strings.appleFloatingBar, checked = appleFloatingBar, description = strings.appleFloatingBarDescription) { enabled ->
                scope.launch { prefs.setUseAppleFloatingBar(enabled) }
            }
        }

        SectionCard {
            Text(strings.themeSettings, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))

            val themeModeOptions = listOf(
                ThemeModePreference.LIGHT to strings.lightMode,
                ThemeModePreference.DARK to strings.darkMode,
                ThemeModePreference.AUTO to strings.autoMode,
            )
            val themeModeSelectedIndex = themeModeOptions.indexOfFirst { it.first == themeMode }.coerceAtLeast(0)
            top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                title = strings.themeMode,
                items = themeModeOptions.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it.second) },
                selectedIndex = themeModeSelectedIndex,
                modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setThemeMode(themeModeOptions[index].first) }
                }
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(label = strings.blurEffect, checked = liquidGlassEnabled, description = strings.blurEffectDescription) { enabled ->
                scope.launch { prefs.setLiquidGlassEnabled(enabled) }
            }

            Spacer(Modifier.height(8.dp))
            ToggleRow(label = strings.miuixCustomColors, checked = useMiuixMonet, description = strings.miuixCustomColorsDescription) { enabled ->
                scope.launch { prefs.setUseMiuixMonet(enabled) }
            }
            AnimatedVisibility(
                visible = useMiuixMonet,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ToggleRow(label = strings.dynamicColor, checked = dynamicColor, description = strings.dynamicColorDescription) { enabled ->
                    scope.launch { prefs.setDynamicColor(enabled) }
                }
            }
            AnimatedVisibility(
                visible = useMiuixMonet,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val paletteStyleOptions = PaletteStyle.entries
                val paletteSelectedIndex = paletteStyleOptions.indexOf(paletteStyle).coerceAtLeast(0)
                top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                    title = strings.paletteStyle,
                    items = paletteStyleOptions.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it.displayName) },
                    selectedIndex = paletteSelectedIndex,
                    modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                    onSelectedIndexChange = { index ->
                        val newStyle = paletteStyleOptions[index]
                        if (newStyle != paletteStyle) {
                            scope.launch { prefs.setPaletteStyle(newStyle) }
                        }
                    }
                )
            }
            AnimatedVisibility(
                visible = useMiuixMonet,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val isSpec2025Supported = paletteStyle.supportsSpec2025
                val availableSpecs = if (isSpec2025Supported) ThemeColorSpec.entries else listOf(ThemeColorSpec.SPEC_2021)
                val activeSpec = if (!isSpec2025Supported) ThemeColorSpec.SPEC_2021 else colorSpec
                val specSelectedIndex = availableSpecs.indexOf(activeSpec).coerceAtLeast(0)
                top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                    title = strings.colorSpec,
                    summary = if (!isSpec2025Supported) strings.colorSpecOnly2021 else null,
                    items = availableSpecs.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it.displayName) },
                    selectedIndex = specSelectedIndex,
                    modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                    onSelectedIndexChange = { index ->
                        val selectedSpec = availableSpecs[index]
                        if (selectedSpec != colorSpec) {
                            scope.launch { prefs.setColorSpec(selectedSpec) }
                        }
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = useMiuixMonet,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SectionCard {
                Text(strings.customColorTitle, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SeedPalette.forEach { argb ->
                        val selected = argb == seedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .bounceClick(enabled = !dynamicColor) { scope.launch { prefs.setSeedColor(argb) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.28f))
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
                                        Color(0xFFEF4444),
                                        Color(0xFFF59E0B),
                                        Color(0xFFFACC15),
                                        Color(0xFF22C55E),
                                        Color(0xFF14B8A6),
                                        Color(0xFF3B82F6),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFEF4444)
                                    )
                                )
                            )
                            .bounceClick(enabled = !dynamicColor) { customPickerOpen = true }
                    )
                }
            }
        }

        SectionCard {
            Text(strings.language, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            val langOptions = AllLanguages
            val langSelectedIndex = langOptions.indexOfFirst { it.code == language }.coerceAtLeast(0)
            top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                title = strings.language,
                items = langOptions.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it.displayName) },
                selectedIndex = langSelectedIndex,
                modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setLanguage(langOptions[index].code) }
                }
            )
        }
    }

    if (customPickerOpen) {
        CustomColorPickerDialog(
            onDismiss = { customPickerOpen = false },
            onColorPicked = { argb ->
                scope.launch { prefs.setSeedColor(argb) }
            },
            initialArgb = seedColor,
            title = strings.customColorTitle,
            hexPlaceholder = strings.hexPlaceholder
        )
    }
}

@Composable
fun PlayerSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    val crossfadeEnabled by prefs.crossfadeEnabled.collectAsState(initial = false)
    val gaplessEnabled by prefs.gaplessEnabled.collectAsState(initial = true)
    val playWithOthers by prefs.playWithOthers.collectAsState(initial = false)

    SettingsPageScaffold(title = strings.playbackSettings, onBack = onBack) {
        SectionCard {
            ToggleRow(
                label = strings.crossfade,
                checked = crossfadeEnabled
            ) { enabled ->
                scope.launch { prefs.setCrossfadeEnabled(enabled) }
            }
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                label = strings.gapless,
                checked = gaplessEnabled
            ) { enabled ->
                scope.launch { prefs.setGaplessEnabled(enabled) }
            }
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                label = strings.playWithOthers,
                checked = playWithOthers
            ) { enabled ->
                scope.launch { prefs.setPlayWithOthers(enabled) }
            }
        }
    }
}

@Composable
fun AboutSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val strings = LocalStrings.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: Exception) { "" }
    }

    SettingsPageScaffold(title = strings.aboutLambdaPlayerTitle, onBack = onBack) {
        SectionCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(strings.appName, style = MiuixAppTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${strings.version} $versionName", style = MiuixAppTheme.typography.bodyMedium, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
            }
        }
        SectionCard {
            SettingsActionRow(
                icon = Icons.Filled.Info,
                title = strings.github
            ) {
                runCatching { uriHandler.openUri("https://github.com/lamda-team/Lambda-Player") }
            }
        }
    }
}

@Composable
fun StatsScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val uiState by playerViewModel.uiState.collectAsState()

    SettingsPageScaffold(title = strings.statsTitle, onBack = onBack) {
        SectionCard {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.totalListeningTime, style = MiuixAppTheme.typography.bodyLarge, color = MiuixAppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(formatListeningTime(uiState.totalListeningMs, strings), style = MiuixAppTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.queue, style = MiuixAppTheme.typography.bodyLarge, color = MiuixAppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(uiState.queue.size.toString(), style = MiuixAppTheme.typography.titleMedium)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.likedSongs, style = MiuixAppTheme.typography.bodyLarge, color = MiuixAppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(uiState.likedSongIds.size.toString(), style = MiuixAppTheme.typography.titleMedium)
            }
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
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val liquidGlassOn = LocalBlurEnabled.current

    val pageBackdrop = if (liquidGlassOn) rememberLayerBackdrop() else null

    val collapseRangePx = with(density) { 44.dp.toPx() }
    val scrollProgress by remember {
        derivedStateOf { (scrollState.value / collapseRangePx).coerceIn(0f, 1f) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MiuixAppTheme.colorScheme.background)) {
        SmallTopAppBar(
            title = title,
            modifier = if (pageBackdrop != null) {
                Modifier.miuixBlurSurface(
                    backdrop = pageBackdrop,
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    blurRadius = 70f,
                    tintAlpha = if (scrollProgress > 0.01f) (0.68f + scrollProgress * 0.27f).coerceIn(0f, 0.95f) else 0f,
                    fallbackColor = Color.Transparent
                )
            } else Modifier,
            color = if (pageBackdrop != null) Color.Transparent else MiuixAppTheme.colorScheme.background.copy(alpha = scrollProgress),
            titleColor = MiuixAppTheme.colorScheme.onBackground.copy(alpha = scrollProgress),
            scrollBehavior = topAppBarScrollBehavior,
            defaultWindowInsetsPadding = false,
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                        .bounceClick { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription, tint = MiuixAppTheme.colorScheme.onBackground)
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(pageBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                .overScrollVertical()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MiuixAppTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MiuixAppTheme.colorScheme.onBackground,
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
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixAppTheme.colorScheme.background.copy(alpha = 0.72f))
            .miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MiuixAppTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, color = MiuixAppTheme.colorScheme.onBackground, style = MiuixAppTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MiuixAppTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun ToggleRow(label: String, checked: Boolean, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = label,
                color = MiuixAppTheme.colorScheme.onBackground,
                style = MiuixAppTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                    style = MiuixAppTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White,
                checkedTrackColor = MiuixAppTheme.colorScheme.primary,
                uncheckedTrackColor = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        )
    }
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
