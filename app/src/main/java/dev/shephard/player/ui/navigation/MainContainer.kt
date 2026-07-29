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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.isLiquidGlassSupported
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
    // `backgroundBackdrop` sadece sabit wallpaper katmanını kaydeder (ucuz, statik) — hem
    // header hem de dock artık bunu sample ediyor. Eskiden ayrıca bir `contentBackdrop` de
    // vardı ve NavGraph'ın (yani aktif sayfanın TÜM içeriğinin — LazyColumn'lar, animasyonlar)
    // her frame bir GraphicsLayer'a kaydedilmesine sebep oluyordu; bu da Liquid Glass açıkken
    // ciddi bir performans maliyetiydi. InstallerX'te de dock'un arkasında sayfa içeriği değil
    // sabit zemin göründüğü için, bu katmana artık hiç ihtiyaç yok — kaldırıldı.
    val backgroundBackdrop = rememberAppBlurBackdrop(blurEnabled)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalAppBackdrop provides backgroundBackdrop,
    ) {
        val navController = rememberNavController()
        var showNowPlaying by remember { mutableStateOf(false) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isBottomRoute = bottomNavDestinations.any { it.route == currentRoute } || currentRoute == null

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
                !isBottomRoute -> navController.popBackStack()
                else -> navController.popBackStack(Destination.Music.route, false)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                        .background(MaterialTheme.colorScheme.background)
                )
                if (wallpaper.isNotEmpty()) {
                    AsyncImage(
                        model = wallpaper,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = wallpaperBrightness))
                    )
                }
            }

            // Uygulama ağacını artık NowPlaying açılınca kaldırmıyoruz.
            // Böylece playlist detail ekranının state'i korunuyor; sheet kapanınca aynı yerde kalır.
            Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        // Always transparent: the background (and wallpaper) is painted by
                        // the backdrop layer above, so an opaque Scaffold here would hide it
                        // and leave every glass surface sampling a flat colour.
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        topBar = {
                            if (isBottomRoute) BrandHeader(currentRoute = currentRoute)
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            NavGraph(
                                navController = navController,
                                playerViewModel = playerViewModel,
                                modifier = Modifier.fillMaxSize(),
                                hasMiniPlayer = hasMiniPlayer,
                                onTrackClick = { tracks, index, playlistName ->
                                    playerViewModel.setQueueAndPlay(tracks, index, playlistName)
                                    showNowPlaying = true
                                },
                                onPlaylistRemixClick = { tracks, playlistName ->
                                    playerViewModel.setQueueAndPlayRemixed(tracks, playlistName)
                                    showNowPlaying = true
                                }
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                            ) {
                                MiniPlayerHost(
                                    playerViewModel = playerViewModel,
                                    visible = hasMiniPlayer,
                                    onOpenNowPlaying = { showNowPlaying = true }
                                )

                                if (isBottomRoute) {
                                    FloatingDock(
                                        currentRoute = currentRoute,
                                        onNavigate = { destination ->
                                            navController.navigate(destination.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }

            AnimatedVisibility(
                visible = showNowPlaying,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = nowPlayingEnterSpring
                ) + fadeIn(tween(120)),
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
            onClick = onOpenNowPlaying,
            onPlayPauseClick = { playerViewModel.togglePlayPause() },
            onNextClick = { playerViewModel.skipToNext() },
            onPreviousClick = { playerViewModel.skipToPrevious() }
        )
    }
}

@Composable
private fun BrandHeader(currentRoute: String?) {
    val strings = LocalStrings.current
    val sectionTitle = when (currentRoute) {
        Destination.Music.route -> strings.music
        Destination.Playlists.route -> strings.playlists
        Destination.Settings.route -> strings.settings
        else -> strings.appName
    }

    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    // InstallerX header tarzı: kartsız, blursuz, büyük sol-hizalı sayfa başlığı — sayfanın
    // en üstünde, arka planla aynı zeminde durur. Marka adı + versiyon ("Lambda Player X.X")
    // ana başlığın altında küçük bir alt-satır olarak duruyor, düzeni bozmadan.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)
    ) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = dev.shephard.player.ui.theme.BrandFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (versionName.isNotEmpty()) "${strings.appName} $versionName" else strings.appName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FloatingDock(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    val blurOn = LocalBlurEnabled.current
    // PERFORMANS FIX: Daha önce dock, `LocalContentBackdrop`'ı (aktif sayfanın TAMAMININ —
    // LazyColumn'lar, animasyonlar, her recompose — her frame bir GraphicsLayer'a kaydedildiği
    // pahalı katman) sample ediyordu. Bu, "Liquid Glass'ı açınca uygulama çok kasıyor"
    // şikayetinin doğrudan kaynağıydı. InstallerX'te dock'un blur'u sayfa İÇERİĞİNİ değil,
    // SABİT arka planı (wallpaper) yansıtır — dock'un arkasında kayan liste değil, duran zemin
    // görünür. `LocalAppBackdrop` zaten bunun için var (sadece wallpaper Box'ını kaydeder,
    // statik, ucuz) ve header zaten bunu kullanıyordu. Dock'u da aynı ucuz katmana geçirdik;
    // artık `contentBackdrop` hiçbir yerde sample edilmiyor, MainContainer'daki NavGraph'a
    // uygulanan `layerBackdrop(contentBackdrop)` da kaldırıldı (bkz. aşağıda).
    val backdrop = LocalAppBackdrop.current

    // Same three-way mode selection InstallerX uses:
    //  - LiquidGlass: full AGSL pipeline (lens refraction, chromatic aberration, bloom) on API 33+
    //  - Blur:        plain gaussian backdrop blur on API 31–32
    //  - None:        opaque pill, no backdrop sampling
    val mode = when {
        blurOn && backdrop != null && isLiquidGlassSupported -> FloatingBottomBarMode.LiquidGlass
        blurOn && backdrop != null -> FloatingBottomBarMode.Blur
        else -> FloatingBottomBarMode.None
    }

    val selectedIndex = bottomNavDestinations
        .indexOfFirst { it.route == currentRoute }
        .let { if (it < 0) 0 else it }
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
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
