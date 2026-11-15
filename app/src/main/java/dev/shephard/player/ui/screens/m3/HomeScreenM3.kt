// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 Material 3 Expressive - Home Screen
@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package dev.shephard.player.ui.screens.m3

import dev.shephard.player.ui.components.PlaylistCoverArt
import dev.shephard.player.ui.components.m3.m3CardPress
import dev.shephard.player.ui.components.HomeYourMix
import dev.shephard.player.ui.components.HomeListeningTime
import dev.shephard.player.ui.components.bottomOverlayContentPadding
import dev.shephard.player.ui.components.rememberHomeMix
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.shephard.player.ui.components.m3.rememberExpressiveTopBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dev.shephard.player.ui.components.m3.M3SmallTopBar
import dev.shephard.player.ui.components.m3.m3TopBarColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.m3.LineageHorizontalMediaItem
import dev.shephard.player.ui.components.m3.LineageNoElements
import dev.shephard.player.ui.components.m3.LineageSectionHeader
import dev.shephard.player.ui.i18n.LocalStrings
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
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onOpenPlaylistDetail: (Int) -> Unit = {},
    onOpenStats: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val isLandscape = dev.shephard.player.ui.util.isLandscapeWindow()
    val listBottomPadding = bottomOverlayContentPadding(
        fallback = when {
            hasMiniPlayer && isLandscape -> 136.dp
            hasMiniPlayer -> 144.dp
            isLandscape -> 84.dp
            else -> 96.dp
        },
    )
    val tracks by libraryViewModel.tracks.collectAsState()
    val hasScanned by libraryViewModel.hasScanned.collectAsState()
    val recentEvents by playerViewModel.statsEventsFlow.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) { libraryViewModel.ensureTracksLoaded() }
    val totalListeningMs by playerViewModel.totalListeningMsLive.collectAsState()

    val featuredTracks = dev.shephard.player.ui.components.rememberFeaturedTracks(tracks, 10)

    val recentlyPlayedTracks = dev.shephard.player.ui.components.rememberRecentTracks(tracks, recentEvents, 15)

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
    val yourMix = rememberHomeMix(tracks, likedIds, recentEvents)
    val featuredPlaylists = remember(rawPlaylists, strings) {
        rawPlaylists.filter {
            !it.isSystem &&
                it.name != strings.likedSongs &&
                it.name != "Liked Songs" &&
                it.name != "Beğenilenler" &&
                it.name != "Favoriler"
        }.shuffled().take(10)
    }

    // Playlist detail is a top-level submenu (PlaylistDetailRoute in NavGraph), exactly like the
    // settings submenus: it covers the pager, dock and mini player by z-order instead of living
    // inside this page. Opening it just pushes the route.
                val m3TopBarState = rememberExpressiveTopBarState()
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize().nestedScroll(m3TopBarState.connection),
                    containerColor = MaterialTheme.colorScheme.surface,
                    topBar = {
                        M3SmallTopBar(
                            state = m3TopBarState,
                            title = { Text(strings.home) },
                            colors = m3TopBarColors(),
                        )
                    },
                ) { padding ->
                        if (isLandscape) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding() + 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                    contentPadding = PaddingValues(start = 12.dp, end = 8.dp, bottom = listBottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    item(key = "your_mix") {
                                        HomeYourMix(
                                            yourMix,
                                            useMiuix = false,
                                            isLoading = !hasScanned,
                                            compact = isLandscape,
                                            onRemix = { playerViewModel.setQueueAndPlayRemixed(it, strings.yourMix) }
                                        ) { queue, index -> onTrackClick(queue, index, strings.yourMix) }
                                    }
                                    item(key = "listening_time") {
                                        HomeListeningTime(totalListeningMs, useMiuix = false, onClick = onOpenStats, compact = isLandscape)
                                    }
                                }
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                    contentPadding = PaddingValues(start = 8.dp, end = 12.dp, bottom = listBottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (featuredPlaylists.isNotEmpty()) {
                                        item(key = "featured_playlists") {
                                            LineageSectionHeader(title = strings.featuredPlaylists)
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                items(featuredPlaylists, key = { it.name + it.createdAt }) { pl ->
                                                    val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                                                    LineageHorizontalMediaItem(
                                                        headline = pl.name,
                                                        subhead = "${plTracks.size} tracks",
                                                        thumbnailSize = if (isLandscape) 108.dp else 128.dp,
                                                        thumbnailContent = {
                                                            PlaylistCoverArt(
                                                                coverUri = pl.coverUri,
                                                                tracks = plTracks,
                                                                placeholderColor = MaterialTheme.colorScheme.secondaryContainer,
                                                                placeholderIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                fallback = {
                                                                    Icon(Icons.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(48.dp))
                                                                }
                                                            )
                                                        },
                                                        onClick = {
                                                            val i = dev.shephard.player.ui.screens.ensureLikedSongsPlaylist(rawPlaylists, strings).indexOf(pl)
                                                            if (i >= 0) onOpenPlaylistDetail(i)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (recentlyPlayedTracks.isNotEmpty()) {
                                        item(key = "recently_played_carousel") {
                                            RecentlyPlayedSectionM3(
                                                title = strings.recentlyPlayed,
                                                tracks = recentlyPlayedTracks,
                                                compact = isLandscape,
                                                onTrackClick = { index -> onTrackClick(recentlyPlayedTracks, index, strings.recentlyPlayed) },
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    top = padding.calculateTopPadding() + 4.dp,
                                    bottom = listBottomPadding
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item(key = "your_mix") {
                                    HomeYourMix(
                                        yourMix,
                                        useMiuix = false,
                                        isLoading = !hasScanned,
                                        compact = isLandscape,
                                        onRemix = { playerViewModel.setQueueAndPlayRemixed(it, strings.yourMix) }
                                    ) { queue, index -> onTrackClick(queue, index, strings.yourMix) }
                                }
                                item(key = "listening_time") {
                                    HomeListeningTime(totalListeningMs, useMiuix = false, onClick = onOpenStats, compact = isLandscape)
                                }
                                if (featuredPlaylists.isNotEmpty()) {
                                    item {
                                        LineageSectionHeader(title = strings.featuredPlaylists)
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(featuredPlaylists, key = { it.name + it.createdAt }) { pl ->
                                                val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                                                LineageHorizontalMediaItem(
                                                    headline = pl.name,
                                                    subhead = "${plTracks.size} tracks",
                                                    thumbnailSize = if (isLandscape) 108.dp else 128.dp,
                                                    thumbnailContent = {
                                                        PlaylistCoverArt(
                                                            coverUri = pl.coverUri,
                                                            tracks = plTracks,
                                                            placeholderColor = MaterialTheme.colorScheme.secondaryContainer,
                                                            placeholderIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            fallback = {
                                                                Icon(Icons.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(48.dp))
                                                            }
                                                        )
                                                    },
                                                    onClick = {
                                                        val i = dev.shephard.player.ui.screens.ensureLikedSongsPlaylist(rawPlaylists, strings).indexOf(pl)
                                                        if (i >= 0) onOpenPlaylistDetail(i)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (recentlyPlayedTracks.isNotEmpty()) {
                                    item(key = "recently_played_carousel") {
                                        RecentlyPlayedSectionM3(
                                            title = strings.recentlyPlayed,
                                            tracks = recentlyPlayedTracks,
                                            compact = isLandscape,
                                            onTrackClick = { index ->
                                                onTrackClick(recentlyPlayedTracks, index, strings.recentlyPlayed)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                }
                }
}

@Composable
private fun RecentlyPlayedSectionM3(
    title: String,
    tracks: List<AudioTrack>,
    compact: Boolean = false,
    onTrackClick: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { tracks.size })
    Column(modifier = Modifier.fillMaxWidth()) {
        LineageSectionHeader(title = title)
        Spacer(Modifier.height(10.dp))
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(if (compact) 196.dp else 268.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 60.dp),
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1,
        ) { page ->
            val track = tracks[page]
            if (page == 0) {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        (fadeIn(tween(300)) + scaleIn(
                            initialScale = 0.82f,
                            animationSpec = spring(dampingRatio = 0.52f, stiffness = 340f),
                        )).togetherWith(
                            fadeOut(tween(200)) + scaleOut(targetScale = 0.82f, animationSpec = tween(200)),
                        )
                    },
                    label = "m3RecentTrackBounceFade",
                ) { displayedTrack ->
                    RecentlyPlayedCardM3(displayedTrack, compact = compact) { onTrackClick(0) }
                }
            } else {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInHorizontally(tween(350)) { -it / 3 }).togetherWith(
                            fadeOut(tween(200)) + slideOutHorizontally(tween(350)) { it / 3 },
                        )
                    },
                    label = "m3RecentTrackSlide",
                ) { displayedTrack ->
                    RecentlyPlayedCardM3(displayedTrack, compact = compact) { onTrackClick(page) }
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedCardM3(
    track: AudioTrack,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(if (compact) 196.dp else 268.dp)
            .aspectRatio(1f)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .m3CardPress(shape = RoundedCornerShape(18.dp), overlayColor = Color.White, onClick = onClick),
    ) {
        var loaded by remember(track.id) { mutableStateOf(false) }
        AsyncImage(
            model = dev.shephard.player.ui.components.rememberStaticArtwork(track.albumArtUri),
            contentDescription = track.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onState = { loaded = it is AsyncImagePainter.State.Success },
        )
        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f)), startY = 100f)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${track.artist} • ${track.album}",
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
