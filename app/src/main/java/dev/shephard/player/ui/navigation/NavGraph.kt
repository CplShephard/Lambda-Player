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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.animation.predictiveback.PredictiveBackAnimation
import dev.shephard.player.ui.animation.predictiveback.PredictiveBackExitDirection
import dev.shephard.player.ui.animation.predictiveback.installerNavTransition
import dev.shephard.player.ui.screens.AboutSettingsScreen
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.PlayerSettingsScreen
import dev.shephard.player.ui.screens.PlaylistDetailScreen
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.SettingsScreen
import dev.shephard.player.ui.screens.ThemeSettingsScreen
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection

@Composable
fun NavGraph(
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> },
    onOpenNowPlaying: () -> Unit = {}
) {
    // MADDE 1 — InstallerX Revived'ın orijinal page ve menu transition yapısı BİREBİR kopyalandı.
    // Menüler arası geçişte Miuix'in `NavTransitions.MiuixDefault` (installerNavTransition)
    // geçişi, NavDisplay (top.yukonga.miuix.kmp.nav.core.NavDisplay) katmanıyla kullanılıyor.
    val backStack = rememberNavBackStack<Route>(Route.Main)
    val navigator = remember(backStack) { Navigator(backStack) }
    val onBack = remember(navigator) { { navigator.pop() } }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        val navCornerRadius = 32.dp
        val roundAllCorners = false
        val effects = remember(navCornerRadius, roundAllCorners) {
            NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = navCornerRadius,
                cornerClipMode = NavCornerClipMode.Leading,
                dimAmount = 0.5f,
                blockInputDuringTransition = false,
            )
        }
        val transition = remember {
            installerNavTransition(
                animation = PredictiveBackAnimation.MIUIX,
                exitDirection = PredictiveBackExitDirection.ALWAYS_RIGHT,
            )
        }
        val swipeBackDirection = when (LocalLayoutDirection.current) {
            LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
            LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
        }

        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            transition = transition,
            effects = effects,
            modifier = modifier
        ) {
            entry<Route.Main> {
                InstallerNavEntry(interceptPredictiveBack = false, onBack = onBack) {
                    MainPagesContainer(
                        playerViewModel = playerViewModel,
                        hasMiniPlayer = hasMiniPlayer,
                        onTrackClick = onTrackClick,
                        onPlaylistRemixClick = onPlaylistRemixClick,
                        onOpenNowPlaying = onOpenNowPlaying
                    )
                }
            }
            entry<Route.PlaylistDetails>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(interceptPredictiveBack = false, onBack = onBack) {
                    PlaylistDetailScreen(
                        playlistIndex = key.playlistIndex,
                        onBack = onBack,
                        onTrackClick = onTrackClick
                    )
                }
            }
            entry<Route.Theme>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack = false, onBack = onBack) {
                    ThemeSettingsScreen(onBack = onBack)
                }
            }
            entry<Route.Player>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack = false, onBack = onBack) {
                    PlayerSettingsScreen(onBack = onBack)
                }
            }
            entry<Route.About>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack = false, onBack = onBack) {
                    AboutSettingsScreen(onBack = onBack)
                }
            }
        }
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

@Composable
fun InstallerNavEntry(
    interceptPredictiveBack: Boolean,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val navigationEventState = rememberNavigationEventState(
        NavigationEventInfo.None
    )
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = interceptPredictiveBack,
        onBackCompleted = onBack,
    )
    content()
}
