// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 - Subpages (Settings, Player, About, Stats)
package dev.shephard.player.ui.screens.m3

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.R
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.ListenStatsCalculator
import dev.shephard.player.data.StatsPeriod
import dev.shephard.player.data.StatsTrackEntry
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.m3.LineageListItem
import dev.shephard.player.ui.components.m3.LineageListItemWithThumbnail
import dev.shephard.player.ui.components.m3.LineageSectionHeader
import dev.shephard.player.ui.glass.LocalWallpaperEnabled
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenM3(
    playerViewModel: PlayerViewModel = viewModel(),
    onOpenThemeSettings: () -> Unit = {},
    onOpenPlayerSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenStats: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val strings = LocalStrings.current
    val totalMs by playerViewModel.totalListeningMsLive.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
                    titleContentColor = wallpaperAdaptiveTextColor(fallback = MaterialTheme.colorScheme.onSurface),
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 80.dp)
        ) {
            item {
                LineageListItem(
                    headline = strings.totalListeningTime,
                    supporting = formatListeningTimeM3Lineage(totalMs, strings),
                    onClick = onOpenStats
                )
            }
            item { LineageSectionHeader(title = strings.settings) }
            item {
                LineageListItem(
                    headline = strings.themeSettings,
                    supporting = strings.themeSettingsSummary,
                    leadingContent = { Icon(Icons.Filled.ColorLens, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = onOpenThemeSettings
                )
            }
            item {
                LineageListItem(
                    headline = strings.playbackSettings,
                    supporting = strings.playbackSettingsSummary,
                    leadingContent = { Icon(Icons.Filled.Headphones, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = onOpenPlayerSettings
                )
            }
            item {
                LineageListItem(
                    headline = strings.aboutLambdaPlayerTitle,
                    supporting = strings.aboutLambdaPlayerSummary,
                    leadingContent = { Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = onOpenAbout
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsScreenM3(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val crossfade by prefs.crossfadeEnabled.collectAsState(initial = false)
    val gapless by prefs.gaplessEnabled.collectAsState(initial = true)
    val playWith by prefs.playWithOthers.collectAsState(initial = false)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(strings.playbackSettings) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
                    titleContentColor = wallpaperAdaptiveTextColor(fallback = MaterialTheme.colorScheme.onSurface),
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
        ) {
            item { LineageSectionHeader(title = strings.playbackSettings) }
            item {
                LineageListItem(
                    headline = strings.crossfade,
                    trailingContent = {
                        androidx.compose.material3.Switch(checked = crossfade, onCheckedChange = { scope.launch { prefs.setCrossfadeEnabled(it) } })
                    }
                )
            }
            item {
                LineageListItem(
                    headline = strings.gapless,
                    trailingContent = {
                        androidx.compose.material3.Switch(checked = gapless, onCheckedChange = { scope.launch { prefs.setGaplessEnabled(it) } })
                    }
                )
            }
            item {
                LineageListItem(
                    headline = strings.playWithOthers,
                    trailingContent = {
                        androidx.compose.material3.Switch(checked = playWith, onCheckedChange = { scope.launch { prefs.setPlayWithOthers(it) } })
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreenM3(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val strings = LocalStrings.current
    val density = LocalDensity.current
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() } catch (_: Exception) { "" }
    }
    var appIconBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val sizePx = with(density) { 88.dp.roundToPx() }
                val shrink = context.applicationInfo.loadIcon(context.packageManager) is android.graphics.drawable.AdaptiveIconDrawable
                val loader = AppIconLoader(sizePx, shrink, context)
                appIconBitmap = loader.loadIcon(context.applicationInfo, false)
            } catch (_: Exception) { appIconBitmap = null }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(strings.aboutSectionTitle) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.size(88.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                val bitmap = appIconBitmap
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = strings.appName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Image(painter = painterResource(id = R.mipmap.ic_launcher), contentDescription = strings.appName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(strings.appName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${strings.version} $versionName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))

            LineageSectionHeader(title = strings.aboutSectionTitle, modifier = Modifier.fillMaxWidth())
            LineageListItem(
                headline = strings.github,
                supporting = "CplShephard",
                leadingContent = { Icon(Icons.Filled.Code, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = { uriHandler.openUri("https://github.com/CplShephard") },
                modifier = Modifier.fillMaxWidth()
            )
            LineageListItem(
                headline = strings.sourceCode,
                supporting = "github.com/CplShephard/Lambda-Player",
                leadingContent = { Icon(Icons.Filled.Info, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = { uriHandler.openUri("https://github.com/CplShephard/Lambda-Player") },
                modifier = Modifier.fillMaxWidth()
            )
            LineageListItem(
                headline = "Miuix",
                supporting = strings.miuixDescription,
                leadingContent = { Icon(Icons.Filled.Layers, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = { uriHandler.openUri("https://github.com/miuix-project/miuix") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreenM3(
    playerViewModel: PlayerViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current
    val allEvents by playerViewModel.statsEventsFlow.collectAsState()
    val tracks by libraryViewModel.tracks.collectAsState()
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.Today) }

    val snapshot = remember(allEvents, selectedPeriod) {
        val periodEvents = ListenStatsCalculator.filterEventsForPeriod(allEvents, selectedPeriod)
        ListenStatsCalculator.buildSnapshot(periodEvents)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(strings.statsTitle) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period chips like SortingChip
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val periods = listOf(StatsPeriod.Today to strings.statsPeriodToday, StatsPeriod.ThisWeek to strings.statsPeriodThisWeek, StatsPeriod.ThisMonth to strings.statsPeriodThisMonth, StatsPeriod.AllTime to strings.statsPeriodAllTime)
                periods.forEach { (period, label) ->
                    androidx.compose.material3.FilterChip(
                        selected = period == selectedPeriod,
                        onClick = { selectedPeriod = period },
                        label = { Text(label) }
                    )
                }
            }

            Text(formatListeningTimeM3Lineage(snapshot.summary.totalListenedMs, strings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            if (snapshot.trackEntries.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(strings.statsEmptyTitle, style = MaterialTheme.typography.titleMedium)
                    Text(strings.statsEmptySubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(strings.statsTopTracks, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                snapshot.trackEntries.take(10).forEachIndexed { index, entry ->
                    val track = remember(entry.trackId, tracks) {
                        tracks.firstOrNull { it.id == entry.trackId } ?: tracks.firstOrNull { it.title == entry.title && it.artist == entry.artistName }
                    }
                    val coverUri = track?.albumArtUri ?: entry.albumArtUri
                    val displayTitle = track?.title?.takeIf { it.isNotBlank() } ?: entry.title
                    val displayArtist = track?.artist?.takeIf { it.isNotBlank() } ?: entry.artistName

                    LineageListItemWithThumbnail(
                        headline = "${index + 1}. $displayTitle",
                        supporting = "$displayArtist • ${formatListeningTimeM3Lineage(entry.listenedMs, strings)}",
                        thumbnailModel = coverUri,
                        placeholderIcon = Icons.Filled.MusicNote
                    )
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

private fun formatListeningTimeM3Lineage(ms: Long, strings: dev.shephard.player.ui.i18n.Strings): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return buildString {
        if (h > 0) append("${h}h ")
        if (m > 0 || h > 0) append("${m}m ")
        append("${s}s")
    }
}
