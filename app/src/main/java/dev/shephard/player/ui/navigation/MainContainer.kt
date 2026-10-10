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
import kotlinx.coroutines.runBlocking
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
    val initialUseMiuix = remember { runBlocking { prefs.useMiuix.first() } }
    val useMiuix by prefs.useMiuix.collectAsState(initial = initialUseMiuix)

    // Resolve the persisted "last main page" synchronously on the very first
    // composition so rememberPagerState is built with a stable initial value.
    // We deliberately do NOT collect lastMainPage as state — doing so would
    // cause a recomposition loop and re-create the pager state on every page
    // change, which in turn causes a noticeable hitch when leaving Settings.
    val initialLastMainPage = remember { runBlocking { prefs.lastMainPage.first() } }

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

        // Build the pager state once with a stable initial value. The
        // rememberPagerState factory is keyed only on pageCount, so this won't
        // be rebuilt on subsequent recompositions.
        val pagerState = rememberPagerState(
            initialPage = initialLastMainPage.coerceIn(0, bottomNavDestinations.size - 1),
            pageCount = { bottomNavDestinations.size },
        )
        val mainPagerState = rememberMainPagerState(pagerState)
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
        LaunchedEffect(settledPage) {
            if (initialLastMainPage != settledPage) {
                delay(250)
                prefs.setLastMainPage(settledPage)
            }
        }

        val topKey = backStack.last()
        // A submenu (Theme/Player/About/Stats) is "open" when it is the top of the
        // back stack. This is only used for the system back handler — the dock and
        // mini player no longer react to it because they live inside the MainRoute
        // entry of NavGraph, where the pushed submenu covers them by z-order.
        val submenuOpen = topKey is ThemeRoute || topKey is PlayerRoute ||
            topKey is AboutRoute || topKey is StatsRoute

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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeBackground),
                    hasMiniPlayer = hasMiniPlayer,
                    onOpenNowPlaying = { showNowPlaying = true },
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
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(0)),
                modifier = Modifier.fillMaxSize()
            ) {
                if (useMiuix) {
                    NowPlayingSheet(
                        playerViewModel = playerViewModel,
                        onDismiss = { showNowPlaying = false }
                    )
                } else {
                    M3NowPlayingSheet(
                        playerViewModel = playerViewModel,
                        onDismiss = { showNowPlaying = false }
                    )
                }
            }
        }
    }
}
