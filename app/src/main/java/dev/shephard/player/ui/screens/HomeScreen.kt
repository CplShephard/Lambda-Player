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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dev.shephard.player.ui.components.overScrollVertical
import dev.shephard.player.ui.navigation.SubmenuNavGuard
import dev.shephard.player.ui.navigation.PageTransitions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import dev.shephard.player.player.PreferencesManager
import kotlinx.coroutines.launch
import dev.shephard.player.ui.components.MiuixTopBar
import dev.shephard.player.ui.components.captureForTopBarBlur
import dev.shephard.player.ui.components.rememberCollapsingTopBarState
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import top.yukonga.miuix.kmp.basic.Scaffold

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
        val matched = recentEvents
            .sortedByDescending { it.timestampMs }
            .mapNotNull { ev ->
                tracks.firstOrNull { it.id == ev.trackId }
                    ?: tracks.firstOrNull { it.title == ev.title && it.artist == ev.artist }
            }
            .distinctBy { it.id }
        if (matched.isNotEmpty()) {
            matched.take(10)
        } else {
            tracks.take(10)
        }
    }

    val topBarState = rememberCollapsingTopBarState()
    val scrollState = rememberScrollState()

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
                    targetContentEnter = PageTransitions.enterSubmenu,
                    initialContentExit = PageTransitions.exitSubmenu,
                    targetContentZIndex = 1f
                )
            } else {
                androidx.compose.animation.ContentTransform(
                    targetContentEnter = PageTransitions.popEnterSubmenu,
                    initialContentExit = PageTransitions.popExitSubmenu,
                    targetContentZIndex = 0f
                )
            }
        },
        label = "homePlaylistSubmenu"
    ) { idx ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val selectedPl = idx?.let { featuredPlaylists.getOrNull(it) }
            if (selectedPl != null) {
                val plTracks = remember(selectedPl, tracks, likedIds) { resolvePlaylistTracks(selectedPl, tracks, likedIds) }
                PlaylistDetailView(
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
                    onAddTracks = { },
                    onPickCover = { },
                    onReorder = { },
                    onChangeSort = { }
                )
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        MiuixTopBar(
                            title = strings.home,
                            state = topBarState
                        )
                    }
                ) { innerPadding ->
                    if (tracks.isEmpty()) {
                        HomeEmptyState()
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .captureForTopBarBlur(topBarState)
                                .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                                .overScrollVertical()
                                .verticalScroll(scrollState)
                                .padding(
                                    top = innerPadding.calculateTopPadding() + 8.dp,
                                    bottom = if (hasMiniPlayer) 176.dp else 96.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            if (featuredTracks.isNotEmpty()) {
                                FeaturedSongsSection(
                                    title = strings.featuredSongs,
                                    tracks = featuredTracks,
                                    onTrackClick = { featuredIdx ->
                                        onTrackClick(featuredTracks, featuredIdx, strings.featuredSongs)
                                    }
                                )
                            }

                            if (featuredPlaylists.isNotEmpty()) {
                                FeaturedPlaylistsSection(
                                    title = strings.featuredPlaylists,
                                    playlists = featuredPlaylists,
                                    onPlaylistClick = { pl ->
                                        val i = featuredPlaylists.indexOf(pl)
                                        if (i >= 0) {
                                            playlistDetailGuard.push(openPlaylistIndex, i) { openPlaylistIndex = i }
                                        }
                                    }
                                )
                            }

                            if (recentlyPlayedTracks.isNotEmpty()) {
                                RecentlyPlayedSection(
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
        }
    }
}

@Composable
private fun HomeEmptyState() {
    val strings = LocalStrings.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MiuixAppTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = strings.noSongsToPlay,
                style = MiuixAppTheme.typography.titleLarge,
                color = wallpaperAdaptiveTextColor(),
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = strings.emptyLibraryHint,
                style = MiuixAppTheme.typography.bodyMedium,
                color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
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
            color = wallpaperAdaptiveTextColor(),
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
            onState = { loaded = it is coil.compose.AsyncImagePainter.State.Success }
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
            color = wallpaperAdaptiveTextColor(),
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
            if (page == 0) {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        (fadeIn(androidx.compose.animation.core.tween(300)) +
                            scaleIn(
                                initialScale = 0.82f,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = 0.52f,
                                    stiffness = 340f
                                )
                            )).togetherWith(
                            fadeOut(androidx.compose.animation.core.tween(200)) +
                                scaleOut(targetScale = 0.82f)
                        )
                    },
                    label = "recentTrackBounceFade"
                ) { targetTrack ->
                    RecentlyPlayedCard(track = targetTrack, onClick = { onTrackClick(0) })
                }
            } else {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        (fadeIn(androidx.compose.animation.core.tween(300)) +
                            androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(350)) { -it / 3 }).togetherWith(
                            fadeOut(androidx.compose.animation.core.tween(200)) +
                                androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(350)) { it / 3 }
                        )
                    },
                    label = "recentTrackSlideRight"
                ) { targetTrack ->
                    RecentlyPlayedCard(track = targetTrack, onClick = { onTrackClick(page) })
                }
            }
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
            .aspectRatio(1f)
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
            onState = { loaded = it is coil.compose.AsyncImagePainter.State.Success }
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

@Composable
private fun FeaturedPlaylistsSection(
    title: String,
    playlists: List<LocalPlaylist>,
    onPlaylistClick: (LocalPlaylist) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { playlists.size })

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MiuixAppTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = wallpaperAdaptiveTextColor(),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(172.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 60.dp),
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val pl = playlists[page]
            FeaturedPlaylistCard(playlist = pl, onClick = { onPlaylistClick(pl) })
        }
    }
}

@Composable
private fun FeaturedPlaylistCard(
    playlist: LocalPlaylist,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(172.dp)
            .aspectRatio(1f)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(MiuixAppTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        val coverUri = playlist.coverUri?.let { android.net.Uri.parse(it) }
        var artLoaded by remember(coverUri) { mutableStateOf(false) }
        if (coverUri != null) {
            AsyncImage(
                model = coverUri,
                contentDescription = playlist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { artLoaded = it is coil.compose.AsyncImagePainter.State.Success }
            )
        }
        if (!artLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LibraryMusic,
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
        Text(
            text = playlist.name,
            style = MiuixAppTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}
