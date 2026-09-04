// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package dev.shephard.player.ui.screens.m3

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import dev.shephard.player.ui.components.m3.BaseWidget
import dev.shephard.player.ui.components.m3.NavigationItemWidget
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.components.m3.SwitchWidget
import dev.shephard.player.ui.i18n.AllLanguages
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
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val language by prefs.language.collectAsState(initial = "en")
    var showLanguageDialog by remember { mutableStateOf(false) }

    val totalMs by playerViewModel.totalListeningMsLive.collectAsState()

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.language) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AllLanguages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { prefs.setLanguage(lang.code) }
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lang.code == language,
                                onClick = {
                                    scope.launch { prefs.setLanguage(lang.code) }
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (lang.code == language) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(strings.close) }
            }
        )
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection) + 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection) + 12.dp,
                bottom = padding.calculateBottomPadding() + 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    onClick = onOpenStats,
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.totalListeningTime,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Filled.QueryStats,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = formatListeningTimeM3(totalMs, strings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                SegmentedColumn(title = strings.settings) {
                    item {
                        NavigationItemWidget(
                            icon = Icons.Filled.ColorLens,
                            title = strings.themeSettings,
                            description = strings.themeSettingsSummary,
                            onClick = onOpenThemeSettings,
                        )
                    }
                    item {
                        NavigationItemWidget(
                            icon = Icons.Filled.Headphones,
                            title = strings.playbackSettings,
                            description = strings.playbackSettingsSummary,
                            onClick = onOpenPlayerSettings,
                        )
                    }
                    item {
                        NavigationItemWidget(
                            icon = Icons.Filled.Info,
                            title = strings.aboutLambdaPlayerTitle,
                            description = strings.aboutLambdaPlayerSummary,
                            onClick = onOpenAbout,
                        )
                    }
                }
            }

            item {
                SegmentedColumn(title = strings.language) {
                    item {
                        BaseWidget(
                            icon = Icons.Filled.Translate,
                            title = strings.language,
                            description = AllLanguages.firstOrNull { it.code == language }?.displayName ?: language,
                            onClick = { showLanguageDialog = true },
                        )
                    }
                }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(strings.playbackSettings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                end = 12.dp,
                bottom = padding.calculateBottomPadding() + 40.dp
            ),
        ) {
            item {
                SegmentedColumn(title = strings.playbackSettings) {
                    item {
                        SwitchWidget(
                            icon = Icons.Default.GraphicEq,
                            title = strings.crossfade,
                            checked = crossfade,
                            onCheckedChange = { scope.launch { prefs.setCrossfadeEnabled(it) } }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = Icons.Default.MusicNote,
                            title = strings.gapless,
                            checked = gapless,
                            onCheckedChange = { scope.launch { prefs.setGaplessEnabled(it) } }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = Icons.Default.Headphones,
                            title = strings.playWithOthers,
                            checked = playWith,
                            onCheckedChange = { scope.launch { prefs.setPlayWithOthers(it) } }
                        )
                    }
                }
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
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(strings.aboutSectionTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                end = 12.dp,
                bottom = padding.calculateBottomPadding() + 40.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
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
                Spacer(Modifier.height(12.dp))
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${strings.version} $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(28.dp))
            }

            item {
                SegmentedColumn(title = strings.aboutSectionTitle) {
                    item {
                        BaseWidget(
                            icon = Icons.Default.Code,
                            title = strings.github,
                            description = "CplShephard",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { uriHandler.openUri("https://github.com/CplShephard") }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Default.Info,
                            title = strings.sourceCode,
                            description = "github.com/CplShephard/Lambda-Player",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { uriHandler.openUri("https://github.com/CplShephard/Lambda-Player") }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Default.Layers,
                            title = "Miuix",
                            description = strings.miuixDescription,
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { uriHandler.openUri("https://github.com/miuix-project/miuix") }
                        )
                    }
                }
            }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(strings.statsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StatsPeriodPillsM3(selectedPeriod = selectedPeriod, onPeriodSelected = { selectedPeriod = it })

            StatsSummaryCardM3(
                totalListenedMs = snapshot.summary.totalListenedMs,
                playCount = snapshot.summary.playCount,
                uniqueAlbumCount = snapshot.summary.uniqueAlbumCount
            )

            if (snapshot.trackEntries.isEmpty()) {
                StatsEmptyStateM3()
            } else {
                Text(
                    text = strings.statsTopTracks,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatsTrackListM3(snapshot.trackEntries.take(10), tracks)
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun StatsPeriodPillsM3(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    val strings = LocalStrings.current
    val periods = listOf(
        StatsPeriod.Today to strings.statsPeriodToday,
        StatsPeriod.ThisWeek to strings.statsPeriodThisWeek,
        StatsPeriod.ThisMonth to strings.statsPeriodThisMonth,
        StatsPeriod.ThisYear to strings.statsPeriodThisYear,
        StatsPeriod.AllTime to strings.statsPeriodAllTime
    )
    val rowScrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rowScrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { (period, label) ->
            val selected = period == selectedPeriod
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable { onPeriodSelected(period) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun StatsSummaryCardM3(totalListenedMs: Long, playCount: Int, uniqueAlbumCount: Int) {
    val formattedTime = formatListeningTimeM3(totalListenedMs, LocalStrings.current)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatsTrackListM3(entries: List<StatsTrackEntry>, tracks: List<AudioTrack>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.heightIn(max = 2400.dp),
        userScrollEnabled = false
    ) {
        itemsIndexed(entries, key = { _, e -> e.trackId }) { index, entry ->
            val track = remember(entry.trackId, tracks) {
                tracks.firstOrNull { it.id == entry.trackId }
                    ?: tracks.firstOrNull { it.title == entry.title && it.artist == entry.artistName }
            }
            val coverUri = track?.albumArtUri ?: entry.albumArtUri
            val displayTitle = track?.title?.takeIf { it.isNotBlank() } ?: entry.title
            val displayArtist = track?.artist?.takeIf { it.isNotBlank() } ?: entry.artistName

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        var loaded by remember(coverUri) { mutableStateOf(false) }
                        AsyncImage(
                            model = coverUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            onState = { loaded = it is AsyncImagePainter.State.Success }
                        )
                        if (!loaded) {
                            Icon(
                                Icons.Filled.MusicNote,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = displayArtist,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatListeningTimeM3(entry.listenedMs, LocalStrings.current),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsEmptyStateM3() {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = strings.statsEmptyTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = strings.statsEmptySubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatListeningTimeM3(
    ms: Long,
    strings: dev.shephard.player.ui.i18n.Strings,
): String {
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
