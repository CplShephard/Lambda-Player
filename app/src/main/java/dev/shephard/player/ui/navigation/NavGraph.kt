package dev.shephard.player.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.AboutSettingsScreen
import dev.shephard.player.ui.screens.PlayerSettingsScreen
import dev.shephard.player.ui.screens.SettingsScreen
import dev.shephard.player.ui.screens.ThemeSettingsScreen

// Miuix NavDisplay route anahtarları — her biri androidx.navigation3.runtime.NavKey.
object MusicRoute : NavKey
object PlaylistsRoute : NavKey
object SettingsRoute : NavKey
object ThemeRoute : NavKey
object PlayerRoute : NavKey
object AboutRoute : NavKey

@Composable
fun NavGraph(
    backStack: SnapshotStateList<NavKey>,
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> }
) {
    // GERÇEK Miuix NavDisplay — miuix-navigation3-ui (androidx.navigation3 tabanlı).
    // Tüm geçişler Miuix'in kendi NavDisplayTransitionEffects'i ile yapılır:
    // squircle köşe kırpma + dim scrim + predictive back. Bu Miuix'in resmi davranışıdır;
    // Compose Navigation (androidx.navigation.compose) tamamen kaldırıldı.
    val entryProvider = remember(backStack) {
        entryProvider<NavKey> {
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
                    onOpenThemeSettings = { backStack.add(ThemeRoute) },
                    onOpenPlayerSettings = { backStack.add(PlayerRoute) },
                    onOpenAbout = { backStack.add(AboutRoute) }
                )
            }
            entry<ThemeRoute> {
                ThemeSettingsScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }
            entry<PlayerRoute> {
                PlayerSettingsScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }
            entry<AboutRoute> {
                AboutSettingsScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }
        }
    }

    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider,
    )

    val transitionEffects = remember(Unit) {
        NavDisplayTransitionEffects(
            enableCornerClip = false,
            dimAmount = 0f,
            blockInputDuringTransition = true,
            popDirectionFollowsSwipeEdge = true,
        )
    }

    Box(modifier = modifier) {
        NavDisplay(
            entries = entries,
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            transitionEffects = transitionEffects,
        )
    }
}
