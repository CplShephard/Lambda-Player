// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import top.yukonga.miuix.kmp.nav.core.NavKey
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.rememberAppBlurBackdrop
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.screens.NowPlayingSheet
import dev.shephard.player.ui.screens.m3.M3NowPlayingSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import top.yukonga.miuix.kmp.blur.layerBackdrop
import dev.shephard.player.ui.util.OverlayBackState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainContainer(
    playerViewModel: PlayerViewModel = viewModel(),
    initialAudioUri: android.net.Uri? = null
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val languageCode by prefs.language.collectAsState(initial = "en")
    val strings = remember(languageCode) { stringsFor(languageCode) }

    val blurEnabled = LocalBlurEnabled.current
    // Resolve the startup page without blocking the UI thread.
    val startup by produceState<Pair<Boolean, Int>?>(null, prefs) {
        prefs.migrateLibraryNavigation()
        value = prefs.useMiuix.first() to prefs.lastMainPage.first()
    }
    val initial = startup ?: return
    val useMiuix by prefs.useMiuix.collectAsState(initial = initial.first)
    val initialLastMainPage = initial.second

    // The custom wallpaper feature was removed: clear its leftover settings and image files once.
    LaunchedEffect(Unit) { prefs.clearRemovedWallpaperData() }

    val contentBackdrop = rememberAppBlurBackdrop(blurEnabled)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalContentBackdrop provides contentBackdrop,
    ) {
        val backStack = remember { mutableStateListOf<NavKey>(MainRoute) }
        var showNowPlaying by remember { mutableStateOf(false) }
        var miniPlayerBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

        val pagerState = rememberPagerState(
            initialPage = initialLastMainPage.coerceIn(0, bottomNavDestinations.size - 1),
            pageCount = { bottomNavDestinations.size },
        )
        val mainPagerState = rememberMainPagerState(pagerState)
        val currentPage = mainPagerState.pagerState.currentPage
        val settledPage = mainPagerState.pagerState.settledPage

        LaunchedEffect(currentPage) {
            mainPagerState.syncPage()
        }

        LaunchedEffect(settledPage) {
            delay(250)
            prefs.setLastMainPage(settledPage)
        }

        val topKey = backStack.last()
        val submenuOpen = topKey is ThemeRoute || topKey is PlayerRoute ||
            topKey is AboutRoute || topKey is StatsRoute || topKey is PlaylistDetailRoute || topKey is AlbumDetailRoute

        val hasMiniPlayer by remember(playerViewModel) {
            playerViewModel.uiState
                .map { it.currentTrack != null }
                .distinctUntilChanged()
        }.collectAsState(initial = false)

        LaunchedEffect(initialAudioUri) {
            if (initialAudioUri != null) {
                playerViewModel.playExternalUri(initialAudioUri)
                delay(120)
                showNowPlaying = true
            }
        }

        BackHandler(
            enabled = !OverlayBackState.hasOpenOverlay &&
                (showNowPlaying || (!submenuOpen && mainPagerState.selectedPage != 0))
        ) {
            when {
                showNowPlaying -> showNowPlaying = false
                else -> mainPagerState.animateToPage(0)
            }
        }

        val themeBackground = if (useMiuix) {
            MiuixAppTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeBackground)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                NavGraph(
                    backStack = backStack,
                    playerViewModel = playerViewModel,
                    useMiuix = useMiuix,
                    preferences = prefs,
                    mainPagerState = mainPagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeBackground),
                    hasMiniPlayer = hasMiniPlayer,
                    onOpenNowPlaying = { showNowPlaying = true },
                    onMiniPlayerBoundsChanged = { miniPlayerBounds = it },
                    onTrackClick = { tracks, index, playlistName ->
                        playerViewModel.setQueueAndPlay(tracks, index, playlistName)
                    },
                    onPlaylistRemixClick = { tracks, playlistName ->
                        playerViewModel.setQueueAndPlayRemixed(tracks, playlistName)
                    }
                )
            }

            AnimatedVisibility(
                visible = showNowPlaying,
                enter = androidx.compose.animation.EnterTransition.None,
                exit = androidx.compose.animation.ExitTransition.None,
                modifier = Modifier.fillMaxSize()
            ) {
                if (useMiuix) {
                    NowPlayingSheet(
                        playerViewModel = playerViewModel,
                        collapsedBounds = miniPlayerBounds,
                        onAlbumClick = { track ->
                            if ((track.albumId ?: 0L) > 0L || track.album.isNotBlank()) {
                                showNowPlaying = false
                                val destination = AlbumDetailRoute(track.albumId ?: 0L, track.album, track.artist)
                                if (backStack.lastOrNull() != destination) backStack.add(destination)
                            }
                        },
                        onDismiss = { showNowPlaying = false }
                    )
                } else {
                    M3NowPlayingSheet(
                        playerViewModel = playerViewModel,
                        collapsedBounds = miniPlayerBounds,
                        onAlbumClick = { track ->
                            if ((track.albumId ?: 0L) > 0L || track.album.isNotBlank()) {
                                showNowPlaying = false
                                val destination = AlbumDetailRoute(track.albumId ?: 0L, track.album, track.artist)
                                if (backStack.lastOrNull() != destination) backStack.add(destination)
                            }
                        },
                        onDismiss = { showNowPlaying = false }
                    )
                }
            }
        }
    }
}
