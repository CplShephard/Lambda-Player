package dev.shephard.player.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.screens.MusicScreen
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
    else -> 0
}

/** Settings altındaki (dock'suz) detay sayfaları. */
private val settingsSubRoutes = setOf(
    SettingsRoutes.Theme,
    SettingsRoutes.Player,
    SettingsRoutes.About
)

private fun isSettingsSubRoute(route: String?): Boolean = route in settingsSubRoutes

// Sekmeler arası (Music ↔ Playlists ↔ Settings) yatay kayma — IntOffset spring ile
private val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
    dampingRatio = 0.8f,
    stiffness = 380f
)

private val fadeSpring = spring<Float>(
    dampingRatio = 1f,
    stiffness = 380f
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
        // MADDE 8 — Settings alt sayfalarına girerken InstallerX/MIUIX "push" animasyonu:
        // yeni sayfa TAMAMEN sağdan gelir, alttaki sayfa paralaksla hafifçe sola kayar.
        // Alt sekmeler arası geçiş (Music/Playlists/Settings) ise eskisi gibi sıralamaya
        // göre sola/sağa kayan yatay geçiş olarak kalıyor — orada "push" mantığı yok,
        // sekmeler eşit seviyede.
        enterTransition = {
            if (isSettingsSubRoute(targetState.destination.route)) {
                PageTransitions.enterPush
            } else {
                val fromIdx = routeOrder(initialState.destination.route)
                val toIdx = routeOrder(targetState.destination.route)
                val dir = if (toIdx >= fromIdx)
                    AnimatedContentTransitionScope.SlideDirection.Left
                else
                    AnimatedContentTransitionScope.SlideDirection.Right
                slideIntoContainer(dir, springSpec) + fadeIn(fadeSpring)
            }
        },
        exitTransition = {
            if (isSettingsSubRoute(targetState.destination.route)) {
                PageTransitions.exitPush
            } else {
                val fromIdx = routeOrder(initialState.destination.route)
                val toIdx = routeOrder(targetState.destination.route)
                val dir = if (toIdx >= fromIdx)
                    AnimatedContentTransitionScope.SlideDirection.Left
                else
                    AnimatedContentTransitionScope.SlideDirection.Right
                slideOutOfContainer(dir, springSpec) + fadeOut(fadeSpring)
            }
        },
        popEnterTransition = {
            if (isSettingsSubRoute(initialState.destination.route)) {
                PageTransitions.popEnterPush
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    springSpec
                ) + fadeIn(fadeSpring)
            }
        },
        popExitTransition = {
            if (isSettingsSubRoute(initialState.destination.route)) {
                PageTransitions.popExitPush
            } else {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    springSpec
                ) + fadeOut(fadeSpring)
            }
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
                hasMiniPlayer = hasMiniPlayer
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
