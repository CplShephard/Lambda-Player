package dev.shephard.player.ui.screens

import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LibraryViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.data.ListenStatsCalculator
import dev.shephard.player.data.StatsPeriod
import dev.shephard.player.data.StatsTrackEntry
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.captureForTopBarBlur
import dev.shephard.player.ui.components.rememberCollapsingTopBarState
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.miuixBlurSurface
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * Flamingo Player'daki "Stats" özelliğinden esinlenilen dinleme istatistikleri sayfası.
 * Settings > Total Listening Time kartına basınca açılır (bkz. NavGraph.kt StatsRoute).
 *
 * Flamingo'nun UI kodu (Title/YosWrapper/ProfileButton, MMKV+Gson depolama) doğrudan
 * kopyalanmadı — konsept (dönem pill'leri, özet kartı, en çok dinlenen sanatçı/albüm/şarkı
 * yatay listeleri) alınıp Lambda Player'ın Miuix tasarım diline ve mevcut altyapısına
 * (DataStore, PlayerViewModel.statsEventsFlow) uyarlandı.
 */
@Composable
fun StatsScreen(
    playerViewModel: PlayerViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val cs = MiuixAppTheme.colorScheme
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val topBarState = rememberCollapsingTopBarState()

    val allEvents by playerViewModel.statsEventsFlow.collectAsState()
    val tracks by libraryViewModel.tracks.collectAsState()
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.Today) }

    val snapshot = remember(allEvents, selectedPeriod) {
        val periodEvents = ListenStatsCalculator.filterEventsForPeriod(allEvents, selectedPeriod)
        ListenStatsCalculator.buildSnapshot(periodEvents)
    }

    val collapseRangePx = with(density) { 44.dp.toPx() }
    val scrollProgress by remember {
        derivedStateOf { (scrollState.value / collapseRangePx).coerceIn(0f, 1f) }
    }

    Column(modifier = Modifier.fillMaxSize().background(cs.background)) {
        SmallTopAppBar(
            title = strings.statsTitle,
            modifier = if (topBarState.pageBackdrop != null) {
                Modifier.miuixBlurSurface(
                    backdrop = topBarState.pageBackdrop,
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    blurRadius = 70f,
                    tintAlpha = if (scrollProgress > 0.01f) (0.68f + scrollProgress * 0.27f).coerceIn(0f, 0.95f) else 0f,
                    fallbackColor = Color.Transparent
                )
            } else Modifier,
            color = if (topBarState.pageBackdrop != null) Color.Transparent else cs.background.copy(alpha = scrollProgress),
            titleColor = cs.onBackground.copy(alpha = scrollProgress),
            scrollBehavior = topBarState.scrollBehavior,
            defaultWindowInsetsPadding = false,
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceVariant.copy(alpha = 0.75f))
                        .bounceClick { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription, tint = cs.onBackground)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(topBarState.pageBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StatsPeriodPills(selectedPeriod = selectedPeriod, onPeriodSelected = { selectedPeriod = it })

            StatsSummaryCard(
                totalListenedMs = snapshot.summary.totalListenedMs,
                playCount = snapshot.summary.playCount,
                uniqueAlbumCount = snapshot.summary.uniqueAlbumCount
            )

            if (snapshot.trackEntries.isEmpty()) {
                StatsEmptyState()
            } else {
                StatsSectionHeader(strings.statsTopTracks)
                StatsTrackList(snapshot.trackEntries.take(10), tracks)
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}

@Composable
private fun StatsPeriodPills(
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
            val cs = MiuixAppTheme.colorScheme
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) cs.primary else cs.surfaceVariant.copy(alpha = 0.6f))
                    .clickable { onPeriodSelected(period) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = if (selected) cs.onPrimary else cs.onSurfaceVariant,
                    style = MiuixAppTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun StatsSummaryCard(totalListenedMs: Long, playCount: Int, uniqueAlbumCount: Int) {
    val strings = LocalStrings.current
    val cs = MiuixAppTheme.colorScheme
    val formattedTime = formatStatsMinutes(totalListenedMs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.5f))
            .padding(20.dp)
    ) {
        Text(
            text = formattedTime,
            style = MiuixAppTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onBackground
        )
    }
}

@Composable
private fun StatsSectionHeader(title: String) {
    Text(
        text = title,
        style = MiuixAppTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MiuixAppTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun StatsTrackList(entries: List<StatsTrackEntry>, tracks: List<AudioTrack>) {
    val cs = MiuixAppTheme.colorScheme
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.heightIn(max = 2400.dp),
        userScrollEnabled = false
    ) {
        itemsIndexed(entries, key = { _, e -> e.trackId }) { index, entry ->
            val track = remember(entry.trackId, tracks) {
                tracks.firstOrNull { it.id == entry.trackId }
                    ?: tracks.firstOrNull { it.title == entry.title && it.artist == entry.artistName }
            }
            val coverUri = track?.albumArtUri ?: entry.albumArtUri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MiuixAppTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cs.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    var loaded by remember(coverUri) { mutableStateOf(false) }
                    AsyncImage(
                        model = coverUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        onState = { loaded = it is coil.compose.AsyncImagePainter.State.Success }
                    )
                    if (!loaded) {
                        Icon(Icons.Filled.MusicNote, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MiuixAppTheme.typography.bodyMedium,
                        color = cs.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.artistName,
                        style = MiuixAppTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatStatsMinutes(entry.listenedMs),
                    style = MiuixAppTheme.typography.labelMedium,
                    color = cs.primary
                )
            }
        }
    }
}

@Composable
private fun StatsEmptyState() {
    val strings = LocalStrings.current
    val cs = MiuixAppTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = cs.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = strings.statsEmptyTitle,
            style = MiuixAppTheme.typography.titleMedium,
            color = cs.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = strings.statsEmptySubtitle,
            style = MiuixAppTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )
    }
}

private fun formatStatsMinutes(listenedMs: Long): String {
    val totalSeconds = listenedMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return buildString {
        if (hours > 0) append("${hours}h ")
        if (minutes > 0 || hours > 0) append("${minutes}m ")
        append("${seconds}s")
    }
}
