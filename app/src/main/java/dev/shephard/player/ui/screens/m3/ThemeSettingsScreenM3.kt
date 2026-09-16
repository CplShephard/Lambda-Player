// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 + InstallerX Revived predictive back parity
package dev.shephard.player.ui.screens.m3

import android.os.Build
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.twotone.ColorLens
import androidx.compose.material.icons.twotone.InvertColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.toPreferenceInt
import dev.shephard.player.theme.PaletteStyle
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection
import dev.shephard.player.theme.ThemeColorSpec
import dev.shephard.player.theme.ThemeMode
import dev.shephard.player.ui.components.m3.ColorSwatchPreview
import dev.shephard.player.ui.components.m3.LineageListItem
import dev.shephard.player.ui.components.m3.LineageSectionHeader
import dev.shephard.player.ui.glass.LocalWallpaperEnabled
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
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
    val predictiveBackDirection by prefs.predictiveBackExitDirection.collectAsState(initial = PredictiveBackExitDirection.FOLLOW_GESTURE)

    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = PreferencesManager.cachedWallpaperBrightness)
    val musicsLayout by prefs.musicsLayout.collectAsState(initial = LayoutMode.LIST)
    val playlistsLayout by prefs.playlistsLayout.collectAsState(initial = LayoutMode.LIST)
    val language by prefs.language.collectAsState(initial = "en")

    var showRemoveWallpaperConfirm by remember { mutableStateOf(false) }
    var wallpaperBrightnessValue by remember { mutableFloatStateOf(PreferencesManager.cachedWallpaperBrightness) }
    LaunchedEffect(wallpaperBrightness) { wallpaperBrightnessValue = wallpaperBrightness }

    val accentGridTarget = paletteStyle != PaletteStyle.Monochrome && (!dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
    val accentGridState = remember { MutableTransitionState(accentGridTarget) }
    LaunchedEffect(accentGridTarget) { accentGridState.targetState = accentGridTarget }

    val themeModeList = listOf(strings.lightMode, strings.darkMode, strings.autoMode)
    val themeModeOptions = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
    val themeModeIndex = themeModeOptions.indexOf(themeMode).coerceAtLeast(0)

    val paletteStyleOptions = PaletteStyle.entries
    val paletteIndex = paletteStyleOptions.indexOf(paletteStyle).coerceAtLeast(0)

    val isSpec2025Supported = paletteStyle.supportsSpec2025
    val availableSpecs = if (isSpec2025Supported) ThemeColorSpec.entries else listOf(ThemeColorSpec.SPEC_2021)
    val activeSpec = if (!isSpec2025Supported) ThemeColorSpec.SPEC_2021 else colorSpec
    val specIndex = availableSpecs.indexOf(activeSpec).coerceAtLeast(0)

    var showPredictiveBackAnimationDialog by remember { mutableStateOf(false) }
    var showPredictiveBackExitDirectionDialog by remember { mutableStateOf(false) }

    if (showPredictiveBackAnimationDialog) {
        AlertDialog(
            onDismissRequest = { showPredictiveBackAnimationDialog = false },
            title = { Text(strings.predictiveBackDescription) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    PredictiveBackAnimation.entries.forEach { animation ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch { prefs.setPredictiveBackAnimation(animation) }
                                showPredictiveBackAnimationDialog = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = animation == predictiveBack, onClick = {
                                scope.launch { prefs.setPredictiveBackAnimation(animation) }
                                showPredictiveBackAnimationDialog = false
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(predictiveBackDisplayName(animation, strings), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPredictiveBackAnimationDialog = false }) { Text(strings.close) } },
        )
    }

    if (showPredictiveBackExitDirectionDialog) {
        AlertDialog(
            onDismissRequest = { showPredictiveBackExitDirectionDialog = false },
            title = { Text(strings.predictiveBackExitDirectionDescription) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    PredictiveBackExitDirection.entries.forEach { direction ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch { prefs.setPredictiveBackExitDirection(direction) }
                                showPredictiveBackExitDirectionDialog = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = direction == predictiveBackDirection, onClick = {
                                scope.launch { prefs.setPredictiveBackExitDirection(direction) }
                                showPredictiveBackExitDirectionDialog = false
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(predictiveDirectionDisplayName(direction, strings), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPredictiveBackExitDirectionDialog = false }) { Text(strings.close) } },
        )
    }

    var wallpaperCropOutputUri by remember { mutableStateOf<Uri?>(null) }
    val wallpaperCropLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        val output = wallpaperCropOutputUri
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null) {
            scope.launch { prefs.setWallpaperUri(output.toString()) }
        }
    }

    fun launchWallpaperCrop(sourceUri: Uri) {
        val dir = java.io.File(context.filesDir, "persisted_wallpaper").apply { mkdirs() }
        val file = java.io.File(dir, "wallpaper_${System.currentTimeMillis()}.jpg")
        val outputUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
            try { context.grantUriPermission(pkg, outputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: SecurityException) { }
        }
        if (resolved.isNotEmpty()) wallpaperCropLauncher.launch(cropIntent)
        else scope.launch {
            val persisted = dev.shephard.player.player.ImagePersistence.persistWallpaper(context, sourceUri)
            prefs.setWallpaperUri((persisted ?: sourceUri).toString())
        }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
            launchWallpaperCrop(uri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(strings.themeSettings) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
                    titleContentColor = wallpaperAdaptiveTextColor(fallback = MaterialTheme.colorScheme.onSurface),
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding(), bottom = paddingValues.calculateBottomPadding() + 80.dp)
        ) {
            // UI Style - Lineage style: simple list items
            item {
                LineageSectionHeader(title = strings.themeUiStyleSection)
                LineageListItem(
                    headline = strings.googleUi,
                    supporting = strings.uiEngineDescription,
                    leadingContent = { Icon(Icons.TwoTone.ColorLens, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = {
                        RadioButton(selected = !useMiuix, onClick = { scope.launch { prefs.setUseMiuix(false) } })
                    },
                    onClick = { scope.launch { prefs.setUseMiuix(false) } }
                )
                LineageListItem(
                    headline = strings.miuixUi,
                    supporting = strings.uiEngineDescription,
                    leadingContent = { Icon(Icons.TwoTone.ColorLens, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = {
                        RadioButton(selected = useMiuix, onClick = { scope.launch { prefs.setUseMiuix(true) } })
                    },
                    onClick = { scope.launch { prefs.setUseMiuix(true) } }
                )
            }

            // M3 UI Section
            item {
                LineageSectionHeader(title = strings.themeM3UiSection)
                LineageListItem(
                    headline = strings.themeMode,
                    supporting = themeModeList[themeModeIndex],
                    leadingContent = { Icon(Icons.Filled.DarkMode, null) },
                    onClick = {
                        val nextIdx = (themeModeIndex + 1) % themeModeOptions.size
                        scope.launch { prefs.setThemeMode(themeModeOptions[nextIdx].toPreferenceInt()) }
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    LineageListItem(
                        headline = strings.blurEffect,
                        supporting = strings.blurEffectDescription,
                        leadingContent = { Icon(Icons.TwoTone.InvertColors, null) },
                        trailingContent = {
                            androidx.compose.material3.Switch(checked = blurEnabled, onCheckedChange = { scope.launch { prefs.setLiquidGlassEnabled(it) } })
                        }
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    LineageListItem(
                        headline = strings.dynamicColor,
                        supporting = strings.dynamicColorDescription,
                        leadingContent = { Icon(Icons.TwoTone.InvertColors, null) },
                        trailingContent = {
                            androidx.compose.material3.Switch(checked = dynamicColor, onCheckedChange = { scope.launch { prefs.setDynamicColor(it) } })
                        }
                    )
                }
                LineageListItem(
                    headline = strings.paletteStyle,
                    supporting = paletteStyle.displayName,
                    leadingContent = { Icon(Icons.Filled.Palette, null) },
                    onClick = {
                        val nextIdx = (paletteIndex + 1) % paletteStyleOptions.size
                        scope.launch { prefs.setPaletteStyle(paletteStyleOptions[nextIdx]) }
                    }
                )
                LineageListItem(
                    headline = strings.colorSpec,
                    supporting = if (!isSpec2025Supported) strings.colorSpecOnly2021 else colorSpec.displayName,
                    leadingContent = { Icon(Icons.Filled.Tune, null) },
                    onClick = {
                        val nextIdx = (specIndex + 1) % availableSpecs.size
                        scope.launch { prefs.setColorSpec(availableSpecs[nextIdx]) }
                    }
                )
            }

            // Accent color grid - Lineage style: simple, no 24dp cards
            item {
                AnimatedVisibility(
                    visibleState = accentGridState,
                    enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + expandVertically(tween(400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(250, easing = FastOutSlowInEasing)) + shrinkVertically(tween(350, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        LineageSectionHeader(title = strings.accentColor)
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
                                                    isSelected = seedColor == rawColor.color.toArgb() && !(dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
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

            // Wallpaper - Lineage style
            item {
                LineageSectionHeader(title = strings.wallpaper)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.width(115.dp).aspectRatio(9f / 19.5f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        if (wallpaper.isNotEmpty()) {
                            var previewLoaded by remember(wallpaper) { mutableStateOf(false) }
                            AsyncImage(model = wallpaper, contentDescription = strings.wallpaperPreviewContentDescription, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop, onState = { previewLoaded = it is AsyncImagePainter.State.Success })
                            if (!previewLoaded) Icon(Icons.Filled.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 1f - wallpaperBrightnessValue)))
                        } else {
                            Icon(Icons.Filled.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LineageListItem(headline = if (wallpaper.isEmpty()) strings.chooseFromGallery else strings.changeWallpaper, leadingContent = { Icon(Icons.Filled.FolderOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }, onClick = { wallpaperPicker.launch(arrayOf("image/*")) })
                        if (wallpaper.isNotEmpty()) {
                            LineageListItem(headline = strings.edit, leadingContent = { Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }, onClick = { runCatching { launchWallpaperCrop(Uri.parse(wallpaper)) } })
                            LineageListItem(headline = strings.removeWallpaper, leadingContent = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp)) }, onClick = { showRemoveWallpaperConfirm = true })
                        }
                    }
                }
                if (wallpaper.isNotEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(strings.wallpaperBrightness, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(value = wallpaperBrightnessValue, onValueChange = { wallpaperBrightnessValue = it }, onValueChangeFinished = { scope.launch { prefs.setWallpaperBrightness(wallpaperBrightnessValue) } }, valueRange = 0f..1f)
                    }
                }
            }

            // Layout
            item {
                LineageSectionHeader(title = strings.layout)
                LineageListItem(
                    headline = strings.musicsLayout,
                    supporting = if (musicsLayout == LayoutMode.GRID) strings.grid else strings.list,
                    leadingContent = { Icon(Icons.Filled.ViewList, null) },
                    onClick = { scope.launch { prefs.setMusicsLayout(if (musicsLayout == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID) } }
                )
                LineageListItem(
                    headline = strings.playlistsLayout,
                    supporting = if (playlistsLayout == LayoutMode.GRID) strings.grid else strings.list,
                    leadingContent = { Icon(Icons.Filled.GridView, null) },
                    onClick = { scope.launch { prefs.setPlaylistsLayout(if (playlistsLayout == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID) } }
                )
            }

            // Predictive Back - InstallerX exact with direction switcher that appears for SCALE and AOSP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                item {
                    LineageSectionHeader(title = strings.predictiveBackTitle)
                    LineageListItem(
                        headline = strings.predictiveBackTitle,
                        supporting = predictiveBackDisplayName(predictiveBack, strings),
                        leadingContent = { Icon(Icons.TwoTone.ColorLens, null) },
                        onClick = { showPredictiveBackAnimationDialog = true }
                    )
                    AnimatedVisibility(
                        visible = predictiveBack == PredictiveBackAnimation.SCALE || predictiveBack == PredictiveBackAnimation.AOSP,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        LineageListItem(
                            headline = strings.predictiveBackExitDirectionTitle,
                            supporting = predictiveDirectionDisplayName(predictiveBackDirection, strings),
                            leadingContent = { Icon(Icons.Filled.Tune, null) },
                            onClick = { showPredictiveBackExitDirectionDialog = true }
                        )
                    }
                }
            }

            // Language
            item {
                LineageSectionHeader(title = strings.language)
                val languageList = AllLanguages.map { it.displayName }
                val languageIndex = AllLanguages.indexOfFirst { it.code == language }.coerceAtLeast(0)
                LineageListItem(
                    headline = strings.language,
                    supporting = AllLanguages.firstOrNull { it.code == language }?.displayName ?: language,
                    leadingContent = { Icon(Icons.Filled.Translate, null) },
                    onClick = {
                        val nextIdx = (languageIndex + 1) % AllLanguages.size
                        scope.launch { prefs.setLanguage(AllLanguages[nextIdx].code) }
                    }
                )
            }
        }
    }

    if (showRemoveWallpaperConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveWallpaperConfirm = false },
            title = { Text(strings.removeWallpaper) },
            text = { Text(strings.removeWallpaperConfirm, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = { TextButton(onClick = { showRemoveWallpaperConfirm = false; scope.launch { prefs.setWallpaperUri("") } }) { Text(strings.removeWallpaper) } },
            dismissButton = { TextButton(onClick = { showRemoveWallpaperConfirm = false }) { Text(strings.cancel) } },
        )
    }
}

private fun predictiveBackDisplayName(animation: PredictiveBackAnimation, strings: dev.shephard.player.ui.i18n.Strings): String = when (animation) {
    PredictiveBackAnimation.NONE -> strings.predictiveBackNone
    PredictiveBackAnimation.AOSP -> strings.predictiveBackAosp
    PredictiveBackAnimation.MIUIX -> strings.predictiveBackMiuix
    PredictiveBackAnimation.SCALE -> strings.predictiveBackScale
    PredictiveBackAnimation.CLASSIC -> strings.predictiveBackClassic
}

private fun predictiveDirectionDisplayName(direction: PredictiveBackExitDirection, strings: dev.shephard.player.ui.i18n.Strings): String = when (direction) {
    PredictiveBackExitDirection.FOLLOW_GESTURE -> strings.predictiveBackFollowGesture
    PredictiveBackExitDirection.ALWAYS_RIGHT -> strings.predictiveBackAlwaysRight
    PredictiveBackExitDirection.ALWAYS_LEFT -> strings.predictiveBackAlwaysLeft
}
