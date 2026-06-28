package dev.shephard.player.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.SettingsScreen

// Ekranlar arası animasyon — IntOffset spring ile
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
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> }
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Music.route,
        modifier = modifier,
        // Music→Playlist→Settings: soldan sağa sıralama → ileri = sola kayar
        enterTransition = {
            val destinations = listOf(
                Destination.Music.route,
                Destination.Playlists.route,
                Destination.Settings.route
            )
            val fromIdx = destinations.indexOf(initialState.destination.route)
            val toIdx   = destinations.indexOf(targetState.destination.route)
            val dir = if (toIdx >= fromIdx)
                AnimatedContentTransitionScope.SlideDirection.Left
            else
                AnimatedContentTransitionScope.SlideDirection.Right
            slideIntoContainer(dir, springSpec) + fadeIn(fadeSpring)
        },
        exitTransition = {
            val destinations = listOf(
                Destination.Music.route,
                Destination.Playlists.route,
                Destination.Settings.route
            )
            val fromIdx = destinations.indexOf(initialState.destination.route)
            val toIdx   = destinations.indexOf(targetState.destination.route)
            val dir = if (toIdx >= fromIdx)
                AnimatedContentTransitionScope.SlideDirection.Left
            else
                AnimatedContentTransitionScope.SlideDirection.Right
            slideOutOfContainer(dir, springSpec) + fadeOut(fadeSpring)
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                springSpec
            ) + fadeIn(fadeSpring)
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                springSpec
            ) + fadeOut(fadeSpring)
        }
    ) {
        composable(Destination.Music.route) {
            MusicScreen(
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
            SettingsScreen()
        }
    }
}
