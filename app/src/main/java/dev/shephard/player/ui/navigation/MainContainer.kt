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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiuixAppTheme.colorScheme.background.copy(alpha = wallpaperBrightness))
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
                        // ÖNEMLİ: Miuix Scaffold'un varsayılanı
                        // (WindowInsets.systemBars.union(displayCutout)) content slotuna
                        // OTOMATİK olarak status bar kadar üst padding ekliyordu — topBar boş
                        // olsa bile (About gibi topBar'sız sayfalarda). Bu yüzden içerik
                        // (BgEffect arkaplanı, wallpaper) hiçbir zaman status bar'ın ARKASINA
                        // uzanamıyordu; o bölge boş/varsayılan renkte kalıp InstallerX'teki gibi
                        // "arkaplan status bar'ın içinden de görünsün" hissini bozuyordu.
                        // Insets'i sıfırlıyoruz — her ekran zaten kendi statusBarsPadding()'ini
                        // (veya BrandHeader gibi kendi insets hesaplamasını) kendisi yönetiyor.
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            if (isBottomRoute) BrandHeader(currentRoute = currentRoute)
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                            ) {
                                // MADDE 5 — Settings alt sayfalarına (Theme / Player /
                                // About) girince dock zaten gizleniyordu ama müzik
                                // çalar pop-up'ı (mini player) orada da görünmeye devam
                                // ediyordu. Artık mini player da sadece ana sekmelerde
                                // gösteriliyor; alt sayfalarda ekran tamamen içeriğe
                                // kalıyor.
                                MiniPlayerHost(
                                    playerViewModel = playerViewModel,
                                    visible = hasMiniPlayer && isBottomRoute,
                                    onOpenNowPlaying = { showNowPlaying = true }
                                )

                                if (isBottomRoute) {
                                    FloatingDock(
                                        currentRoute = currentRoute,
                                onNavigate = { destination ->
                                    val key = when (destination) {
                                        Destination.Music -> MusicRoute
                                        Destination.Playlists -> PlaylistsRoute
                                        Destination.Settings -> SettingsRoute
                                        else -> MusicRoute
                                    }
                                    backStack.add(key)
                                }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
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
private fun BrandHeader(currentRoute: String?) {
    val strings = LocalStrings.current
    val sectionTitle = when (currentRoute) {
        Destination.Music.route -> strings.music
        Destination.Playlists.route -> strings.playlists
        Destination.Settings.route -> strings.settings
        else -> null
    }

    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    val blurOn = LocalBlurEnabled.current
    val headerBackdrop = LocalAppBackdrop.current
    val headerShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .then(
                if (blurOn && headerBackdrop != null) {
                    // Subtle title-card blur: lighter than MiniPlayer/dock, just enough to lift it.
                    Modifier.miuixBlurSurface(
                        backdrop = headerBackdrop,
                        shape = headerShape,
                        blurRadius = 14f,
                        tintAlpha = 0.46f,
                        fallbackColor = MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    )
                } else {
                    Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f), headerShape)
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = strings.appName.uppercase(),
                    style = MiuixAppTheme.typography.titleLarge.copy(
                        fontFamily = dev.shephard.player.ui.theme.BrandFontFamily,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MiuixAppTheme.colorScheme.onBackground
                )
                if (versionName.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = versionName,
                        style = MiuixAppTheme.typography.titleLarge.copy(
                            fontFamily = dev.shephard.player.ui.theme.BrandFontFamily,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MiuixAppTheme.colorScheme.primary
                    )
                }
            }
            if (sectionTitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sectionTitle,
                    style = MiuixAppTheme.typography.titleMedium,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
                        style = MiuixAppTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
