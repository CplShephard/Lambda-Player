// SPDX-License-Identifier: GPL-3.0-only
// Material 3 Expressive - Theme Settings with Lineage + M3 Switchers
package dev.shephard.player.ui.screens.m3

import android.os.Build
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import dev.shephard.player.ui.components.m3.DropDownMenuWidget
import dev.shephard.player.ui.components.m3.LineageListItem
import dev.shephard.player.ui.components.m3.LineageSectionHeader
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
    val predictiveBackDirection by prefs.predictiveBackExitDirection.collectAsState(initial = PredictiveBackExitDirection.FOLLOW_GESTURE)

    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = PreferencesManager.cachedWallpaperBrightness)
    val musicsLayout by prefs.musicsLayout.collectAsState(initial = LayoutMode.LIST)
    val playlistsLayout by prefs.playlistsLayout.collectAsState(initial = LayoutMode.LIST)
    val language by prefs.language.collectAsState(initial = "en")

    var showRemoveWallpaperConfirm by remember { mutableStateOf(false) }
    var wallpaperBrightnessValue by remember { mutableFloatStateOf(PreferencesManager.cachedWallpaperBrightness) }
    LaunchedEffect(wallpaperBrightness) { wallpaperBrightnessValue = wallpaperBrightness }

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
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(strings.themeSettings) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            SwitchWidget(
                                icon = Icons.TwoTone.InvertColors,
                                title = strings.blurEffect,
                                description = strings.blurEffectDescription,
                                checked = blurEnabled,
                                onCheckedChange = { scope.launch { prefs.setLiquidGlassEnabled(it) } }
                            )
                        }
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

            // Wallpaper - solid background (fix #5)
            item {
                SegmentedColumn(title = strings.wallpaper) {
                    item {
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
                    }
                    if (wallpaper.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(strings.wallpaperBrightness, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(value = wallpaperBrightnessValue, onValueChange = { wallpaperBrightnessValue = it }, onValueChangeFinished = { scope.launch { prefs.setWallpaperBrightness(wallpaperBrightnessValue) } }, valueRange = 0f..1f)
                            }
                        }
                    }
                }
            }

            // Layout - switchers restored #1
            item {
                SegmentedColumn(title = strings.layout) {
                    item {
                        DropDownMenuWidget(
                            icon = Icons.Filled.ViewList,
                            title = strings.musicsLayout,
                            choice = if (musicsLayout == LayoutMode.GRID) 1 else 0,
                            data = listOf(strings.list, strings.grid),
                            onChoiceChange = { idx -> scope.launch { prefs.setMusicsLayout(if (idx == 1) LayoutMode.GRID else LayoutMode.LIST) } }
                        )
                    }
                    item {
                        DropDownMenuWidget(
                            icon = Icons.Filled.GridView,
                            title = strings.playlistsLayout,
                            choice = if (playlistsLayout == LayoutMode.GRID) 1 else 0,
                            data = listOf(strings.list, strings.grid),
                            onChoiceChange = { idx -> scope.launch { prefs.setPlaylistsLayout(if (idx == 1) LayoutMode.GRID else LayoutMode.LIST) } }
                        )
                    }
                }
            }

            // Predictive Back - switcher dropdown #7 + bottom direction switcher (InstallerX Revived)
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
                        item(animatedVisibility = predictiveBack == PredictiveBackAnimation.SCALE || predictiveBack == PredictiveBackAnimation.AOSP) {
                            Column {
                                DropDownMenuWidget(
                                    icon = Icons.Filled.Tune,
                                    title = strings.predictiveBackExitDirectionTitle,
                                    description = strings.predictiveBackExitDirectionDescription,
                                    choice = PredictiveBackExitDirection.entries.indexOf(predictiveBackDirection).coerceAtLeast(0),
                                    data = PredictiveBackExitDirection.entries.map { predictiveDirectionDisplayName(it, strings) },
                                    onChoiceChange = { idx -> scope.launch { prefs.setPredictiveBackExitDirection(PredictiveBackExitDirection.entries[idx]) } }
                                )
                                M3PredictiveBackDirectionBottomSwitcher(
                                    selectedDirection = predictiveBackDirection,
                                    onSelect = { dir -> scope.launch { prefs.setPredictiveBackExitDirection(dir) } }
                                )
                            }
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

@Composable
private fun M3PredictiveBackDirectionBottomSwitcher(
    selectedDirection: PredictiveBackExitDirection,
    onSelect: (PredictiveBackExitDirection) -> Unit
) {
    val options = listOf(
        PredictiveBackExitDirection.FOLLOW_GESTURE,
        PredictiveBackExitDirection.ALWAYS_RIGHT,
        PredictiveBackExitDirection.ALWAYS_LEFT
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { dir ->
            val isSelected = dir == selectedDirection
            androidx.compose.material3.FilterChip(
                selected = isSelected,
                onClick = { onSelect(dir) },
                label = {
                    Text(
                        text = when (dir) {
                            PredictiveBackExitDirection.FOLLOW_GESTURE -> "Follow"
                            PredictiveBackExitDirection.ALWAYS_RIGHT -> "Right"
                            PredictiveBackExitDirection.ALWAYS_LEFT -> "Left"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
