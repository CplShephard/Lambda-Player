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
    // Resolve the startup page before creating the pager without blocking the UI thread.
    // The value is loaded once, so later page writes never recreate the pager.
    val startup by produceState<Triple<Boolean, Boolean, Int>?>(null, prefs) {
        prefs.migrateLibraryNavigation()
        value = Triple(prefs.useMiuix.first(), prefs.onlineModeEnabled.first(), prefs.lastMainPage.first())
    }
    val initial = startup ?: return
    val useMiuix by prefs.useMiuix.collectAsState(initial = initial.first)
    val startupOnlineEnabled = initial.second
    val initialLastMainPage = initial.third
    // Online mode controls the 4th dock tab (Discover to the left of Home)
    val initialOnlineModeEnabled = remember { kotlinx.coroutines.runBlocking { prefs.onlineModeEnabled.first() } }
    val onlineModeEnabled by prefs.onlineModeEnabled.collectAsState(initial = initialOnlineModeEnabled)
    val activeDestinations = remember(onlineModeEnabled) { activeBottomNavDestinations(onlineModeEnabled) }
    // Translate persisted 3-page index to current 4-page layout:
    // offline 0=Home,1=Library,2=Settings  ->  online 1=Home,2=Library,3=Settings,0=Discover
    val adjustedInitialPage = remember(startupOnlineEnabled, onlineModeEnabled, initialLastMainPage) {
        when {
            !startupOnlineEnabled && onlineModeEnabled -> (initialLastMainPage + 1).coerceIn(0, activeDestinations.size - 1)
            startupOnlineEnabled && !onlineModeEnabled -> (initialLastMainPage - 1).coerceIn(0, activeDestinations.size - 1)
            else -> initialLastMainPage.coerceIn(0, activeDestinations.size - 1)
        }
    }

    // The custom wallpaper feature was removed: clear its leftover settings and image files once.
    LaunchedEffect(Unit) { prefs.clearRemovedWallpaperData() }

    // Backdrop used by the dock / mini player glass. Its layer records ONLY the
    // pager (inside NavGraph), and every consumer of it lives OUTSIDE that
    // recording, so it can never become self-referential.
    //
    // NOTE: we deliberately do NOT record a whole-app backdrop anymore. Wrapping
    // the entire app in Modifier.layerBackdrop(...) while in-app glass surfaces
    // (Apple dock, glass icon buttons) consume the same backdrop creates a
    // self-referential RenderNode, which the RenderThread cannot terminate —
    // prepareTreeImpl() recurses until the stack overflows and the app dies with
    // SIGSEGV right after enabling blur.
    val contentBackdrop = rememberAppBlurBackdrop(blurEnabled)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalContentBackdrop provides contentBackdrop,
    ) {
        val backStack = remember { mutableStateListOf<NavKey>(MainRoute) }
        var showNowPlaying by remember { mutableStateOf(false) }
        var miniPlayerBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

        // Build the pager state once with a stable initial value. The
        // pageCount lambda reacts to onlineModeEnabled so the pager expands to 4 pages
        // without recreating thePagerState — existing pager animations keep running.
        val pagerState = rememberPagerState(
            initialPage = adjustedInitialPage.coerceIn(0, (activeDestinations.size - 1).coerceAtLeast(0)),
            pageCount = { activeDestinations.size },
        )
        val mainPagerState = rememberMainPagerState(pagerState)
        // Keep the selected tab in sync when Online Mode toggles (Discover inserted/removed at front)
        var previousOnlineEnabled by remember { mutableStateOf(onlineModeEnabled) }
        androidx.compose.runtime.LaunchedEffect(onlineModeEnabled, activeDestinations.size) {
            if (previousOnlineEnabled != onlineModeEnabled) {
                val target = if (onlineModeEnabled) {
                    // offline (Home 0) -> online (Home 1): shift +1
                    (mainPagerState.selectedPage + 1).coerceIn(0, activeDestinations.size - 1)
                } else {
                    // online -> offline: Discover 0 -> Home 0, others -1
                    if (mainPagerState.selectedPage == 0) 0 else (mainPagerState.selectedPage - 1).coerceIn(0, activeDestinations.size - 1)
                }
                if (target != mainPagerState.selectedPage && mainPagerState.pagerState.currentPage == mainPagerState.selectedPage) {
                    mainPagerState.animateToPage(target)
                }
                previousOnlineEnabled = onlineModeEnabled
            }
        }
        val currentPage = mainPagerState.pagerState.currentPage
        val settledPage = mainPagerState.pagerState.settledPage

        // Keep the nav dock's selected page in sync with the pager the moment the
        // current page changes (a swipe updates currentPage as soon as it crosses
        // the halfway point), instead of waiting for the settle animation to end —
        // that lag is what made the dock look like it refreshed "a bit after"
        // entering the page. animateToPage() already sets selectedPage itself, and
        // syncPage() is guarded by isNavigating so it won't fight that animation.
        LaunchedEffect(currentPage) {
            mainPagerState.syncPage()
        }

        // Persist the settled page, debounced so we don't write to DataStore on
        // every intermediate frame of a swipe gesture. This is the main fix for
        // the Settings -> Playlists transition hitch.
        // We persist the raw pager index (0..3 online, 0..2 offline). Startup translation handles mode changes.
        LaunchedEffect(settledPage) {
            delay(250)
            prefs.setLastMainPage(settledPage)
        }

        val topKey = backStack.last()
        // A submenu (Theme/Player/About/Stats) is "open" when it is the top of the
        // back stack. This is only used for the system back handler — the dock and
        // mini player no longer react to it because they live inside the MainRoute
        // entry of NavGraph, where the pushed submenu covers them by z-order.
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

        // Submenus are popped by NavDisplay's own predictive back handling (see NavGraph), so this
        // plain BackHandler only covers the now-playing sheet and returning to the first pager page.
        // Stays off while a drawer/sheet/dialog is open so that overlay handles the back gesture itself.
        BackHandler(
            enabled = !OverlayBackState.hasOpenOverlay &&
                (showNowPlaying || (!submenuOpen && mainPagerState.selectedPage != 0))
        ) {
            when {
                showNowPlaying -> showNowPlaying = false
                else -> mainPagerState.animateToPage(0)
            }
        }

        // Solid theme background behind the whole app.
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
            // No whole-app layerBackdrop here — see the comment on contentBackdrop
            // above. Recording the entire app into a backdrop and then drawing from
            // that backdrop inside the app crashes the RenderThread (self-referential
            // RenderNode). This Box is just the plain layout container.
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                NavGraph(
                    backStack = backStack,
                    playerViewModel = playerViewModel,
                    useMiuix = useMiuix,
                    preferences = prefs,
                    mainPagerState = mainPagerState,
                    activeDestinations = activeDestinations,
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
                // The sheet owns its bidirectional bounds animation; keep it mounted until
                // its collapse completes instead of applying a second fade on top.
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
