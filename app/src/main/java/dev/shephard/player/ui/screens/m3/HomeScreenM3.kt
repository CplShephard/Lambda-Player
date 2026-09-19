// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 Material 3 Expressive - Home Screen
package dev.shephard.player.ui.screens.m3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.m3.LineageHorizontalMediaItem
import dev.shephard.player.ui.components.m3.LineageListItemWithThumbnail
import dev.shephard.player.ui.components.m3.LineageNoElements
import dev.shephard.player.ui.components.m3.LineageSectionHeader
import dev.shephard.player.ui.glass.LocalWallpaperEnabled
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.navigation.PageTransitions
import dev.shephard.player.ui.navigation.SubmenuNavGuard
import dev.shephard.player.ui.screens.LocalPlaylist
import dev.shephard.player.ui.screens.encodePlaylists
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
    val recentEvents by playerViewModel.statsEventsFlow.collectAsState()

    val featuredTracks = remember(tracks) {
        if (tracks.isNotEmpty()) tracks.shuffled().take(10) else emptyList()
    }

    val recentlyPlayedTracks = remember(recentEvents, tracks) {
        val matched = recentEvents
            .sortedByDescending { it.timestampMs }
            .mapNotNull { ev ->
                tracks.firstOrNull { it.id == ev.trackId }
                    ?: tracks.firstOrNull { it.title == ev.title && it.artist == ev.artist }
            }
            .distinctBy { it.id }
        if (matched.isNotEmpty()) matched.take(15) else tracks.take(15)
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
        }.shuffled().take(10)
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
                    topBar = {
                        TopAppBar(
                            title = { Text(strings.home) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
                                titleContentColor = wallpaperAdaptiveTextColor(fallback = MaterialTheme.colorScheme.onSurface),
                            ),
                        )
                    },
                ) { padding ->
                    if (tracks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            LineageNoElements(
                                icon = Icons.Filled.MusicNote,
                                message = strings.noSongsToPlay
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding() + 8.dp,
                                bottom = if (hasMiniPlayer) 160.dp else 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (featuredTracks.isNotEmpty()) {
                                item {
                                    LineageSectionHeader(title = strings.featuredSongs)
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(featuredTracks, key = { it.id }) { track ->
                                            LineageHorizontalMediaItem(
                                                headline = track.title,
                                                subhead = track.artist,
                                                thumbnailModel = track.albumArtUri,
                                                placeholderIcon = Icons.Filled.MusicNote,
                                                onClick = {
                                                    val idx = featuredTracks.indexOf(track)
                                                    if (idx >= 0) onTrackClick(featuredTracks, idx, strings.featuredSongs)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (featuredPlaylists.isNotEmpty()) {
                                item {
                                    LineageSectionHeader(title = strings.featuredPlaylists)
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(featuredPlaylists, key = { it.name + it.createdAt }) { pl ->
                                            LineageHorizontalMediaItem(
                                                headline = pl.name,
                                                subhead = "${pl.trackIds.size} tracks",
                                                thumbnailModel = pl.coverUri,
                                                placeholderIcon = Icons.Filled.QueueMusic,
                                                onClick = {
                                                    val i = featuredPlaylists.indexOf(pl)
                                                    if (i >= 0) playlistDetailGuard.push(openPlaylistIndex, i) { openPlaylistIndex = i }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (recentlyPlayedTracks.isNotEmpty()) {
                                item {
                                    LineageSectionHeader(title = strings.recentlyPlayed)
                                }
                                items(recentlyPlayedTracks, key = { it.id }) { track ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        LineageListItemWithThumbnail(
                                            headline = track.title,
                                            supporting = "${track.artist} • ${track.album}",
                                            thumbnailModel = track.albumArtUri,
                                            placeholderIcon = Icons.Filled.MusicNote,
                                            onClick = {
                                                val idx = recentlyPlayedTracks.indexOf(track)
                                                if (idx >= 0) onTrackClick(recentlyPlayedTracks, idx, strings.recentlyPlayed)
                                            }
                                        )
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
