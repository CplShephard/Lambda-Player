@file:OptIn(ExperimentalFoundationApi::class)

package dev.shephard.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.rememberBounceOverscrollEffect
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.OpenInNew
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
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val strings = LocalStrings.current

    val totalMs by prefs.totalListeningMs.collectAsState(initial = 0L)
    val crossfade by prefs.crossfadeEnabled.collectAsState(initial = false)
    val gapless by prefs.gaplessEnabled.collectAsState(initial = true)
    val playWith by prefs.playWithOthers.collectAsState(initial = false)
    val accent by prefs.accentColor.collectAsState(initial = AccentPalette.first())
    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = 0.55f)
    val language by prefs.language.collectAsState(initial = "en")
    val themeMode by prefs.themeMode.collectAsState(initial = ThemeModePreference.LIGHT)
    val dynamicColor by prefs.dynamicColor.collectAsState(initial = false)
    val playlistsLayout by prefs.playlistsLayout.collectAsState(initial = LayoutMode.LIST)
    val musicsLayout by prefs.musicsLayout.collectAsState(initial = LayoutMode.LIST)

    var langMenuOpen by remember { mutableStateOf(false) }
    var customPickerOpen by remember { mutableStateOf(false) }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            scope.launch { prefs.setWallpaperUri(uri.toString()) }
        }
    }

    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) { null }
    }
    val versionName = packageInfo?.versionName ?: "2.0"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .overscroll(rememberBounceOverscrollEffect())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User profile card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick { uriHandler.openUri("https://github.com/CplShephard") },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = "CplShephard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.viewGithub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Total listening time
        SectionCard {
            Text(
                text = strings.totalListeningTime,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatListeningTime(totalMs),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Playback Settings
        SectionCard {
            Text(
                text = strings.playbackSettings,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(label = strings.crossfade, checked = crossfade) {
                scope.launch { prefs.setCrossfadeEnabled(it) }
            }
            ToggleRow(label = strings.gapless, checked = gapless) {
                scope.launch { prefs.setGaplessEnabled(it) }
            }
            ToggleRow(label = strings.playWithOthers, checked = playWith) {
                scope.launch { prefs.setPlayWithOthers(it) }
            }
        }

        // Theme settings
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.themeSettings,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 16.dp)
                )
                ThemeModeSegmentedSwitch(
                    selectedMode = themeMode,
                    lightLabel = strings.lightMode,
                    autoLabel = strings.autoMode,
                    darkLabel = strings.darkMode,
                    onModeSelected = { mode -> scope.launch { prefs.setThemeMode(mode) } }
                )
            }

            Spacer(Modifier.height(10.dp))
            ToggleRow(label = strings.dynamicColor, checked = dynamicColor) { enabled ->
                if (enabled) customPickerOpen = false
                scope.launch { prefs.setDynamicColor(enabled) }
            }

            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.alpha(if (dynamicColor) 0.38f else 1f)
            ) {
                Text(
                    text = strings.accentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentPalette.forEach { argb ->
                        val selected = argb == accent
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .bounceClick(enabled = !dynamicColor) {
                                    scope.launch { prefs.setAccentColor(argb) }
                                }
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f))
                                )
                            }
                        }
                    }
                    val isCustomSelected = accent !in AccentPalette
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
                            .bounceClick(enabled = !dynamicColor) { customPickerOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ColorLens,
                            contentDescription = strings.customColor,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        if (isCustomSelected) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                            )
                        }
                    }
                }

                if (accent !in AccentPalette) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(accent))
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "#%06X".format(accent and 0xFFFFFF),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Layout toggles
            Text(
                text = strings.playlistsLayout,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayoutToggleChip(
                    selected = playlistsLayout == LayoutMode.LIST,
                    label = strings.list,
                    icon = Icons.Filled.List,
                    onClick = { scope.launch { prefs.setPlaylistsLayout(LayoutMode.LIST) } }
                )
                LayoutToggleChip(
                    selected = playlistsLayout == LayoutMode.GRID,
                    label = strings.grid,
                    icon = Icons.Filled.ViewModule,
                    onClick = { scope.launch { prefs.setPlaylistsLayout(LayoutMode.GRID) } }
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = strings.musicsLayout,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayoutToggleChip(
                    selected = musicsLayout == LayoutMode.LIST,
                    label = strings.list,
                    icon = Icons.Filled.List,
                    onClick = { scope.launch { prefs.setMusicsLayout(LayoutMode.LIST) } }
                )
                LayoutToggleChip(
                    selected = musicsLayout == LayoutMode.GRID,
                    label = strings.grid,
                    icon = Icons.Filled.ViewModule,
                    onClick = { scope.launch { prefs.setMusicsLayout(LayoutMode.GRID) } }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Wallpaper
            Text(
                text = strings.wallpaper,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            if (wallpaper.isNotEmpty()) {
                var previewLoaded by remember(wallpaper) { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = wallpaper,
                        contentDescription = "Wallpaper preview",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        onState = { previewLoaded = it is AsyncImagePainter.State.Success }
                    )
                    if (!previewLoaded) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .bounceClick { wallpaperPicker.launch(arrayOf("image/*")) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (wallpaper.isEmpty()) strings.chooseFromGallery else strings.changeWallpaper,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (wallpaper.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = strings.wallpaperBrightness,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.brightness,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Slider(
                        value = wallpaperBrightness,
                        onValueChange = {
                            scope.launch { prefs.setWallpaperBrightness(it) }
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${((1f - wallpaperBrightness) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (wallpaper.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.removeWallpaper,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .bounceClick { scope.launch { prefs.setWallpaperUri("") } }
                            .padding(8.dp)
                    )
                }
            }
        }

        // Language menu
        SectionCard {
            Text(
                text = strings.language,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .bounceClick { langMenuOpen = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = AllLanguages.firstOrNull { it.code == language }?.displayName
                            ?: language,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = langMenuOpen,
                    onDismissRequest = { langMenuOpen = false }
                ) {
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

        Spacer(Modifier.height(8.dp))
        Text(
            text = "${strings.version} $versionName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 96.dp)
        )
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
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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

private fun formatListeningTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, s)
    else "%dm %02ds".format(m, s)
}
