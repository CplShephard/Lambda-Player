// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 - Subpages with M3 padding/radius + solid background
package dev.shephard.player.ui.screens.m3

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import dev.shephard.player.ui.components.comradeEasterEggTrigger
import dev.shephard.player.ui.components.m3.M3SmallTopBar
import dev.shephard.player.ui.components.m3.DropDownMenuWidget
import dev.shephard.player.player.LyricsAutoSearchMode
import dev.shephard.player.player.VolumeNormalization
import dev.shephard.player.ui.screens.lyricsAutoSearchLabel
import dev.shephard.player.ui.screens.volumeNormalizationLabel
import dev.shephard.player.ui.components.m3.m3TopBarColors
import dev.shephard.player.ui.components.m3.m3ItemCardColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.twotone.LinearScale
import androidx.compose.material.icons.twotone.SpeakerGroup
import androidx.compose.material.icons.twotone.SwapHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.R
import dev.shephard.player.data.ListenStatsCalculator
import dev.shephard.player.data.StatsPeriod
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.m3.LineageListItem
import dev.shephard.player.ui.components.m3.LineageListItemWithThumbnail
import dev.shephard.player.ui.components.m3.LineageSectionHeader
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.components.m3.SwitchWidget
import dev.shephard.player.ui.screens.AppLicenses
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
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            M3SmallTopBar(
                title = { Text(strings.settings) },
                colors = m3TopBarColors(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 80.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    onClick = onOpenStats
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(strings.totalListeningTime, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(8.dp))
                        Text(formatListeningTimeM3Lineage(totalMs, strings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Text(strings.statsTopTracks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
            item { LineageSectionHeader(title = strings.settings) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = m3ItemCardColors()
                ) {
                    LineageListItem(
                        headline = strings.themeSettings,
                        supporting = strings.themeSettingsSummary,
                        leadingContent = { Icon(Icons.Filled.ColorLens, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = onOpenThemeSettings
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = m3ItemCardColors()
                ) {
                    LineageListItem(
                        headline = strings.playbackSettings,
                        supporting = strings.playbackSettingsSummary,
                        leadingContent = { Icon(Icons.Filled.Headphones, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = onOpenPlayerSettings
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = m3ItemCardColors()
                ) {
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
    val lyricsAutoSearch by prefs.lyricsAutoSearchMode.collectAsState(initial = LyricsAutoSearchMode.OFF)
    val volumeNormalization by prefs.volumeNormalization.collectAsState(initial = VolumeNormalization.NONE)

            Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            M3SmallTopBar(
                title = { Text(strings.playbackSettings) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription) }
                },
                colors = m3TopBarColors(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 80.dp)
        ) {
            item {
                SegmentedColumn(title = strings.playbackSettings) {
                    item {
                        SwitchWidget(
                            icon = Icons.TwoTone.SwapHoriz,
                            title = strings.crossfade,
                            checked = crossfade,
                            onCheckedChange = { scope.launch { prefs.setCrossfadeEnabled(it) } }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = Icons.TwoTone.LinearScale,
                            title = strings.gapless,
                            checked = gapless,
                            onCheckedChange = { scope.launch { prefs.setGaplessEnabled(it) } }
                        )
                    }
                    item {
                        val lyricsModes = LyricsAutoSearchMode.entries
                        DropDownMenuWidget(
                            icon = Icons.Filled.Subtitles,
                            title = strings.lyricsAutoSearchTitle,
                            description = strings.lyricsAutoSearchDescription,
                            choice = lyricsModes.indexOf(lyricsAutoSearch),
                            data = lyricsModes.map { lyricsAutoSearchLabel(it, strings) },
                            onChoiceChange = { idx -> scope.launch { prefs.setLyricsAutoSearchMode(lyricsModes[idx]) } }
                        )
                    }
                    item {
                        val normalizationModes = VolumeNormalization.entries
                        DropDownMenuWidget(
                            icon = Icons.Filled.GraphicEq,
                            title = strings.volumeNormalizationTitle,
                            description = strings.volumeNormalizationDescription,
                            choice = normalizationModes.indexOf(volumeNormalization),
                            data = normalizationModes.map { volumeNormalizationLabel(it, strings) },
                            onChoiceChange = { idx -> scope.launch { prefs.setVolumeNormalization(normalizationModes[idx]) } }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = Icons.TwoTone.SpeakerGroup,
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
    var showLicenses by remember { mutableStateOf(false) }
    if (showLicenses) LicensesDialogM3(onDismiss = { showLicenses = false })
    var showComradeEasterEgg by remember { mutableStateOf(false) }
    if (showComradeEasterEgg) {
        dev.shephard.player.ui.components.ComradeEasterEggDialog(useMiuix = false, onDismiss = { showComradeEasterEgg = false })
    }
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
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                M3SmallTopBar(
                    title = { Text(strings.aboutSectionTitle) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription) } },
                    colors = m3TopBarColors(),
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
            // Hidden pop-up: tap the icon quickly several times (Chinese UI only).
            Box(modifier = Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)).comradeEasterEggTrigger { showComradeEasterEgg = true }, contentAlignment = Alignment.Center) {
                val bitmap = appIconBitmap
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = strings.appName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Image(painter = painterResource(id = R.drawable.ic_launcher_preview), contentDescription = strings.appName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(strings.appName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${strings.version} $versionName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))

            LineageSectionHeader(title = strings.aboutSectionTitle, modifier = Modifier.fillMaxWidth())
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(20.dp),
                colors = m3ItemCardColors()
            ) {
                LineageListItem(
                    headline = strings.github,
                    supporting = "CplShephard",
                    leadingContent = { Icon(Icons.Filled.Code, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { uriHandler.openUri("https://github.com/CplShephard") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(20.dp),
                colors = m3ItemCardColors()
            ) {
                LineageListItem(
                    headline = strings.sourceCode,
                    supporting = "github.com/CplShephard/Lambda-Player",
                    leadingContent = { Icon(Icons.Filled.Info, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { uriHandler.openUri("https://github.com/CplShephard/Lambda-Player") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(20.dp),
                colors = m3ItemCardColors()
            ) {
                LineageListItem(
                    headline = strings.licensesTitle,
                    supporting = strings.licensesSummary,
                    leadingContent = { Icon(Icons.Filled.Description, null) },
                    onClick = { showLicenses = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)
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
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                M3SmallTopBar(
                    title = { Text(strings.statsTitle) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription) } },
                    colors = m3TopBarColors(),
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
            // Sliding animation like Miuix for Today/ThisMonth filters
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                val periods = listOf(StatsPeriod.Today to strings.statsPeriodToday, StatsPeriod.ThisWeek to strings.statsPeriodThisWeek, StatsPeriod.ThisMonth to strings.statsPeriodThisMonth, StatsPeriod.AllTime to strings.statsPeriodAllTime)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(periods.size) { idx ->
                        val (period, label) = periods[idx]
                        androidx.compose.foundation.layout.Box(modifier = Modifier.animateItem(
                            fadeInSpec = androidx.compose.animation.core.tween(250),
                            fadeOutSpec = androidx.compose.animation.core.tween(200),
                            placementSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 380f)
                        )) {
                            androidx.compose.material3.FilterChip(
                                selected = period == selectedPeriod,
                                onClick = { selectedPeriod = period },
                                label = { Text(label) }
                            )
                        }
                    }
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
                    androidx.compose.animation.AnimatedContent(
                        // The ranking animation is keyed only by song identity, not listenedMs.
                        // Timer ticks update the duration text without replaying the slide.
                        targetState = entry.trackId,
                        transitionSpec = {
                            (androidx.compose.animation.slideInVertically(
                                animationSpec = androidx.compose.animation.core.tween(340),
                            ) { height -> height / 3 } +
                                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)))
                                .togetherWith(
                                    androidx.compose.animation.slideOutVertically(
                                        animationSpec = androidx.compose.animation.core.tween(260),
                                    ) { height -> -height / 3 } +
                                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180))
                                )
                        },
                        label = "m3StatsRankSlide$index",
                    ) { animatedTrackId ->
                        val animatedEntry = snapshot.trackEntries.firstOrNull { it.trackId == animatedTrackId } ?: entry
                        val track = remember(animatedTrackId, tracks) {
                            tracks.firstOrNull { it.id == animatedTrackId }
                                ?: tracks.firstOrNull { it.title == animatedEntry.title && it.artist == animatedEntry.artistName }
                        }
                        val coverUri = track?.albumArtUri ?: animatedEntry.albumArtUri
                        val displayTitle = track?.title?.takeIf { it.isNotBlank() } ?: animatedEntry.title
                        val displayArtist = track?.artist?.takeIf { it.isNotBlank() } ?: animatedEntry.artistName

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = m3ItemCardColors()
                        ) {
                            LineageListItemWithThumbnail(
                                headline = "${index + 1}. $displayTitle",
                                supporting = "$displayArtist • ${formatListeningTimeM3Lineage(animatedEntry.listenedMs, strings)}",
                                thumbnailModel = coverUri,
                                placeholderIcon = Icons.Filled.MusicNote
                            )
                        }
                    }
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

@Composable
private fun LicensesDialogM3(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.licensesTitle) },
        text = {
            LazyColumn {
                items(AppLicenses) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { uriHandler.openUri(entry.url) }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Text(entry.name, style = MaterialTheme.typography.titleSmall)
                        Text(entry.license, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text(entry.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
    )
}
