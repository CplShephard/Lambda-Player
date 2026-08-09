// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.components.InstallerXTopBar
import dev.shephard.player.ui.components.captureForTopBarBlur
import dev.shephard.player.ui.components.rememberCollapsingTopBarState
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import top.yukonga.miuix.kmp.basic.Scaffold

/**
 * Flamingo Player'daki Home.kt sayfasından (RecommendCard - Featured Songs ve RecentlyPlayedCard
 * - Recently Played / Viewed) esinlenerek Lambda Player'a eklenen ana sayfa.
 *
 * Miuix / InstallerX tasarım diline, SmallTopAppBar (blur topbar / collapsing top bar),
 * ve projenin yerel dinleme istatistiklerine (statsEventsFlow) entegre edilmiştir.
 */
@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> }
) {
    val strings = LocalStrings.current
    val tracks by libraryViewModel.tracks.collectAsState()
    val recentEvents by playerViewModel.statsEventsFlow.collectAsState()

    LaunchedEffect(Unit) {
        if (tracks.isEmpty()) {
            libraryViewModel.loadTracks()
        }
    }

    val featuredTracks = remember(tracks) {
        if (tracks.isNotEmpty()) {
            tracks.shuffled().take(5)
        } else {
            emptyList()
        }
    }

    val recentlyPlayedTracks = remember(recentEvents, tracks) {
        val recentIds = recentEvents
            .sortedByDescending { it.timestampMs }
            .map { it.trackId }
            .distinct()
        val matched = recentIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
        if (matched.isNotEmpty()) {
            matched.take(10)
        } else {
            tracks.take(10)
        }
    }

    val topBarState = rememberCollapsingTopBarState()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            InstallerXTopBar(
                title = strings.home,
                state = topBarState
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .captureForTopBarBlur(topBarState)
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = if (hasMiniPlayer) 176.dp else 96.dp
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Featured Songs Section (RecommendCard)
            if (featuredTracks.isNotEmpty()) {
                FeaturedSongsSection(
                    title = strings.featuredSongs,
                    tracks = featuredTracks,
                    onTrackClick = { idx ->
                        onTrackClick(featuredTracks, idx, strings.featuredSongs)
                    }
                )
            }

            // Recently Played / Viewed Section (RecentlyPlayedCard)
            if (recentlyPlayedTracks.isNotEmpty()) {
                RecentlyPlayedSection(
                    title = strings.recentlyPlayed,
                    tracks = recentlyPlayedTracks,
                    onTrackClick = { idx ->
                        onTrackClick(recentlyPlayedTracks, idx, strings.recentlyPlayed)
                    }
                )
            }
        }
    }
}

@Composable
private fun FeaturedSongsSection(
    title: String,
    tracks: List<AudioTrack>,
    onTrackClick: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tracks.size })

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MiuixAppTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MiuixAppTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(268.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 60.dp),
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val track = tracks[page]
            FeaturedSongCard(track = track, onClick = { onTrackClick(page) })
        }
    }
}

@Composable
private fun FeaturedSongCard(
    track: AudioTrack,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(268.dp)
            .aspectRatio(1f)
            .shadow(10.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(MiuixAppTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        var loaded by remember(track.id) { mutableStateOf(false) }
        AsyncImage(
            model = track.albumArtUri,
            contentDescription = track.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { loaded = true }
        )
        if (!loaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MiuixAppTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 150f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    title: String,
    tracks: List<AudioTrack>,
    onTrackClick: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tracks.size })

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MiuixAppTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MiuixAppTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(268.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 60.dp),
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val track = tracks[page]
            RecentlyPlayedCard(track = track, onClick = { onTrackClick(page) })
        }
    }
}

@Composable
private fun RecentlyPlayedCard(
    track: AudioTrack,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(268.dp)
            .height(200.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixAppTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        var loaded by remember(track.id) { mutableStateOf(false) }
        AsyncImage(
            model = track.albumArtUri,
            contentDescription = track.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { loaded = true }
        )
        if (!loaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MiuixAppTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 100f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${track.artist} • ${track.album}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
