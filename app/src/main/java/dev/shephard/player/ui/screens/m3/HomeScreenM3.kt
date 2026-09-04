// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.screens.m3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.navigation.PageTransitions
import dev.shephard.player.ui.navigation.SubmenuNavGuard
import dev.shephard.player.ui.screens.LocalPlaylist
import dev.shephard.player.ui.screens.encodePlaylists
import dev.shephard.player.ui.screens.ensureLikedSongsPlaylist
import dev.shephard.player.ui.screens.parsePlaylists
import dev.shephard.player.ui.screens.resolvePlaylistTracks
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenM3(
    libraryViewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> }
) {
    val strings = LocalStrings.current
    val tracks by libraryViewModel.tracks.collectAsState()
    val isLoading by libraryViewModel.isLoading.collectAsState()
    val recentEvents by playerViewModel.statsEventsFlow.collectAsState()

    val featuredTracks = remember(tracks) {
        if (tracks.isNotEmpty()) tracks.shuffled().take(5) else emptyList()
    }

    val recentlyPlayedTracks = remember(recentEvents, tracks) {
        val matched = recentEvents
            .sortedByDescending { it.timestampMs }
            .mapNotNull { ev ->
                tracks.firstOrNull { it.id == ev.trackId }
                    ?: tracks.firstOrNull { it.title == ev.title && it.artist == ev.artist }
            }
            .distinctBy { it.id }
        if (matched.isNotEmpty()) matched.take(10) else tracks.take(10)
    }

    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val rawPlaylistsJson by prefs.playlistsJson.collectAsState(initial = "[]")
    val rawPlaylists = remember(rawPlaylistsJson) { parsePlaylists(rawPlaylistsJson) }
    val likedSongIdsJson by prefs.likedSongIds.collectAsState(initial = "[]")
    val likedIds = remember(likedSongIdsJson) {
        try {
            org.json.JSONArray(likedSongIdsJson).let { arr -> (0 until arr.length()).map { arr.getLong(it) } }
        } catch (_: Exception) { emptyList() }
    }
    val featuredPlaylists = remember(rawPlaylists, strings) {
        rawPlaylists.filter {
            !it.isSystem &&
                it.name != strings.likedSongs &&
                it.name != "Liked Songs" &&
                it.name != "Beğenilenler" &&
                it.name != "Favoriler"
        }.shuffled().take(5)
    }

    var openPlaylistIndex by remember { mutableStateOf<Int?>(null) }
    val playlistDetailGuard = remember { SubmenuNavGuard() }

    BackHandler(enabled = openPlaylistIndex != null) {
        playlistDetailGuard.pop { openPlaylistIndex = null }
    }

    AnimatedContent(
        targetState = openPlaylistIndex,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (targetState != null) {
                androidx.compose.animation.ContentTransform(
                    targetContentEnter = PageTransitions.m3EnterSubmenu,
                    initialContentExit = PageTransitions.m3ExitSubmenu,
                    targetContentZIndex = 1f
                )
            } else {
                androidx.compose.animation.ContentTransform(
                    targetContentEnter = PageTransitions.m3PopEnterSubmenu,
                    initialContentExit = PageTransitions.m3PopExitSubmenu,
                    targetContentZIndex = 0f
                )
            }
        },
        label = "homeM3PlaylistSubmenu"
    ) { idx ->
        Box(modifier = Modifier.fillMaxSize()) {
            val selectedPl = idx?.let { featuredPlaylists.getOrNull(it) }
            if (selectedPl != null) {
                val plTracks = remember(selectedPl, tracks, likedIds) { resolvePlaylistTracks(selectedPl, tracks, likedIds) }
                M3PlaylistDetail(
                    playlist = selectedPl,
                    allTracks = tracks,
                    plTracks = plTracks,
                    strings = strings,
                    onBack = { playlistDetailGuard.pop { openPlaylistIndex = null } },
                    onTrackClick = { list, i -> onTrackClick(list, i, selectedPl.name) },
                    onPlayAll = { if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0, selectedPl.name) },
                    onPlayRemix = { if (plTracks.isNotEmpty()) onTrackClick(plTracks.shuffled(), 0, selectedPl.name) },
                    onRemoveTrack = { trackId ->
                        val newTrackIds = selectedPl.trackIds - trackId
                        val rawIdx = rawPlaylists.indexOf(selectedPl)
                        if (rawIdx >= 0) {
                            val next = rawPlaylists.toMutableList()
                            next[rawIdx] = selectedPl.copy(trackIds = newTrackIds)
                            scope.launch { prefs.setPlaylistsJson(encodePlaylists(next)) }
                        }
                    },
                    onAddTracks = {},
                    onPickCover = {},
                    onRename = {},
                    onDelete = {},
                )
            } else {
                M3HomeRoot(
                    strings = strings,
                    isLoading = isLoading,
                    tracks = tracks,
                    hasMiniPlayer = hasMiniPlayer,
                    featuredTracks = featuredTracks,
                    featuredPlaylists = featuredPlaylists,
                    recentlyPlayedTracks = recentlyPlayedTracks,
                    onTrackClick = onTrackClick,
                    onPlaylistClick = { pl ->
                        val i = featuredPlaylists.indexOf(pl)
                        if (i >= 0) playlistDetailGuard.push(openPlaylistIndex, i) { openPlaylistIndex = i }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3HomeRoot(
    strings: dev.shephard.player.ui.i18n.Strings,
    isLoading: Boolean,
    tracks: List<AudioTrack>,
    hasMiniPlayer: Boolean,
    featuredTracks: List<AudioTrack>,
    featuredPlaylists: List<LocalPlaylist>,
    recentlyPlayedTracks: List<AudioTrack>,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit,
    onPlaylistClick: (LocalPlaylist) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(strings.home) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        if (tracks.isEmpty()) {
            M3HomeEmptyState(strings)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = if (hasMiniPlayer) 176.dp else 96.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (featuredTracks.isNotEmpty()) {
                    M3FeaturedSongsSection(
                        title = strings.featuredSongs,
                        tracks = featuredTracks,
                        onTrackClick = { featuredIdx ->
                            onTrackClick(featuredTracks, featuredIdx, strings.featuredSongs)
                        }
                    )
                }
                if (featuredPlaylists.isNotEmpty()) {
                    M3FeaturedPlaylistsSection(
                        title = strings.featuredPlaylists,
                        playlists = featuredPlaylists,
                        onPlaylistClick = onPlaylistClick,
                    )
                }
                if (recentlyPlayedTracks.isNotEmpty()) {
                    M3RecentlyPlayedSection(
                        title = strings.recentlyPlayed,
                        tracks = recentlyPlayedTracks,
                        onTrackClick = { recentIdx ->
                            onTrackClick(recentlyPlayedTracks, recentIdx, strings.recentlyPlayed)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun M3HomeEmptyState(strings: dev.shephard.player.ui.i18n.Strings) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = strings.noSongsToPlay,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = strings.pickASong,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun M3FeaturedSongsSection(
    title: String,
    tracks: List<AudioTrack>,
    onTrackClick: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tracks.size })
    Column(modifier = Modifier.fillMaxWidth()) {
        M3SectionTitle(title)
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(268.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 60.dp),
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val track = tracks[page]
            M3FeaturedSongCard(track = track, onClick = { onTrackClick(page) })
        }
    }
}

@Composable
private fun M3FeaturedSongCard(track: AudioTrack, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(268.dp).aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (track.albumArtUri != null) {
                AsyncImage(
                    model = track.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                        RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    )
                    .padding(16.dp),
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun M3RecentlyPlayedSection(
    title: String,
    tracks: List<AudioTrack>,
    onTrackClick: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tracks.size })
    Column(modifier = Modifier.fillMaxWidth()) {
        M3SectionTitle(title)
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(268.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 60.dp),
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val track = tracks[page]
            AnimatedContent(
                targetState = track,
                transitionSpec = {
                    (fadeIn(tween(300)) +
                        scaleIn(
                            initialScale = 0.82f,
                            animationSpec = spring(dampingRatio = 0.52f, stiffness = 340f)
                        )
                    ).togetherWith(
                        fadeOut(tween(120)) + scaleOut(
                            targetScale = 1.05f,
                            animationSpec = spring(dampingRatio = 0.52f, stiffness = 340f)
                        )
                    )
                },
                label = "recentM3",
            ) { currentTrack ->
                M3FeaturedSongCard(track = currentTrack, onClick = { onTrackClick(page) })
            }
        }
    }
}

@Composable
private fun M3FeaturedPlaylistsSection(
    title: String,
    playlists: List<LocalPlaylist>,
    onPlaylistClick: (LocalPlaylist) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { playlists.size })
    Column(modifier = Modifier.fillMaxWidth()) {
        M3SectionTitle(title)
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(172.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 60.dp),
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val pl = playlists[page]
            M3FeaturedPlaylistCard(playlist = pl, onClick = { onPlaylistClick(pl) })
        }
    }
}

@Composable
private fun M3FeaturedPlaylistCard(playlist: LocalPlaylist, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(172.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (playlist.coverUri != null) {
                    AsyncImage(
                        model = playlist.coverUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.trackIds.size} ${LocalStrings.current.trackCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun M3SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
