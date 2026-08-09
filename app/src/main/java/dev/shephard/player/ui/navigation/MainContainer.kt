package dev.shephard.player.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import top.yukonga.miuix.kmp.basic.Scaffold
import dev.shephard.player.ui.miuix.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.MiniPlayer
import dev.shephard.player.ui.glass.FloatingBottomBar
import dev.shephard.player.ui.glass.FloatingBottomBarItem
import dev.shephard.player.ui.glass.FloatingBottomBarMode
import dev.shephard.player.ui.glass.LocalAppBackdrop
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.isLiquidGlassSupported
import dev.shephard.player.ui.glass.miuixBlurSurface
import dev.shephard.player.ui.glass.rememberAppBlurBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.screens.NowPlayingSheet

// Açılış: yumuşak yaylı kayma
private val nowPlayingEnterSpring = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 180f
)
// Kapanış: fazla overshoot olmadan hızlı ve smooth
private val nowPlayingExitSpring = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 480f
)

@Composable
fun MainContainer(
    playerViewModel: PlayerViewModel = viewModel(),
    initialAudioUri: android.net.Uri? = null
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val languageCode by prefs.language.collectAsState(initial = "en")
    val strings = remember(languageCode) { stringsFor(languageCode) }
    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = 0.55f)

    val blurEnabled = LocalBlurEnabled.current

    // TWO separate backdrops, on purpose.
    //
    // `backgroundBackdrop` captures ONLY the wallpaper/background layer. Every blurred
    // surface that lives *inside* the page content (cards, sheets, dialogs, the header,
    // the mini player) samples this one.
    //
    // `contentBackdrop` captures the scrolling page content and is sampled ONLY by the
    // floating dock, which is drawn outside that content.
    //
    // Previously there was a single backdrop that wrapped the page content while surfaces
    // inside that very content sampled it — a layer reading itself. That recursion is what
    // crashed the app as soon as the Liquid Glass switch was turned on.
    val backgroundBackdrop = rememberAppBlurBackdrop(blurEnabled)
    val contentBackdrop = rememberAppBlurBackdrop(blurEnabled)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalAppBackdrop provides backgroundBackdrop,
        LocalContentBackdrop provides contentBackdrop,
    ) {
        val backStack = remember { mutableStateListOf<NavKey>(MusicRoute) }
        var showNowPlaying by remember { mutableStateOf(false) }

        val currentRouteKey = backStack.last()
        val currentRoute: String? = when (currentRouteKey) {
            is MusicRoute -> Destination.Music.route
            is PlaylistsRoute -> Destination.Playlists.route
            is SettingsRoute -> Destination.Settings.route
            is ThemeRoute -> SettingsRoutes.Theme
            is PlayerRoute -> SettingsRoutes.Player
            is AboutRoute -> SettingsRoutes.About
            else -> null
        }
        // MADDE 3 — Submenü kapanış (exit) animasyonu boyunca da dock/mini submenu'nün
        // ALTINDA kalmalı. Route pop edilince currentRoute anında ana sekmeye döner; o sırada
        // submenu hâlâ ekrandan kayıyor olabilir. Bir önceki route'u hatırlayıp, eğer geçiş
        // "submenu açıkken" gerçekleşiyorsa dock/mini'yi submenu'nün altında tutuyoruz.
        var prevRoute by remember { mutableStateOf<String?>(currentRoute) }
        // Submenu çıkış animasyonu ~500ms sürüyor; o süre boyunca dock/mini submenu'nün
        // altında kalsın diye prevRoute'u hafifçe gecikmeli güncelliyoruz. Böylece route
        // pop edilince dock/mini animasyonun ortasında öne fırlamaz.
        LaunchedEffect(currentRoute) {
            val leavingSubmenu =
                (prevRoute == SettingsRoutes.Theme || prevRoute == SettingsRoutes.Player || prevRoute == SettingsRoutes.About) &&
                (currentRoute != SettingsRoutes.Theme && currentRoute != SettingsRoutes.Player && currentRoute != SettingsRoutes.About)
            if (leavingSubmenu) {
                kotlinx.coroutines.delay(550)
            }
            prevRoute = currentRoute
        }
        val isBottomRoute = bottomNavDestinations.any { it.route == currentRoute } || currentRoute == null
        // Submenu geçişindeysek (açılırken ya da kapanırken) dock/mini alt katmanda kalır.
        val submenuInvolved =
            (currentRoute == SettingsRoutes.Theme || currentRoute == SettingsRoutes.Player || currentRoute == SettingsRoutes.About) ||
            (prevRoute == SettingsRoutes.Theme || prevRoute == SettingsRoutes.Player || prevRoute == SettingsRoutes.About)

        // Do not collect the whole PlayerUiState at the root: position/progress changes
        // every 500ms while playing. Only the boolean that affects page bottom padding is
        // observed here; the MiniPlayer subtree collects the full state by itself.
        val hasMiniPlayer by remember(playerViewModel) {
            playerViewModel.uiState
                .map { it.currentTrack != null }
                .distinctUntilChanged()
        }.collectAsState(initial = false)

        LaunchedEffect(initialAudioUri) {
            if (initialAudioUri != null) {
                playerViewModel.playExternalUri(initialAudioUri)
                delay(120)
                showNowPlaying = true
            }
        }

        // Back: collapse player first, then pop nav stack.
        BackHandler(enabled = showNowPlaying || currentRoute != Destination.Music.route) {
            when {
                showNowPlaying -> showNowPlaying = false
                !isBottomRoute -> if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                else -> while (backStack.size > 1 && backStack.last() != MusicRoute) backStack.removeAt(backStack.lastIndex)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixAppTheme.colorScheme.background)
        ) {
            // Wallpaper background — her iki ekranın arkasında sabit.
            // This whole background block is recorded into `backgroundBackdrop`, which is
            // what in-content glass surfaces sample. It contains no glass surfaces itself,
            // so there is no way for it to reference itself.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (backgroundBackdrop != null) Modifier.layerBackdrop(backgroundBackdrop)
                        else Modifier
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixAppTheme.colorScheme.background)
                )
                if (wallpaper.isNotEmpty()) {
                    AsyncImage(
                        model = wallpaper,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // ÖNEMLİ: wallpaperBrightness artık DOĞRUDAN "parlaklık" anlamında
                    // saklanıyor (0 = en karanlık, 1 = en parlak/en az karartılmış). Karartma
                    // katmanının kendisi bir "dim overlay" olduğu için opaklığı brightness'ın
                    // TERSİ olmalı — bu çeviri kasıtlı olarak burada, TEK noktada yapılıyor
                    // (ayarlar ekranındaki slider state'inde değil), böylece slider'ın kendisi
                    // hiçbir ters çevirme mantığı taşımıyor ve "her bırakışta zıplama" sorunu
                    // bir daha oluşamaz.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiuixAppTheme.colorScheme.background.copy(alpha = 1f - wallpaperBrightness))
                    )
                }
            }

            // Uygulama ağacını artık NowPlaying açılınca kaldırmıyoruz.
            // Böylece playlist detail ekranının state'i korunuyor; sheet kapanınca aynı yerde kalır.
            //
            // MADDE 4 — eski "LAMBDA PLAYER" marka kartı (BrandHeader) burada `Scaffold`'un
            // `topBar`'ı olarak SABİT bir yükseklikte render ediliyordu. Theme/Player/About
            // sayfalarına geçilince bu alan aniden değişiyor (kart kayboluyor/beliriyor), bu da
            // page transition'ın "kartın altına ışınlanıp kapanma" gibi bozuk görünmesine yol
            // açıyordu — kart page transition'a kendini bir ekran sınırı gibi dayatıyordu.
            // Kullanıcı isteğiyle kart TAMAMEN kaldırıldı: artık Music/Playlists/Settings de
            // Theme/Playback/About ile birebir aynı CollapsingTopBar desenini kendi
            // içeriklerinde kullanıyor (bkz. MusicScreen/PlaylistScreen/SettingsScreen).
            // Scaffold'un topBar'ı yok, bu yüzden innerPadding.top hep 0 — her ekran kendi
            // başlığını ve status bar boşluğunu kendi yönetiyor.
            Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        // Always transparent: the background (and wallpaper) is painted by
                        // the backdrop layer above, so an opaque Scaffold here would hide it
                        // and leave every glass surface sampling a flat colour.
                        containerColor = Color.Transparent,
                        // İçerik (BgEffect arkaplanı, wallpaper) status bar'ın ARKASINA da
                        // uzanmalı — her ekran zaten kendi status bar boşluğunu SmallTopAppBar
                        // üzerinden kendi yönetiyor.
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            // MADDE 7 (bu tur) — Dock ve mini player, NavGraph'dan ÖNCE (alt katmanda)
                            // çizilir ve zIndex'i `isBottomRoute`'a göre ayarlanır:
                            //  - Ana sekmeler (Music/Playlists/Settings) + playlistdetail: isBottomRoute
                            //    true → zIndex 1 → dock/mini içeriğin ÜZERİNDE görünür (eskisi gibi).
                            //  - Gerçek submenüler (Theme/Playback/About): isBottomRoute false →
                            //    zIndex 0 → NavGraph üstte çizilir; opak submenu dock/mini'yi ÖRTER
                            //    (submenu onların ÜZERİNE gelir, InstallerX'teki gibi). Dock/mini
                            //    kaybolmaz; sadece opak submenu'nün altında kalır, submenu kapanınca
                            //    anında geri görünür.
                            // MADDE 10 — Dock ve mini player, NowPlayingSheet açılınca GİZLENMEZ;
                            // sheet zaten en üstte çizildiği için ikisini örter (altında kalırlar).
                            // MADDE 3 — Playlistdetail ISTİSNAdır: o Playlists ana sekmesinin
                            // (isBottomRoute) İÇİNDE render edildiği için dock/mini onun ÜZERİNDE
                            // kalır. Theme/Playback/About gibi gerçek submenu'lerde (isBottomRoute=false)
                            // ise zIndex 0 → NavGraph üstte çizilir, dock/mini opak submenu'nün
                            // altında kalır (submenu onların üzerine gelir).
                            Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        // MADDE 3 — Submenu geçişindeyken (açılış/kapanış) dock/mini
                                        // alt katmanda kalır ki opak submenu onları örtsün.
                                        .zIndex(if (isBottomRoute && !submenuInvolved) 1f else 0f)
                                ) {
                                    MiniPlayerHost(
                                        playerViewModel = playerViewModel,
                                        visible = hasMiniPlayer,
                                        onOpenNowPlaying = { showNowPlaying = true }
                                    )
                                    FloatingDock(
                                        currentRoute = currentRoute,
                                        onNavigate = { destination ->
                                            val key = when (destination) {
                                                Destination.Music -> MusicRoute
                                                Destination.Playlists -> PlaylistsRoute
                                                Destination.Settings -> SettingsRoute
                                                else -> MusicRoute
                                            }
                                            if (backStack.last() != key) {
                                                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                                                    backStack.clear()
                                                    backStack.add(key)
                                                }
                                            }
                                        }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }

                            NavGraph(
                                backStack = backStack,
                                playerViewModel = playerViewModel,
                                // Capture the page content into the CONTENT backdrop so the
                                // dock blurs what scrolls underneath it. The dock is drawn
                                // outside this layer, so there is no feedback loop. Glass
                                // surfaces inside the page sample the background backdrop
                                // instead — never this one.
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (contentBackdrop != null) Modifier.layerBackdrop(contentBackdrop)
                                        else Modifier
                                    ),
                                hasMiniPlayer = hasMiniPlayer,
                                // Bir müziğe basınca NowPlayingSheet AÇILMAZ — sadece çalma
                                // başlar ve altta mini player belirir. NowPlayingSheet'e
                                // mini player'a basarak ulaşılır.
                                onTrackClick = { tracks, index, playlistName ->
                                    playerViewModel.setQueueAndPlay(tracks, index, playlistName)
                                },
                                onPlaylistRemixClick = { tracks, playlistName ->
                                    playerViewModel.setQueueAndPlayRemixed(tracks, playlistName)
                                }
                            )
                        }
                    }

            AnimatedVisibility(
                visible = showNowPlaying,
                // MADDE (köşe yarıçapı zamanlaması) — açılış kaydırması (slide) artık
                // NowPlayingSheet içinde `dragOffset` üzerinden yapılıyor; böylece köşe
                // yarıçapı sheet TAM oturana kadar 30dp kalıyor, animasyon bitince 0'a
                // iniyor. Burada sadece yumuşak bir fade kalıyor.
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(0)),
                modifier = Modifier.fillMaxSize()
            ) {
                NowPlayingSheet(
                    playerViewModel = playerViewModel,
                    onDismiss = { showNowPlaying = false }
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerHost(
    playerViewModel: PlayerViewModel,
    visible: Boolean,
    onOpenNowPlaying: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(0.85f, 300f)
        ),
        exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        val playerState by playerViewModel.uiState.collectAsState()
        MiniPlayer(
            state = playerState,
            progressFlow = playerViewModel.progress,
            onClick = onOpenNowPlaying,
            onPlayPauseClick = { playerViewModel.togglePlayPause() },
            onNextClick = { playerViewModel.skipToNext() },
            onPreviousClick = { playerViewModel.skipToPrevious() }
        )
    }
}

@Composable
private fun FloatingDock(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    val blurOn = LocalBlurEnabled.current
    // The dock lives outside the page content, so it is the one component allowed to
    // sample the content backdrop.
    val backdrop = LocalContentBackdrop.current

    // Same three-way mode selection InstallerX uses:
    //  - LiquidGlass: full AGSL pipeline (lens refraction, chromatic aberration, bloom) on API 33+
    //  - Blur:        plain gaussian backdrop blur on API 31–32
    //  - None:        opaque pill, no backdrop sampling
    val mode = when {
        blurOn && backdrop != null && isLiquidGlassSupported -> FloatingBottomBarMode.LiquidGlass
        blurOn && backdrop != null -> FloatingBottomBarMode.Blur
        else -> FloatingBottomBarMode.None
    }

    // MADDE 2 — Submenü (Theme/Playback/About) açıkken currentRoute bir alt menü yoludur;
    // bottomNavDestinations içinde eşleşmezdi ve `.let { if (it < 0) 0 else it }` yüzünden
    // dock Music sekmesini işaretliyordu. Submenü açıkken dock Settings'i işaretlemeli.
    val selectedIndex = when {
        currentRoute == Destination.Music.route ||
            currentRoute == Destination.Playlists.route ||
            currentRoute == Destination.Settings.route ->
            bottomNavDestinations.indexOfFirst { it.route == currentRoute }
        // Theme/Player/About gibi alt menülerde Settings seçili görünsün.
        else -> bottomNavDestinations.indexOfFirst { it.route == Destination.Settings.route }
    }.let { if (it < 0) 0 else it }
    val strings = LocalStrings.current

    // KRİTİK: Daha önce backdrop == null (Liquid Glass kapalı ya da desteklenmiyor)
    // durumunda TAMAMEN AYRI bir composable (NonBlurDock — farklı boyut, etiketsiz,
    // farklı highlight stili) kullanılıyordu. Bu da "Liquid Glass kapanınca dock eski/
    // farklı bir docka dönüşüyor" şikayetinin sebebiydi. InstallerX'te dock HER ZAMAN
    // aynı FloatingBottomBar component'idir, sadece mode (LiquidGlass/Blur/None) değişir
    // — görsel iskelet (pill şekli, boyut, etiketli sekmeler, drag ile geçiş, seçim
    // highlight'ı) her modda birebir aynı kalır. Bu yüzden NonBlurDock tamamen kaldırıldı;
    // backdrop null olduğunda gerçek bir Backdrop tipi gerektiği için (drawBackdrop API'si
    // non-null ister) boş/no-op bir dummy backdrop veriyoruz — None modda zaten hiç
    // sample edilmiyor, sadece tip uyumluluğu için var.
    val dummyBackdrop = rememberLayerBackdrop()
    val effectiveBackdrop = backdrop ?: dummyBackdrop

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingBottomBar(
            selectedIndex = { selectedIndex },
            onSelected = { index -> onNavigate(bottomNavDestinations[index]) },
            backdrop = effectiveBackdrop,
            tabsCount = bottomNavDestinations.size,
            mode = mode
        ) {
            bottomNavDestinations.forEachIndexed { index, dest ->
                val selected = index == selectedIndex
                val label = when (dest) {
                    Destination.Music -> strings.music
                    Destination.Playlists -> strings.playlists
                    Destination.Settings -> strings.settings
                }
                FloatingBottomBarItem(
                    onClick = { onNavigate(dest) },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label
                    )
                    Text(
                        text = label,
                        style = MiuixAppTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
