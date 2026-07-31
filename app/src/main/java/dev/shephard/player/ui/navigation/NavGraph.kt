// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.screens.AboutSettingsScreen
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.PlayerSettingsScreen
import dev.shephard.player.ui.screens.PlaylistDetailScreen
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.SettingsScreen
import dev.shephard.player.ui.screens.ThemeSettingsScreen
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun NavGraph(
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> },
    onOpenNowPlaying: () -> Unit = {}
) {
    // MADDE 1 — InstallerX Revived / miuix 0.9.3'ün orijinal page ve menu transition yapısı BİREBİR entegre edildi.
    // Menüler arası geçişte miuix 0.9.3'ün kendi NavDisplay (androidx.navigation3.ui.NavDisplay) ve
    // NavDisplayTransitionEffects (32dp squircle corner clip + 0.5f dim scrim + predictive back) katmanı kullanılıyor.
    val backStack = remember { mutableStateListOf<NavKey>(Route.Main) }
    val navigator = remember(backStack) { Navigator(backStack) }
    val onBack = remember(navigator) { { navigator.pop() } }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        val entryProvider: (key: NavKey) -> NavEntry<NavKey> = { key ->
            when (key) {
                is Route.Main -> NavEntry(key) {
                    MainPagesContainer(
                        playerViewModel = playerViewModel,
                        hasMiniPlayer = hasMiniPlayer,
                        onTrackClick = onTrackClick,
                        onPlaylistRemixClick = onPlaylistRemixClick,
                        onOpenNowPlaying = onOpenNowPlaying
                    )
                }
                is Route.PlaylistDetails -> NavEntry(key) {
                    PlaylistDetailScreen(
                        playlistIndex = key.playlistIndex,
                        onBack = onBack,
                        onTrackClick = onTrackClick
                    )
                }
                is Route.Theme -> NavEntry(key) {
                    ThemeSettingsScreen(onBack = onBack)
                }
                is Route.Player -> NavEntry(key) {
                    PlayerSettingsScreen(onBack = onBack)
                }
                is Route.About -> NavEntry(key) {
                    AboutSettingsScreen(onBack = onBack)
                }
                else -> NavEntry(key) { }
            }
        }

        val entries = rememberDecoratedNavEntries(
            backStack = backStack,
            entryProvider = entryProvider,
        )

        NavDisplay(
            entries = entries,
            onBack = onBack,
            transitionEffects = NavDisplayTransitionEffects(
                enableCornerClip = true,
                dimAmount = 0.5f,
                blockInputDuringTransition = true,
                popDirectionFollowsSwipeEdge = false,
            ),
            modifier = modifier
        )
    }
}

@Composable
fun MainPagesContainer(
    playerViewModel: PlayerViewModel,
    hasMiniPlayer: Boolean,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit,
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Ana sayfalar (musics / playlists / settings) arası geçiş, InstallerX Revived'ın
    // orijinal HorizontalPager + MainPagerState.animateToPage (PagerState.kt) mekanizması
    // ile BİREBİR aynı yatay kayma geçiş animasyonunu kullanır.
    val navigator = LocalNavigator.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val mainPagerState = rememberMainPagerState(pagerState)
    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }
    MainScreenBackHandler(
        mainPagerState = mainPagerState,
        navigator = navigator,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BrandHeader(currentPageIndex = mainPagerState.selectedPage)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HorizontalPager(
                state = mainPagerState.pagerState,
                userScrollEnabled = true,
                overscrollEffect = null,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> MusicScreen(
                        playerViewModel = playerViewModel,
                        onTrackClick = { tracks, index -> onTrackClick(tracks, index, null) },
                        hasMiniPlayer = hasMiniPlayer
                    )
                    1 -> PlaylistScreen(
                        onTrackClick = onTrackClick,
                        onPlaylistRemixClick = onPlaylistRemixClick,
                        hasMiniPlayer = hasMiniPlayer,
                        onOpenDetail = { idx -> navigator.push(Route.PlaylistDetails(idx)) }
                    )
                    2 -> SettingsScreen(
                        playerViewModel = playerViewModel,
                        onOpenThemeSettings = { navigator.push(Route.Theme) },
                        onOpenPlayerSettings = { navigator.push(Route.Player) },
                        onOpenAbout = { navigator.push(Route.About) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                MiniPlayerHost(
                    playerViewModel = playerViewModel,
                    visible = hasMiniPlayer,
                    onOpenNowPlaying = onOpenNowPlaying
                )
                FloatingDock(
                    selectedIndex = mainPagerState.selectedPage,
                    onSelectPage = { idx -> mainPagerState.animateToPage(idx) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
