package dev.shephard.player.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.AboutSettingsScreen
import dev.shephard.player.ui.screens.PlayerSettingsScreen
import dev.shephard.player.ui.screens.SettingsScreen
import dev.shephard.player.ui.screens.ThemeSettingsScreen
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.entry

// Miuix NavDisplay route anahtarları — her biri NavKey.
// NOT: rememberNavBackStack kullanılmıyor; serileştirme (kotlinx-serialization) gerektiren
// yolu atlayıp doğrudan mutableStateListOf<NavKey> + NavController ile kuruluyor.
object MusicRoute : NavKey
object PlaylistsRoute : NavKey
object SettingsRoute : NavKey
object ThemeRoute : NavKey
object PlayerRoute : NavKey
object AboutRoute : NavKey

@Composable
fun NavGraph(
    navController: NavController,
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> }
) {
    // MADDE 1 — Sayfa geçişleri artık Miuix'in kendi NavDisplay'i ile, global geçiş olarak
    // NavTransitions.MiuixDefault kullanılarak yapılıyor (InstallerX ile birebir). Tüm
    // sayfalar (Music / Playlists / Settings ve alt sayfalar) bu tek geçişi paylaşıyor.
    NavDisplay(navController, modifier = modifier.fillMaxSize()) {
        entry<MusicRoute> {
            MusicScreen(
                playerViewModel = playerViewModel,
                onTrackClick = { tracks, index -> onTrackClick(tracks, index, null) },
                hasMiniPlayer = hasMiniPlayer
            )
        }
        entry<PlaylistsRoute> {
            PlaylistScreen(
                onTrackClick = onTrackClick,
                onPlaylistRemixClick = onPlaylistRemixClick,
                hasMiniPlayer = hasMiniPlayer
            )
        }
        entry<SettingsRoute> {
            SettingsScreen(
                playerViewModel = playerViewModel,
                onOpenThemeSettings = { navController.push(ThemeRoute) },
                onOpenPlayerSettings = { navController.push(PlayerRoute) },
                onOpenAbout = { navController.push(AboutRoute) }
            )
        }
        entry<ThemeRoute> {
            ThemeSettingsScreen(onBack = { navController.pop() })
        }
        entry<PlayerRoute> {
            PlayerSettingsScreen(onBack = { navController.pop() })
        }
        entry<AboutRoute> {
            AboutSettingsScreen(onBack = { navController.pop() })
        }
    }
}
