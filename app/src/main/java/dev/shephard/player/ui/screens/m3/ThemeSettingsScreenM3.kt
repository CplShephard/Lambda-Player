// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewCarousel
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalLayoutDirection
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
import dev.shephard.player.ui.components.m3.BaseWidget
import dev.shephard.player.ui.components.m3.ColorSwatchPreview
import dev.shephard.player.ui.components.m3.DropDownMenuWidget
import dev.shephard.player.ui.components.m3.RadioButtonWidget
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.components.m3.SwitchWidget
import dev.shephard.player.ui.i18n.AllLanguages
import dev.shephard.player.ui.glass.LocalWallpaperEnabled
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
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
    // Material 3 always uses the Monet (wallpaper-dynamic) palette — there is
    // no "Miuix custom colors" switch in the M3 settings (as in the original
    // M3 theme settings file).
    val useMiuixMonet = true
    val dynamicColor by prefs.dynamicColor.collectAsState(initial = false)
    val seedColor by prefs.seedColor.collectAsState(initial = PresetColors.first().color.toArgb())
    val blurEnabled by prefs.liquidGlassEnabled.collectAsState(initial = false)
    val appleFloatingBar by prefs.useAppleFloatingBar.collectAsState(initial = false)
    val predictiveBack by prefs.predictiveBackAnimation.collectAsState(initial = PredictiveBackAnimation.MIUIX)
    val predictiveBackDirection by prefs.predictiveBackExitDirection
        .collectAsState(initial = PredictiveBackExitDirection.FOLLOW_GESTURE)

    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = PreferencesManager.cachedWallpaperBrightness)
    val musicsLayout by prefs.musicsLayout.collectAsState(initial = LayoutMode.LIST)
    val playlistsLayout by prefs.playlistsLayout.collectAsState(initial = LayoutMode.LIST)
    val language by prefs.language.collectAsState(initial = "en")

    var showRemoveWallpaperConfirm by remember { mutableStateOf(false) }
    var wallpaperBrightnessValue by remember { mutableFloatStateOf(PreferencesManager.cachedWallpaperBrightness) }
    LaunchedEffect(wallpaperBrightness) {
        wallpaperBrightnessValue = wallpaperBrightness
    }

    // Monochrome has no coloured seed to pick — hide the grid (same as the
    // Miuix engine fix). Monet is always on for Material 3.
    val accentGridTarget = useMiuixMonet &&
        paletteStyle != PaletteStyle.Monochrome &&
        (!dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
    val accentGridState = remember { MutableTransitionState(accentGridTarget) }
    LaunchedEffect(accentGridTarget) { accentGridState.targetState = accentGridTarget }

    val themeModeList: List<String> = listOf(strings.lightMode, strings.darkMode, strings.autoMode)
    val themeModeOptions = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
    val themeModeIndex = themeModeOptions.indexOf(themeMode).coerceAtLeast(0)

    val languageList = AllLanguages.map { it.displayName }
    val languageIndex = AllLanguages.indexOfFirst { it.code == language }.coerceAtLeast(0)

    val paletteStyleOptions = PaletteStyle.entries
    val paletteIndex = paletteStyleOptions.indexOf(paletteStyle).coerceAtLeast(0)

    val isSpec2025Supported = paletteStyle.supportsSpec2025
    val availableSpecs = if (isSpec2025Supported) ThemeColorSpec.entries else listOf(ThemeColorSpec.SPEC_2021)
    val activeSpec = if (!isSpec2025Supported) ThemeColorSpec.SPEC_2021 else colorSpec
    val specIndex = availableSpecs.indexOf(activeSpec).coerceAtLeast(0)

    val predictiveBackOptions = PredictiveBackAnimation.entries
    val predictiveBackIndex = predictiveBackOptions.indexOf(predictiveBack).coerceAtLeast(0)

    val predictiveDirectionOptions = PredictiveBackExitDirection.entries
    val predictiveDirectionIndex = predictiveDirectionOptions.indexOf(predictiveBackDirection).coerceAtLeast(0)

    // ── Predictive back dialogs (exactly like InstallerX M3 page) ────────────
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { prefs.setPredictiveBackAnimation(animation) }
                                    showPredictiveBackAnimationDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = animation == predictiveBack,
                                onClick = {
                                    scope.launch { prefs.setPredictiveBackAnimation(animation) }
                                    showPredictiveBackAnimationDialog = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = predictiveBackDisplayName(animation, strings),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPredictiveBackAnimationDialog = false }) {
                    Text(strings.close)
                }
            },
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { prefs.setPredictiveBackExitDirection(direction) }
                                    showPredictiveBackExitDirectionDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = direction == predictiveBackDirection,
                                onClick = {
                                    scope.launch { prefs.setPredictiveBackExitDirection(direction) }
                                    showPredictiveBackExitDirectionDialog = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = predictiveDirectionDisplayName(direction, strings),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPredictiveBackExitDirectionDialog = false }) {
                    Text(strings.close)
                }
            },
        )
    }

    // ── Wallpaper picking (same flow as the Miuix page) ──────────────────────
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
        val outputUri = FileProvider.getUriForFile(
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

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(strings.themeSettings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = wallpaperAdaptiveTextColor(fallback = MaterialTheme.colorScheme.onSurface),
                    navigationIconContentColor = wallpaperAdaptiveTextColor(fallback = MaterialTheme.colorScheme.onSurface),
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection) + 20.dp,
                top = paddingValues.calculateTopPadding() + 16.dp,
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection) + 20.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Group 1: UI Style ────────────────────────────────────────────
            item {
                SegmentedColumn(title = strings.themeUiStyleSection) {
                    item {
                        RadioButtonWidget(
                            title = strings.googleUi,
                            description = strings.uiEngineDescription,
                            iconPlaceholder = false,
                            selected = !useMiuix,
                            onClick = { scope.launch { prefs.setUseMiuix(false) } }
                        )
                    }
                    item {
                        RadioButtonWidget(
                            title = strings.miuixUi,
                            description = strings.uiEngineDescription,
                            iconPlaceholder = false,
                            selected = useMiuix,
                            onClick = { scope.launch { prefs.setUseMiuix(true) } }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = Icons.Default.ViewCarousel,
                            title = strings.m3FloatingBar,
                            description = strings.m3FloatingBarDescription,
                            checked = appleFloatingBar,
                            onCheckedChange = { scope.launch { prefs.setUseAppleFloatingBar(it) } }
                        )
                    }
                }
            }

            // ── Group 2: Material 3 UI (Monet colors / theme mode) ─────────────
            item {
                SegmentedColumn(title = strings.themeM3UiSection) {
                    item {
                        DropDownMenuWidget(
                            icon = Icons.Default.DarkMode,
                            title = strings.themeMode,
                            description = themeModeList[themeModeIndex],
                            choice = themeModeIndex,
                            data = themeModeList,
                            onChoiceChange = { index ->
                                scope.launch { prefs.setThemeMode(themeModeOptions[index].toPreferenceInt()) }
                            }
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
                    // Monet (wallpaper-dynamic colours) is always on for the
                    // Material 3 engine — the custom-colours toggle is a Miuix
                    // setting and is not shown here.
                    item {
                        Column {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                SwitchWidget(
                                    icon = Icons.TwoTone.InvertColors,
                                    title = strings.dynamicColor,
                                    description = strings.dynamicColorDescription,
                                    checked = dynamicColor,
                                    onCheckedChange = { scope.launch { prefs.setDynamicColor(it) } }
                                )
                            }
                            DropDownMenuWidget(
                                icon = Icons.Default.Palette,
                                title = strings.paletteStyle,
                                description = paletteStyle.displayName,
                                choice = paletteIndex,
                                data = paletteStyleOptions.map { it.displayName },
                                onChoiceChange = { index ->
                                    val newStyle = paletteStyleOptions[index]
                                    if (newStyle != paletteStyle) {
                                        scope.launch { prefs.setPaletteStyle(newStyle) }
                                    }
                                }
                            )
                            DropDownMenuWidget(
                                icon = Icons.Default.Tune,
                                title = strings.colorSpec,
                                description = if (!isSpec2025Supported) strings.colorSpecOnly2021 else colorSpec.displayName,
                                choice = specIndex,
                                data = availableSpecs.map { it.displayName },
                                onChoiceChange = { index ->
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

            // ── Group 3: Accent color grid ───────────────────────────────────
            item {
                AnimatedVisibility(
                    visibleState = accentGridState,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                        expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                        shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                ) {
                    SegmentedColumn(title = strings.accentColor) {
                        item(
                            forceFlatBottom = true,
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

            // ── Group 4: Wallpaper (M3 version of the Miuix section) ────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.wallpaper,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
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
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
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
                                        Icon(Icons.Filled.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                                        .clickable { wallpaperPicker.launch(arrayOf("image/*")) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (wallpaper.isEmpty()) strings.chooseFromGallery else strings.changeWallpaper,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (wallpaper.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                                            .clickable {
                                                runCatching {
                                                    launchWallpaperCrop(Uri.parse(wallpaper))
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = strings.edit,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
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
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFE53935)
                                        )
                                    }
                                }
                            }
                        }

                        if (wallpaper.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = strings.wallpaperBrightness,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = wallpaperBrightnessValue,
                                onValueChange = { wallpaperBrightnessValue = it },
                                onValueChangeFinished = { scope.launch { prefs.setWallpaperBrightness(wallpaperBrightnessValue) } },
                                valueRange = 0f..1f,
                            )
                        }
                    }
                }
            }

            // ── Group 5: Layout (Music / Playlists grid-list switchers) ──────
            item {
                SegmentedColumn(title = strings.layout) {
                    item {
                        DropDownMenuWidget(
                            icon = Icons.Default.ViewList,
                            title = strings.musicsLayout,
                            description = if (musicsLayout == LayoutMode.GRID) strings.grid else strings.list,
                            choice = if (musicsLayout == LayoutMode.GRID) 1 else 0,
                            data = listOf(strings.list, strings.grid),
                            onChoiceChange = { index ->
                                scope.launch { prefs.setMusicsLayout(if (index == 1) LayoutMode.GRID else LayoutMode.LIST) }
                            }
                        )
                    }
                    item {
                        DropDownMenuWidget(
                            icon = Icons.Default.GridView,
                            title = strings.playlistsLayout,
                            description = if (playlistsLayout == LayoutMode.GRID) strings.grid else strings.list,
                            choice = if (playlistsLayout == LayoutMode.GRID) 1 else 0,
                            data = listOf(strings.list, strings.grid),
                            onChoiceChange = { index ->
                                scope.launch { prefs.setPlaylistsLayout(if (index == 1) LayoutMode.GRID else LayoutMode.LIST) }
                            }
                        )
                    }
                }
            }

            // ── Group 6: Predictive Back (Android 14+, InstallerX exactly) ──
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                item {
                    SegmentedColumn(title = strings.predictiveBackTitle) {
                        item {
                            BaseWidget(
                                icon = Icons.TwoTone.ColorLens,
                                title = strings.predictiveBackTitle,
                                description = predictiveBackDisplayName(predictiveBack, strings),
                                onClick = { showPredictiveBackAnimationDialog = true },
                            )
                        }
                        item(
                            animatedVisibility = predictiveBack == PredictiveBackAnimation.SCALE,
                            forceFlatBottom = predictiveBack == PredictiveBackAnimation.SCALE,
                        ) {
                            BaseWidget(
                                icon = Icons.Default.Tune,
                                title = strings.predictiveBackExitDirectionTitle,
                                description = predictiveDirectionDisplayName(predictiveBackDirection, strings),
                                onClick = { showPredictiveBackExitDirectionDialog = true },
                            )
                        }
                    }
                }
            }

            // ── Group 7: Language (switcher popup like the other options) ────
            item {
                SegmentedColumn(title = strings.language) {
                    item {
                        DropDownMenuWidget(
                            icon = Icons.Filled.Translate,
                            title = strings.language,
                            description = AllLanguages.firstOrNull { it.code == language }?.displayName ?: language,
                            choice = languageIndex,
                            data = languageList,
                            onChoiceChange = { index ->
                                val lang = AllLanguages[index]
                                if (lang.code != language) {
                                    scope.launch { prefs.setLanguage(lang.code) }
                                }
                            },
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
            text = {
                Text(
                    text = strings.removeWallpaperConfirm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveWallpaperConfirm = false
                        scope.launch { prefs.setWallpaperUri("") }
                    }
                ) {
                    Text(strings.removeWallpaper)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveWallpaperConfirm = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

private fun predictiveBackDisplayName(
    animation: PredictiveBackAnimation,
    strings: dev.shephard.player.ui.i18n.Strings,
): String = when (animation) {
    PredictiveBackAnimation.NONE -> strings.predictiveBackNone
    PredictiveBackAnimation.AOSP -> strings.predictiveBackAosp
    PredictiveBackAnimation.MIUIX -> strings.predictiveBackMiuix
    PredictiveBackAnimation.SCALE -> strings.predictiveBackScale
    PredictiveBackAnimation.CLASSIC -> strings.predictiveBackClassic
}

private fun predictiveDirectionDisplayName(
    direction: PredictiveBackExitDirection,
    strings: dev.shephard.player.ui.i18n.Strings,
): String = when (direction) {
    PredictiveBackExitDirection.FOLLOW_GESTURE -> strings.predictiveBackFollowGesture
    PredictiveBackExitDirection.ALWAYS_RIGHT -> strings.predictiveBackAlwaysRight
    PredictiveBackExitDirection.ALWAYS_LEFT -> strings.predictiveBackAlwaysLeft
}
