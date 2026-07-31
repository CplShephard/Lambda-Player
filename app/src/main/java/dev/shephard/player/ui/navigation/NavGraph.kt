package dev.shephard.player.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.screens.AboutSettingsScreen
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.PlayerSettingsScreen
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.SettingsScreen
import dev.shephard.player.ui.screens.ThemeSettingsScreen

// Miuix NavDisplay route anahtarları — her biri androidx.navigation3.runtime.NavKey.
object MusicRoute : NavKey
object PlaylistsRoute : NavKey
object SettingsRoute : NavKey
object ThemeRoute : NavKey
object PlayerRoute : NavKey
object AboutRoute : NavKey

// --- ORİJİNAL animasyon (projenin sana ilk verildiği hâli) -------------------
// İlk sürümde androidx.navigation.compose NavHost ile kullanılan klasik slide:
// ileri = soldan/sağa yöne göre tam genişlikte kayma + hafif fade, spring tabanlı
// (dampingRatio 0.8, stiffness 380). Bu, Music / Playlists / Settings (üst seviye
// sekmeler) için kullanılır.
//
// NOT: Bu Compose sürümünde `slideIntoContainer` / `slideOutOfContainer` sembolleri
// classpath'te yok; Miuix'in kendisi de `slideInHorizontally` / `slideOutHorizontally`
// kullanıyor. O yüzden orijinal `slideIntoContainer(SlideDirection.Left)` davranışını
// birebir veren `slideInHorizontally(initialOffsetX = { it })` (sağdan giriş) +
// `slideOutHorizontally(targetOffsetX = { -it })` (sola tam çıkış) ile kuruyoruz.
private val originalSlideSpec = spring<IntOffset>(
    dampingRatio = 0.8f,
    stiffness = 380f,
)
private val originalFadeSpec = spring<Float>(
    dampingRatio = 1f,
    stiffness = 380f,
)

// Üst seviye sekmeler için orijinal klasik slide (sabit yön: ileri = Sol, geri = Sağ).
private val originalTabTransition: Map<String, Any> =
    NavDisplay.transitionSpec {
        ContentTransform(
            slideInHorizontally(initialOffsetX = { it }, animationSpec = originalSlideSpec) + fadeIn(originalFadeSpec),
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = originalSlideSpec) + fadeOut(originalFadeSpec),
        )
    } + NavDisplay.popTransitionSpec {
        ContentTransform(
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = originalSlideSpec) + fadeIn(originalFadeSpec),
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = originalSlideSpec) + fadeOut(originalFadeSpec),
        )
    }

private fun isSubmenu(key: NavKey?): Boolean =
    key is ThemeRoute || key is PlayerRoute || key is AboutRoute

@Composable
fun NavGraph(
    backStack: SnapshotStateList<NavKey>,
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> }
) {
    val currentTop = backStack.last()

    // Bir önceki üst route'u hatırla: transitionEffects'in (corner clip + dim scrim) yalnızca
    // alt menü (Theme / Player / About) geçişlerinde Miuix stiline geçmesi için kullanılır.
    var prevTop by remember { mutableStateOf<NavKey>(currentTop) }
    LaunchedEffect(currentTop) { prevTop = currentTop }

    val involvesSubmenu = isSubmenu(currentTop) || isSubmenu(prevTop)

    // Üst seviye sekmeler = ORİJİNAL klasik slide (köşe kırpma / dim scrim YOK).
    // Alt menüler (Theme / Player / About) = YENİ Miuix animasyonu (squircle köşe kırpma + dim scrim
    // + predictive back). Bunlar metadata'sız bırakıldığı için global Miuix varsayılan slide'ı
    // kullanır; corner clip + dim ise aşağıdaki transitionEffects ile yalnızca alt menü
    // geçişlerinde devreye girer.
    val transitionEffects = remember(involvesSubmenu) {
        if (involvesSubmenu) {
            NavDisplayTransitionEffects(
                enableCornerClip = true,
                dimAmount = 0.5f,
                blockInputDuringTransition = true,
                popDirectionFollowsSwipeEdge = true,
            )
        } else {
            NavDisplayTransitionEffects(
                enableCornerClip = false,
                dimAmount = 0f,
                blockInputDuringTransition = false,
            )
        }
    }

    val entryProvider = remember(backStack) {
        entryProvider<NavKey> {
            // --- ÜST SEVİYE SEKMELER: orijinal klasik slide ---
            entry<MusicRoute>(metadata = originalTabTransition) {
                MusicScreen(
                    playerViewModel = playerViewModel,
                    onTrackClick = { tracks, index -> onTrackClick(tracks, index, null) },
                    hasMiniPlayer = hasMiniPlayer
                )
            }
            entry<PlaylistsRoute>(metadata = originalTabTransition) {
                PlaylistScreen(
                    onTrackClick = onTrackClick,
                    onPlaylistRemixClick = onPlaylistRemixClick,
                    hasMiniPlayer = hasMiniPlayer
                )
            }
            entry<SettingsRoute>(metadata = originalTabTransition) {
                SettingsScreen(
                    playerViewModel = playerViewModel,
                    onOpenThemeSettings = { backStack.add(ThemeRoute) },
                    onOpenPlayerSettings = { backStack.add(PlayerRoute) },
                    onOpenAbout = { backStack.add(AboutRoute) }
                )
            }

            // --- ALT MENÜLER: metadata YOK → YENİ Miuix animasyonu (corner clip + dim) ---
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

    Box(modifier = modifier) {
        NavDisplay(
            entries = entries,
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            transitionEffects = transitionEffects,
        )
    }
}
