package dev.shephard.player.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Composable
fun NavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> }
) {
    // MADDE 1 — Geçişler InstallerX'in `NavTransitions.MiuixDefault` hissini veren
    // PageTransitions ile uygulanıyor (alpha 0.9 fade + 0.92↔1.08 ölçek + parallax kayma).
    // MiuixDefault tek bir global geçiştir; burada da TÜM sayfalara (Music / Playlists /
    // Settings ve alt sayfalar) aynı geçiş uygulanıyor.
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
