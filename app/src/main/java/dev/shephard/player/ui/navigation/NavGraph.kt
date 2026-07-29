package dev.shephard.player.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.PlaylistDetailScreen
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.AboutSettingsScreen
import dev.shephard.player.ui.screens.PlayerSettingsScreen
import dev.shephard.player.ui.screens.SettingsScreen
import dev.shephard.player.ui.screens.ThemeSettingsScreen

private fun routeOrder(route: String?): Int = when (route) {
    Destination.Music.route -> 0
    Destination.Playlists.route -> 1
    Destination.Settings.route -> 2
    SettingsRoutes.Theme, SettingsRoutes.Player, SettingsRoutes.About -> 3
    else -> if (route?.startsWith(PlaylistRoutes.DetailBase) == true) 2 else 0
}

// Alt sayfa mı? (Playlist Detail, Theme/Player/About Settings) — bunlar dock'un 3 ana
// sekmesi değil, InstallerX'teki gibi "yeni bir sayfaya girme" hissi vermesi gereken
// detay/ayar ekranları. Ana sekmeler arası (Music/Playlists/Settings) yatay tab-switch
// animasyonunu kullanırken, bu alt sayfalar kendi push/pop transition'ını kullanır.
private fun isSubPage(route: String?): Boolean =
    route == SettingsRoutes.Theme || route == SettingsRoutes.Player || route == SettingsRoutes.About ||
        route?.startsWith(PlaylistRoutes.DetailBase) == true

// Ekranlar arası animasyon — IntOffset spring ile
private val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
    dampingRatio = 0.8f,
    stiffness = 380f
)

private val fadeSpring = spring<Float>(
    dampingRatio = 1f,
    stiffness = 380f
)

// Alt sayfalara giriş: InstallerX'teki "yeni sekme" hissi — sağdan biraz daha güçlü bir
// push ile gelir, ana sekmeler arası geçişten görsel olarak ayrışır.
private val subPageSpringSpec = spring<androidx.compose.ui.unit.IntOffset>(
    dampingRatio = 0.86f,
    stiffness = 420f
)

@Composable
fun NavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> }
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Music.route,
        modifier = modifier,
        // Music→Playlist→Settings: soldan sağa sıralama → ileri = sola kayar.
        // Alt sayfalara (Playlist Detail, Theme/Player/About Settings) girerken InstallerX'teki
        // gibi ayrı, belirgin bir "yeni sayfa" push transition'ı kullanılır — ana sekmeler
        // arası tab-switch hissinden görsel olarak ayrışır.
        enterTransition = {
            val toRoute = targetState.destination.route
            if (isSubPage(toRoute)) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, subPageSpringSpec) +
                    fadeIn(fadeSpring)
            } else {
                val fromIdx = routeOrder(initialState.destination.route)
                val toIdx = routeOrder(toRoute)
                val dir = if (toIdx >= fromIdx)
                    AnimatedContentTransitionScope.SlideDirection.Left
                else
                    AnimatedContentTransitionScope.SlideDirection.Right
                slideIntoContainer(dir, springSpec) + fadeIn(fadeSpring)
            }
        },
        exitTransition = {
            val toRoute = targetState.destination.route
            if (isSubPage(toRoute)) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, subPageSpringSpec) +
                    fadeOut(fadeSpring)
            } else {
                val fromIdx = routeOrder(initialState.destination.route)
                val toIdx = routeOrder(toRoute)
                val dir = if (toIdx >= fromIdx)
                    AnimatedContentTransitionScope.SlideDirection.Left
                else
                    AnimatedContentTransitionScope.SlideDirection.Right
                slideOutOfContainer(dir, springSpec) + fadeOut(fadeSpring)
            }
        },
        popEnterTransition = {
            val fromRoute = initialState.destination.route
            val spec = if (isSubPage(fromRoute)) subPageSpringSpec else springSpec
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                spec
            ) + fadeIn(fadeSpring)
        },
        popExitTransition = {
            val fromRoute = initialState.destination.route
            val spec = if (isSubPage(fromRoute)) subPageSpringSpec else springSpec
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                spec
            ) + fadeOut(fadeSpring)
        }
    ) {
        composable(Destination.Music.route) {
            MusicScreen(
                playerViewModel = playerViewModel,
                onTrackClick = { tracks, index -> onTrackClick(tracks, index, null) },
                hasMiniPlayer = hasMiniPlayer
            )
        }
        composable(Destination.Playlists.route) {
            PlaylistScreen(
                onTrackClick = onTrackClick,
                onPlaylistRemixClick = onPlaylistRemixClick,
                onOpenPlaylist = { index -> navController.navigate(PlaylistRoutes.detail(index)) },
                hasMiniPlayer = hasMiniPlayer
            )
        }
        composable(
            route = PlaylistRoutes.DetailPattern,
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { entry ->
            val index = entry.arguments?.getInt("index") ?: 0
            PlaylistDetailScreen(
                playlistIndex = index,
                onBack = { navController.popBackStack() },
                onTrackClick = onTrackClick,
                onPlaylistRemixClick = onPlaylistRemixClick
            )
        }
        composable(Destination.Settings.route) {
            SettingsScreen(
                playerViewModel = playerViewModel,
                onOpenThemeSettings = { navController.navigate(SettingsRoutes.Theme) },
                onOpenPlayerSettings = { navController.navigate(SettingsRoutes.Player) },
                onOpenAbout = { navController.navigate(SettingsRoutes.About) }
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
