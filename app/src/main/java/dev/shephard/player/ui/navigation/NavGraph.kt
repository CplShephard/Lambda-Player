// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.screens.AboutSettingsScreen
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.PlayerSettingsScreen
import dev.shephard.player.ui.screens.PlaylistDetailScreen
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.SettingsScreen
import dev.shephard.player.ui.screens.ThemeSettingsScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> },
    onOpenNowPlaying: () -> Unit = {}
) {
    // MADDE 1 — InstallerX Revived'ın orijinal page ve menu transition animasyonları BİREBİR entegre edildi.
    // Resmi Jetpack Compose NavHost üzerinde menüler arası geçişte MiuixDefault ({it} sağdan girer 0 fade,
    // -it/4 sola paralaks ve %0.9 soluklaşır) kullanılır.
    NavHost(
        navController = navController,
        startDestination = Destination.Music.route,
        modifier = modifier,
        enterTransition = { PageTransitions.enterPush },
        exitTransition = { PageTransitions.exitPush },
        popEnterTransition = { PageTransitions.popEnterPush },
        popExitTransition = { PageTransitions.popExitPush }
    ) {
        composable(Destination.Music.route) {
            MainPagesContainer(
                playerViewModel = playerViewModel,
                hasMiniPlayer = hasMiniPlayer,
                onTrackClick = onTrackClick,
                onPlaylistRemixClick = onPlaylistRemixClick,
                onOpenDetail = { idx -> navController.navigate("playlists/detail/$idx") },
                onOpenThemeSettings = { navController.navigate(SettingsRoutes.Theme) },
                onOpenPlayerSettings = { navController.navigate(SettingsRoutes.Player) },
                onOpenAbout = { navController.navigate(SettingsRoutes.About) },
                onOpenNowPlaying = onOpenNowPlaying
            )
        }
        composable(
            route = "playlists/detail/{index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            PlaylistDetailScreen(
                playlistIndex = index,
                onBack = { navController.popBackStack() },
                onTrackClick = onTrackClick,
                onPlaylistRemixClick = onPlaylistRemixClick
            )
        }
        composable(SettingsRoutes.Theme) {
            ThemeSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoutes.Player) {
            PlayerSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoutes.About) {
            AboutSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun MainPagesContainer(
    playerViewModel: PlayerViewModel,
    hasMiniPlayer: Boolean,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit,
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit,
    onOpenDetail: (Int) -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenPlayerSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Ana sayfalar (musics / playlists / settings) arası geçiş, InstallerX Revived'ın
    // orijinal HorizontalPager + animateScrollToPage mekanizması ile BİREBİR aynı yatay kayma
    // geçiş animasyonunu kullanır.
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BrandHeader(currentPageIndex = pagerState.currentPage)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HorizontalPager(
                state = pagerState,
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
                        onOpenDetail = onOpenDetail
                    )
                    2 -> SettingsScreen(
                        playerViewModel = playerViewModel,
                        onOpenThemeSettings = onOpenThemeSettings,
                        onOpenPlayerSettings = onOpenPlayerSettings,
                        onOpenAbout = onOpenAbout
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
                    selectedIndex = pagerState.currentPage,
                    onSelectPage = { idx ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(idx)
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
