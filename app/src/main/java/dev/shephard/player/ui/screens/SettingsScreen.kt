@file:OptIn(ExperimentalFoundationApi::class)

package dev.shephard.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.miuixWidgetClick
import dev.shephard.player.ui.components.overScrollVertical
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.ThemeModePreference
import dev.shephard.player.ui.components.CustomColorPickerDialog
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.blurSurface
import dev.shephard.player.ui.i18n.AllLanguages
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

/** Preset accent colors. The 7th slot is "Custom" — handled separately. */
private val AccentPalette = listOf(
    0xFF22C55E.toInt(), // green (lambda)
    0xFF3B82F6.toInt(), // blue
    0xFFE11D48.toInt(), // rose
    0xFFF59E0B.toInt(), // amber
    0xFF8B5CF6.toInt(), // violet
    0xFF14B8A6.toInt(), // teal
)

@Composable
fun SettingsScreen(
    onOpenThemeSettings: () -> Unit = {},
    onOpenPlayerSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .overScrollVertical()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // InstallerX tarzı ana Settings: üstte widget kart, altında üç büyük yönlendirme.
        TotalListeningTimeCard(prefs = prefs)

        SettingsNavigationCard(
            icon = Icons.Filled.ColorLens,
            title = strings.themeSettings,
            summary = "Colors, wallpaper, Liquid Glass, layout and language",
            onClick = onOpenThemeSettings
        )
        SettingsNavigationCard(
            icon = Icons.Filled.MusicNote,
            title = strings.playbackSettings,
            summary = "Crossfade, gapless playback and audio focus",
            onClick = onOpenPlayerSettings
        )
        SettingsNavigationCard(
            icon = Icons.Filled.Info,
            title = "About Lambda Player",
            summary = "Version, project links and credits",
            onClick = onOpenAbout
        )

        Spacer(Modifier.height(110.dp))
    }
}

@Composable
fun ThemeSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    val accent by prefs.accentColor.collectAsState(initial = AccentPalette.first())
    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = 0.55f)
    val cardAlpha by prefs.cardAlpha.collectAsState(initial = 0.85f)
    val language by prefs.language.collectAsState(initial = "en")
    val themeMode by prefs.themeMode.collectAsState(initial = ThemeModePreference.LIGHT)
    val dynamicColor by prefs.dynamicColor.collectAsState(initial = false)
    val liquidGlassEnabled by prefs.liquidGlassEnabled.collectAsState(initial = false)
    val playlistsLayout by prefs.playlistsLayout.collectAsState(initial = LayoutMode.LIST)
    val musicsLayout by prefs.musicsLayout.collectAsState(initial = LayoutMode.LIST)

    var langMenuOpen by remember { mutableStateOf(false) }
    var customPickerOpen by remember { mutableStateOf(false) }
    var wallpaperBrightnessValue by remember(wallpaperBrightness) { mutableStateOf(wallpaperBrightness) }
    var cardAlphaValue by remember(cardAlpha) { mutableStateOf(cardAlpha) }

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
            scope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistWallpaper(context, uri)
                prefs.setWallpaperUri((persisted ?: uri).toString())
            }
        }
    }

    SettingsPageScaffold(title = strings.themeSettings, onBack = onBack) {
        SectionCard {
            Text(strings.themeSettings, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(strings.darkMode, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
                ThemeModeSegmentedSwitch(
                    selectedMode = themeMode,
                    lightLabel = strings.lightMode,
                    autoLabel = strings.autoMode,
                    darkLabel = strings.darkMode,
                    onModeSelected = { mode -> scope.launch { prefs.setThemeMode(mode) } }
                )
            }
            Spacer(Modifier.height(8.dp))
            ToggleRow(label = strings.dynamicColor, checked = dynamicColor) { enabled ->
                if (enabled) customPickerOpen = false
                scope.launch { prefs.setDynamicColor(enabled) }
            }
            ToggleRow(label = strings.blurEffect, checked = liquidGlassEnabled) { enabled ->
                scope.launch { prefs.setLiquidGlassEnabled(enabled) }
            }

            Spacer(Modifier.height(12.dp))
            Text(strings.accentColor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentPalette.forEach { argb ->
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
            Text(strings.wallpaper, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(10.dp))
            if (wallpaper.isNotEmpty()) {
                var previewLoaded by remember(wallpaper) { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = wallpaper,
                        contentDescription = "Wallpaper preview",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        onState = { previewLoaded = it is AsyncImagePainter.State.Success }
                    )
                    if (!previewLoaded) Icon(Icons.Filled.BrokenImage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
            }
            SettingsActionRow(
                icon = Icons.Filled.Image,
                title = if (wallpaper.isEmpty()) strings.chooseFromGallery else strings.changeWallpaper,
                onClick = { wallpaperPicker.launch(arrayOf("image/*")) }
            )
            if (wallpaper.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(strings.wallpaperBrightness, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = wallpaperBrightnessValue,
                    onValueChange = { wallpaperBrightnessValue = it },
                    onValueChangeFinished = { scope.launch { prefs.setWallpaperBrightness(wallpaperBrightnessValue) } },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
                Text(strings.cardOpacity, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = cardAlphaValue,
                    onValueChange = { cardAlphaValue = it },
                    onValueChangeFinished = { scope.launch { prefs.setCardAlpha(cardAlphaValue) } },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
                Text(
                    text = strings.removeWallpaper,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.bounceClick { scope.launch { prefs.setWallpaperUri("") } }.padding(8.dp)
                )
            }
        }

        SectionCard {
            Text("Layout", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(10.dp))
            Text(strings.musicsLayout, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayoutToggleChip(musicsLayout == LayoutMode.LIST, strings.list, Icons.AutoMirrored.Filled.List) { scope.launch { prefs.setMusicsLayout(LayoutMode.LIST) } }
                LayoutToggleChip(musicsLayout == LayoutMode.GRID, strings.grid, Icons.Filled.ViewModule) { scope.launch { prefs.setMusicsLayout(LayoutMode.GRID) } }
            }
            Spacer(Modifier.height(12.dp))
            Text(strings.playlistsLayout, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayoutToggleChip(playlistsLayout == LayoutMode.LIST, strings.list, Icons.AutoMirrored.Filled.List) { scope.launch { prefs.setPlaylistsLayout(LayoutMode.LIST) } }
                LayoutToggleChip(playlistsLayout == LayoutMode.GRID, strings.grid, Icons.Filled.ViewModule) { scope.launch { prefs.setPlaylistsLayout(LayoutMode.GRID) } }
            }
        }

        SectionCard {
            Text(strings.language, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Box {
                SettingsActionRow(
                    icon = Icons.Filled.ArrowDropDown,
                    title = AllLanguages.firstOrNull { it.code == language }?.displayName ?: language,
                    onClick = { langMenuOpen = true }
                )
                DropdownMenu(expanded = langMenuOpen, onDismissRequest = { langMenuOpen = false }) {
                    AllLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.displayName) },
                            onClick = {
                                scope.launch { prefs.setLanguage(lang.code) }
                                langMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(110.dp))
    }

    if (customPickerOpen && !dynamicColor) {
        CustomColorPickerDialog(
            onDismiss = { customPickerOpen = false },
            onColorPicked = { argb ->
                scope.launch { prefs.setAccentColor(argb) }
                customPickerOpen = false
            },
            initialArgb = accent,
            title = strings.customColorTitle,
            hexPlaceholder = strings.hexPlaceholder,
            applyLabel = strings.apply
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
            Text(strings.playbackSettings, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
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

    SettingsPageScaffold(title = "About", onBack = onBack) {
        SectionCard {
            Text(strings.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            Text("${strings.version} $versionName", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard {
            SettingsActionRow(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = "GitHub / CplShephard",
                onClick = { uriHandler.openUri("https://github.com/CplShephard") }
            )
            SettingsActionRow(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = "InstallerX Revived",
                onClick = { uriHandler.openUri("https://github.com/InstallerX-Revived/InstallerX") }
            )
            Text(
                text = "Miuix / InstallerX inspired interface polish for Lambda Player.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(Modifier.height(110.dp))
    }
}


@Composable
private fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .overScrollVertical()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                    .bounceClick { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(4.dp))
        content()
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
        modifier = Modifier.miuixWidgetClick { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
            .miuixWidgetClick { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

// İzole edilmiş: totalListeningMs PlayerViewModel'in observePosition() döngüsü tarafından
// artık 10 saniyelik paketler halinde güncelleniyor. Bu değer buradaki KENDİ collectAsState'inde toplandığı için
// recompose sadece bu küçük composable ile sınırlı kalıyor — SettingsScreen'in geri kalanı
// (toggle'lar, slider'lar) her tick'te yeniden çizilmiyor.
@Composable
private fun TotalListeningTimeCard(prefs: PreferencesManager) {
    val strings = LocalStrings.current
    val totalMs by prefs.totalListeningMs.collectAsState(initial = 0L)
    SectionCard(modifier = Modifier.miuixWidgetClick { }) {
        Text(
            text = strings.totalListeningTime,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatListeningTime(totalMs, strings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val liquidGlassOn = LocalBlurEnabled.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (liquidGlassOn) Modifier.blurSurface(enabled = true, shape = RoundedCornerShape(20.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (liquidGlassOn) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun LayoutToggleChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
            )
            .bounceClick { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ThemeModeSegmentedSwitch(
    selectedMode: Int,
    lightLabel: String,
    autoLabel: String,
    darkLabel: String,
    onModeSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .width(156.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeModeSegment(
            mode = ThemeModePreference.LIGHT,
            selectedMode = selectedMode,
            label = lightLabel,
            icon = Icons.Filled.WbSunny,
            onModeSelected = onModeSelected
        )
        ThemeModeSegment(
            mode = ThemeModePreference.AUTO,
            selectedMode = selectedMode,
            label = autoLabel,
            icon = Icons.Filled.BrightnessAuto,
            onModeSelected = onModeSelected
        )
        ThemeModeSegment(
            mode = ThemeModePreference.DARK,
            selectedMode = selectedMode,
            label = darkLabel,
            icon = Icons.Filled.NightsStay,
            onModeSelected = onModeSelected
        )
    }
}

@Composable
private fun RowScope.ThemeModeSegment(
    mode: Int,
    selectedMode: Int,
    label: String,
    icon: ImageVector,
    onModeSelected: (Int) -> Unit
) {
    val selected = mode == selectedMode
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .bounceClick(enabled = !selected) { onModeSelected(mode) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 16.dp)
        )
        Switch(checked = checked, onCheckedChange = onChange)
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
