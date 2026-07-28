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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.MiniPlayer
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.glass.FloatingBottomBar
import dev.shephard.player.ui.glass.FloatingBottomBarItem
import dev.shephard.player.ui.glass.FloatingBottomBarMode
import dev.shephard.player.ui.glass.LocalAppBackdrop
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.blurSurface
import dev.shephard.player.ui.glass.isLiquidGlassSupported
import dev.shephard.player.ui.glass.rememberAppBlurBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
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
        val navController = rememberNavController()
        var showNowPlaying by remember { mutableStateOf(false) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val playerState by playerViewModel.uiState.collectAsState()

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
                            BrandHeader(currentRoute = currentRoute)
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            NavGraph(
                                navController = navController,
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
                                hasMiniPlayer = playerState.currentTrack != null,
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
                                AnimatedVisibility(
                                    visible = playerState.currentTrack != null,
                                    enter = fadeIn(tween(200)) + slideInVertically(
                                        initialOffsetY = { it / 2 },
                                        animationSpec = spring(0.85f, 300f)
                                    ),
                                    exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 })
                                ) {
                                    MiniPlayer(
                                        state = playerState,
                                        onClick = { showNowPlaying = true },
                                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                                        onNextClick = { playerViewModel.skipToNext() },
                                        onPreviousClick = { playerViewModel.skipToPrevious() }
                                    )
                                }

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
    val headerShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .then(
                if (blurOn) {
                    Modifier.blurSurface(enabled = true, shape = headerShape)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
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
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = dev.shephard.player.ui.theme.BrandFontFamily,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (versionName.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = versionName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = dev.shephard.player.ui.theme.BrandFontFamily,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (sectionTitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (backdrop != null) {
            FloatingBottomBar(
                selectedIndex = { selectedIndex },
                onSelected = { index -> onNavigate(bottomNavDestinations[index]) },
                backdrop = backdrop,
                tabsCount = bottomNavDestinations.size,
                mode = mode
            ) {
                bottomNavDestinations.forEachIndexed { index, dest ->
                    val selected = index == selectedIndex
                    FloatingBottomBarItem(
                        onClick = { onNavigate(dest) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.label
                        )
                    }
                }
            }
        } else {
            // Blur disabled or unsupported: keep the dock fully functional without a backdrop.
            NonBlurDock(
                selectedIndex = selectedIndex,
                onNavigate = onNavigate
            )
        }
    }
}

/**
 * Fallback dock used when blur is switched off (or the device predates API 31), so the app
 * never depends on a backdrop being present. Visually a plain Material 3 pill.
 */
@Composable
private fun NonBlurDock(
    selectedIndex: Int,
    onNavigate: (Destination) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bottomNavDestinations.forEachIndexed { index, dest ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { onNavigate(dest) }
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = dest.label,
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
