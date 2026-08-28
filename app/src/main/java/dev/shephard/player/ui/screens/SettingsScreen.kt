@file:OptIn(ExperimentalFoundationApi::class, dev.shephard.player.ui.miuix.ExperimentalMaterial3Api::class)

package dev.shephard.player.ui.screens

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Edit

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.foundation.ExperimentalFoundationApi

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.captureForTopBarBlur
import dev.shephard.player.ui.components.miuixWidgetClick
import dev.shephard.player.ui.components.overScrollVertical
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import dev.shephard.player.ui.miuix.Card
import dev.shephard.player.ui.miuix.CardDefaults
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.rememberDrawerDismiss
import dev.shephard.player.ui.miuix.Slider
import dev.shephard.player.ui.miuix.SliderDefaults
import dev.shephard.player.ui.miuix.Switch
import dev.shephard.player.ui.miuix.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.shephard.player.ui.miuix.SwitchDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import dev.shephard.player.R
import top.yukonga.miuix.kmp.basic.Card as MiuixNativeCard
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.ThemeModePreference
import dev.shephard.player.theme.PaletteStyle
import dev.shephard.player.theme.ThemeColorSpec
import dev.shephard.player.ui.components.CustomColorPickerDialog
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.miuixBlurSurface
import dev.shephard.player.ui.theme.material.PresetColors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import dev.shephard.player.ui.glass.bgeffect.BgEffectBackground
import dev.shephard.player.ui.i18n.AllLanguages
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

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

val accent by prefs.accentColor.collectAsState(initial = SeedPalette.first())
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

var langMenuOpen by remember { mutableStateOf(false) }
    var customPickerOpen by remember { mutableStateOf(false) }
    var wallpaperBrightnessValue by remember { mutableFloatStateOf(PreferencesManager.cachedWallpaperBrightness) }
    LaunchedEffect(wallpaperBrightness) {
        wallpaperBrightnessValue = wallpaperBrightness
    }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        wallpaperBrightnessValue = wallpaperBrightness
    }

var wallpaperCropOutputUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val wallpaperCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = wallpaperCropOutputUri
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null) {
            scope.launch { prefs.setWallpaperUri(output.toString()) }
        }
    }

fun launchWallpaperCrop(sourceUri: android.net.Uri) {
        val dir = java.io.File(context.filesDir, "persisted_wallpaper").apply { mkdirs() }
        val file = java.io.File(dir, "wallpaper_${System.currentTimeMillis()}.jpg")
        val outputUri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        wallpaperCropOutputUri = outputUri

val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

val cropIntent = android.content.Intent("com.android.camera.action.CROP").apply {
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
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "wallpaper", sourceUri)
        }
        val resolved = context.packageManager.queryIntentActivities(cropIntent, 0)
        for (info in resolved) {
            val pkg = info.activityInfo?.packageName ?: continue
            try {
                context.grantUriPermission(
                    pkg, outputUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
    ) { uri: android.net.Uri? ->
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
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                ToggleRow(label = strings.dynamicColor, checked = dynamicColor, description = strings.dynamicColorDescription) { enabled ->
                    scope.launch { prefs.setDynamicColor(enabled) }
                }
            }
            AnimatedVisibility(
                visible = useMiuixMonet,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
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
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
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

SectionCard {
            Text(strings.accentColor, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SeedPalette.forEach { argb ->
                    val selected = argb == accent
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .bounceClick(enabled = !dynamicColor) { scope.launch { prefs.setAccentColor(argb) } },
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
                            brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                listOf(
                                    Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFFACC15),
                                    Color(0xFF22C55E), Color(0xFF14B8A6), Color(0xFF3B82F6),
                                    Color(0xFF8B5CF6), Color(0xFFEF4444)
                                )
                            )
                        )
                        .bounceClick(enabled = !dynamicColor) { customPickerOpen = true }
                )
            }
        }

SectionCard {
            Text(strings.wallpaper, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

Box(
                    modifier = Modifier
                        .width(115.dp)
                        .aspectRatio(9f / 19.5f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiuixAppTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    if (wallpaper.isNotEmpty()) {
                        var previewLoaded by remember(wallpaper) { mutableStateOf(false) }
                        AsyncImage(
                            model = wallpaper,
                            contentDescription = strings.wallpaperPreviewContentDescription,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                            onState = { previewLoaded = it is AsyncImagePainter.State.Success }
                        )
                        if (!previewLoaded) {
                            Icon(Icons.Filled.BrokenImage, null, tint = MiuixAppTheme.colorScheme.onSurfaceVariant)
                        }

Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 1f - wallpaperBrightnessValue))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { wallpaperPicker.launch(arrayOf("image/*")) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = MiuixAppTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (wallpaper.isEmpty()) strings.chooseFromGallery else strings.changeWallpaper,
                            style = MiuixAppTheme.typography.bodyMedium,
                            color = MiuixAppTheme.colorScheme.onBackground
                        )
                    }

if (wallpaper.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    runCatching {
                                        launchWallpaperCrop(android.net.Uri.parse(wallpaper))
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MiuixAppTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.edit,
                                style = MiuixAppTheme.typography.bodyMedium,
                                color = MiuixAppTheme.colorScheme.onBackground
                            )
                        }

Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { showRemoveWallpaperConfirm = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.removeWallpaper,
                                style = MiuixAppTheme.typography.bodyMedium,
                                color = Color(0xFFE53935)
                            )
                        }
                    }
                }
            }

if (wallpaper.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(strings.wallpaperBrightness, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = wallpaperBrightnessValue,
                    onValueChange = { wallpaperBrightnessValue = it },
                    onValueChangeFinished = { scope.launch { prefs.setWallpaperBrightness(wallpaperBrightnessValue) } },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MiuixAppTheme.colorScheme.primary,
                        activeTrackColor = MiuixAppTheme.colorScheme.primary,
                        inactiveTrackColor = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }
        }

SectionCard {
            Text(strings.layout, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(10.dp))

val layoutModeOptions = listOf(
                LayoutMode.LIST to strings.list,
                LayoutMode.GRID to strings.grid,
            )
            val musicsLayoutIndex = layoutModeOptions.indexOfFirst { it.first == musicsLayout }.coerceAtLeast(0)
            top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                title = strings.musicsLayout,
                items = layoutModeOptions.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it.second) },
                selectedIndex = musicsLayoutIndex,
                modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setMusicsLayout(layoutModeOptions[index].first) }
                }
            )
            Spacer(Modifier.height(4.dp))
            val playlistsLayoutIndex = layoutModeOptions.indexOfFirst { it.first == playlistsLayout }.coerceAtLeast(0)
            top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                title = strings.playlistsLayout,
                items = layoutModeOptions.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it.second) },
                selectedIndex = playlistsLayoutIndex,
                modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setPlaylistsLayout(layoutModeOptions[index].first) }
                }
            )
        }

SectionCard {
            Text(strings.language, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            SettingsActionRow(
                icon = Icons.Filled.ArrowDropDown,
                title = AllLanguages.firstOrNull { it.code == language }?.displayName ?: language,
                onClick = { langMenuOpen = true }
            )
        }

Spacer(Modifier.height(110.dp))
    }

if (langMenuOpen) {
        MiuixDrawer(
            onDismissRequest = { langMenuOpen = false },
        ) {

val dismissDrawer = rememberDrawerDismiss()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .padding(20.dp)
            ) {
                Text(strings.language, style = MiuixAppTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).overScrollVertical(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AllLanguages) { lang ->
                        val selected = lang.code == language
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else MiuixAppTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.58f)
                                )
                                .miuixWidgetClick(pressScale = 0.97f, maxTiltDegrees = 3f) {
                                    scope.launch { prefs.setLanguage(lang.code) }
                                    dismissDrawer()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lang.displayName,
                                color = if (selected) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onBackground,
                                style = MiuixAppTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selected) {
                                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MiuixAppTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

if (customPickerOpen && !dynamicColor) {
        CustomColorPickerDialog(
            onDismiss = { customPickerOpen = false },
            onColorPicked = { argb ->

scope.launch { prefs.setAccentColor(argb) }
            },
            initialArgb = accent,
            title = strings.customColorTitle,
            hexPlaceholder = strings.hexPlaceholder
        )
    }

if (showRemoveWallpaperConfirm) {
        dev.shephard.player.ui.miuix.MiuixDialog(
            onDismissRequest = { showRemoveWallpaperConfirm = false },
            title = strings.removeWallpaper,
            text = {
                Text(
                    text = strings.removeWallpaperConfirm,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
                )
            },
            buttons = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dev.shephard.player.ui.miuix.TextButton(
                        onClick = { showRemoveWallpaperConfirm = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MiuixAppTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = strings.cancel,
                            style = MiuixAppTheme.typography.labelLarge,
                            color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    dev.shephard.player.ui.miuix.TextButton(
                        onClick = {
                            showRemoveWallpaperConfirm = false
                            scope.launch { prefs.setWallpaperUri("") }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MiuixAppTheme.colorScheme.primary.copy(alpha = 0.16f))
                    ) {
                        Text(
                            text = strings.removeWallpaper,
                            style = MiuixAppTheme.typography.labelLarge,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        )
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

SettingsPageScaffold(title = strings.playbackSettings, onBack = onBack) {
        SectionCard {
            Text(strings.playbackSettings, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            ToggleRow(label = strings.crossfade, checked = crossfade) { scope.launch { prefs.setCrossfadeEnabled(it) } }
            ToggleRow(label = strings.gapless, checked = gapless) { scope.launch { prefs.setGaplessEnabled(it) } }
            ToggleRow(label = strings.playWithOthers, checked = playWith) { scope.launch { prefs.setPlayWithOthers(it) } }
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

val density = LocalDensity.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val appBarHeight = 56.dp
    val headerTopPadding = statusBarPadding + appBarHeight + 40.dp

val lazyListState = rememberLazyListState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    var logoHeightPx by remember { mutableIntStateOf(0) }
    var headerHeightDp by remember { mutableStateOf(190.dp) }
    var appIconBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sizePx = with(density) { 88.dp.roundToPx() }
                val shrink = context.applicationInfo.loadIcon(context.packageManager) is android.graphics.drawable.AdaptiveIconDrawable
                val loader = me.zhanghai.android.appiconloader.AppIconLoader(sizePx, shrink, context)
                appIconBitmap = loader.loadIcon(context.applicationInfo, false)
            } catch (e: Exception) {
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
    BgEffectBackground(
        isDarkTheme = isDarkTheme,
        modifier = Modifier.fillMaxSize(),
        isFullSize = true,

surface = Color.Black,
        alpha = { 1f - scrollProgress * 0.85f }
    ) {

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
                        painter = painterResource(id = R.mipmap.ic_launcher),
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
                .overScrollVertical()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
        ) {

item(key = "logoSpacer") {

Box(
                    Modifier
                        .fillMaxWidth()
                        .height(headerTopPadding + headerHeightDp + 88.dp)
                        .onSizeChanged { size -> logoHeightPx = size.height }
                )
            }
            item(key = "about_content") {
                Column(modifier = Modifier.fillParentMaxHeight()) {
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
                            title = "Miuix",
                            summary = strings.miuixDescription,
                            onClick = { uriHandler.openUri("https://github.com/composex/miuix") }
                        )
                    }
                    Spacer(Modifier.height(110.dp))
                }
            }
        }

SmallTopAppBar(
            title = strings.aboutSectionTitle,
            modifier = Modifier.align(Alignment.TopCenter),
            color = MiuixAppTheme.colorScheme.background.copy(alpha = scrollProgress),
            titleColor = MiuixAppTheme.colorScheme.onBackground.copy(alpha = scrollProgress),
            scrollBehavior = topAppBarScrollBehavior,
            defaultWindowInsetsPadding = false,
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .bounceClick { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backContentDescription,
                        tint = MiuixAppTheme.colorScheme.onBackground
                    )
                }
            }
        )
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
                    fallbackColor = androidx.compose.ui.graphics.Color.Transparent
                )
            } else Modifier,
            color = if (pageBackdrop != null)
                androidx.compose.ui.graphics.Color.Transparent
            else
                MiuixAppTheme.colorScheme.background.copy(alpha = scrollProgress),
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
