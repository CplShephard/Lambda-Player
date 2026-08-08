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
object StatsRoute : NavKey

// Ana sekmelerin sıralaması — orijinal koddaki `destinations` listesiyle birebir aynı.
// Yön buna göre hesaplanıyor: hedef index kaynaktan büyükse ileri (Sol), küçükse geri (Sağ).
private val tabOrder = listOf(MusicRoute, PlaylistsRoute, SettingsRoute)

// ÖNEMLİ: Scene<T>.entries.last().contentKey, navigation3-runtime'ın DEFAULT
// contentKeyFactory'sine göre `key.toString()` sonucudur — `MusicRoute` gibi bir NavKey
// objesiyle REFERANS/DATA-CLASS eşitliğiyle (`==`) asla eşleşmez (biri String, diğeri
// object). Bu yüzden karşılaştırmayı `toString()` üzerinden yapıyoruz — `object MusicRoute`
// için varsayılan `toString()` sınıfın tam adını döndürür, bu kararlı ve benzersizdir,
// tıpkı contentKey'in zaten kullandığı değerin kendisi gibi.
private fun tabIndexOf(contentKey: Any?): Int =
    tabOrder.indexOfFirst { it.toString() == contentKey?.toString() }

// MADDE 1 — Musics/Playlists/Settings İÇİN SPRİNG ANİMASYONU İPTAL EDİLDİ.
// Bir önceki turda InstallerX'ten alınan scale/paralaks spring (PageTransitions.enterPush)
// kullanılıyordu; kullanıcı isteğiyle eski klasik slide+fade geçişine dönüldü.
// Yön DİNAMİK: `Scene.entries.lastOrNull()?.contentKey` üzerinden hesaplanır.
// İleri = sağdan giriş / sola çıkış; geri = tersi.
private val originalSlideSpec = spring<IntOffset>(
    dampingRatio = 0.8f,
    stiffness = 380f,
)
private val originalFadeSpec = spring<Float>(
    dampingRatio = 1f,
    stiffness = 380f,
)

private val originalTabTransition: Map<String, Any> =
    NavDisplay.transitionSpec {
        val fromIdx = tabIndexOf(initialState.entries.lastOrNull()?.contentKey)
        val toIdx = tabIndexOf(targetState.entries.lastOrNull()?.contentKey)
        val forward = toIdx >= fromIdx
        if (forward) {
            ContentTransform(
                slideInHorizontally(initialOffsetX = { it }, animationSpec = originalSlideSpec) + fadeIn(originalFadeSpec),
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = originalSlideSpec) + fadeOut(originalFadeSpec),
            )
        } else {
            ContentTransform(
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = originalSlideSpec) + fadeIn(originalFadeSpec),
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = originalSlideSpec) + fadeOut(originalFadeSpec),
            )
        }
    } + NavDisplay.popTransitionSpec {
        val fromIdx = tabIndexOf(initialState.entries.lastOrNull()?.contentKey)
        val toIdx = tabIndexOf(targetState.entries.lastOrNull()?.contentKey)
        val forward = toIdx >= fromIdx
        if (forward) {
            ContentTransform(
                slideInHorizontally(initialOffsetX = { it }, animationSpec = originalSlideSpec) + fadeIn(originalFadeSpec),
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = originalSlideSpec) + fadeOut(originalFadeSpec),
            )
        } else {
            ContentTransform(
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = originalSlideSpec) + fadeIn(originalFadeSpec),
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = originalSlideSpec) + fadeOut(originalFadeSpec),
            )
        }
    }

private fun isSubmenu(key: NavKey?): Boolean =
    key is ThemeRoute || key is PlayerRoute || key is AboutRoute || key is StatsRoute

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

    // InstallerX Revived'ın Navigator.push/pop debounce'unun portu — bkz. SubmenuNavGuard.kt.
    // Sadece Theme/Player/About (submenu) geçişlerine uygulanır.
    val submenuGuard = remember { SubmenuNavGuard() }

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
                // KAPANIRKEN KÖŞE YUVARLAKLIĞI TUTARSIZLIĞI FIX: Bu bayrak, hangi köşelerin
                // yuvarlanacağını "son predictive-back swipe'ının hangi kenardan başladığına"
                // (lastSwipeEdge) göre belirliyor. Ama biz gerçek edge-swipe gesture'ı
                // KULLANMIYORUZ — geri gitme geri tuşu/onBack() çağrısıyla (programatik)
                // tetikleniyor. lastSwipeEdge hiçbir zaman kullanıcı girdisinden anlamlı bir
                // değer almadığı için, kapanış animasyonundaki köşe yuvarlaklığı SS'lerde
                // görüldüğü gibi tutarsız kalıyordu (bazen açılışla aynı köşeler yuvarlak,
                // bazen ters/düz). Kapatınca artık her zaman açılışla AYNI köşe kırpma
                // yönünü kullanıyor.
                popDirectionFollowsSwipeEdge = false,
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
                    onOpenThemeSettings = {
                        submenuGuard.push(currentTop, ThemeRoute) { backStack.add(ThemeRoute) }
                    },
                    onOpenPlayerSettings = {
                        submenuGuard.push(currentTop, PlayerRoute) { backStack.add(PlayerRoute) }
                    },
                    onOpenAbout = {
                        submenuGuard.push(currentTop, AboutRoute) { backStack.add(AboutRoute) }
                    },
                    onOpenStats = {
                        submenuGuard.push(currentTop, StatsRoute) { backStack.add(StatsRoute) }
                    }
                )
            }

            // --- ALT MENÜLER: metadata YOK → YENİ Miuix animasyonu (corner clip + dim) ---
            entry<ThemeRoute> {
                ThemeSettingsScreen(onBack = { submenuGuard.pop { backStack.removeAt(backStack.lastIndex) } })
            }
            entry<PlayerRoute> {
                PlayerSettingsScreen(onBack = { submenuGuard.pop { backStack.removeAt(backStack.lastIndex) } })
            }
            entry<AboutRoute> {
                AboutSettingsScreen(onBack = { submenuGuard.pop { backStack.removeAt(backStack.lastIndex) } })
            }
            entry<StatsRoute> {
                dev.shephard.player.ui.screens.StatsScreen(
                    playerViewModel = playerViewModel,
                    onBack = { submenuGuard.pop { backStack.removeAt(backStack.lastIndex) } }
                )
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
