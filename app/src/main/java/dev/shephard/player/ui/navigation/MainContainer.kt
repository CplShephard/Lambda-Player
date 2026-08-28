package dev.shephard.player.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.MiniPlayer
import dev.shephard.player.ui.components.MiniPlayerM3
import dev.shephard.player.ui.glass.FloatingBottomBar
import dev.shephard.player.ui.glass.FloatingBottomBarItem
import dev.shephard.player.ui.glass.FloatingBottomBarMode
import dev.shephard.player.ui.glass.LocalAppBackdrop
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.isLiquidGlassSupported
import dev.shephard.player.ui.glass.rememberAppBlurBackdrop
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.screens.NowPlayingSheet
import dev.shephard.player.ui.screens.NowPlayingSheetM3
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem

@OptIn(ExperimentalFoundationApi::class)
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
    val useMiuix by prefs.useMiuix.collectAsState(initial = true)
    val appleFloatingBar by prefs.useAppleFloatingBar.collectAsState(initial = false)
    val lastMainPage by prefs.lastMainPage.collectAsState(initial = 0)

    val backgroundBackdrop = rememberAppBlurBackdrop(blurEnabled)
    val contentBackdrop = rememberAppBlurBackdrop(blurEnabled)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalAppBackdrop provides backgroundBackdrop,
        LocalContentBackdrop provides contentBackdrop,
    ) {
        val backStack = remember { mutableStateListOf<NavKey>(MainRoute) }
        var showNowPlaying by remember { mutableStateOf(false) }

        val pagerState = rememberPagerState(
            initialPage = lastMainPage.coerceIn(0, bottomNavDestinations.size - 1),
            pageCount = { bottomNavDestinations.size },
        )
        val mainPagerState = rememberMainPagerState(pagerState)
        val currentPage = mainPagerState.pagerState.currentPage
        val settledPage = mainPagerState.pagerState.settledPage
        LaunchedEffect(currentPage) {
            mainPagerState.syncPage()
        }
        LaunchedEffect(settledPage) {
            if (lastMainPage != settledPage) {
                prefs.setLastMainPage(settledPage)
            }
        }

        val topKey = backStack.last()
        val submenuOpen = topKey is ThemeRoute || topKey is PlayerRoute ||
            topKey is AboutRoute || topKey is StatsRoute
        var prevSubmenuOpen by remember { mutableStateOf(false) }
        LaunchedEffect(submenuOpen) {
            if (!submenuOpen && prevSubmenuOpen) {
                delay(550)
            }
            prevSubmenuOpen = submenuOpen
        }
        val submenuInvolved = submenuOpen || prevSubmenuOpen

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

        BackHandler(enabled = showNowPlaying || submenuOpen || mainPagerState.selectedPage != 0) {
            when {
                showNowPlaying -> showNowPlaying = false
                submenuOpen -> if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                else -> mainPagerState.animateToPage(0)
            }
        }

        val containerBackground = if (useMiuix) {
            MiuixAppTheme.colorScheme.background
        } else {
            androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerBackground)
        ) {
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
                        .background(containerBackground)
                )
                if (wallpaper.isNotEmpty()) {
                    AsyncImage(
                        model = wallpaper,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 1f - wallpaperBrightness))
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .zIndex(if (!submenuInvolved) 1f else 0f)
                        ) {
                            MiniPlayerHost(
                                playerViewModel = playerViewModel,
                                visible = hasMiniPlayer,
                                useMiuix = useMiuix,
                                onOpenNowPlaying = { showNowPlaying = true }
                            )
                            MainDock(
                                appleStyle = appleFloatingBar,
                                useMiuix = useMiuix,
                                selectedIndex = { mainPagerState.selectedPage },
                                onSelected = { index -> mainPagerState.animateToPage(index) },
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        NavGraph(
                            backStack = backStack,
                            playerViewModel = playerViewModel,
                            useMiuix = useMiuix,
                            mainPagerState = mainPagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (contentBackdrop != null) Modifier.layerBackdrop(contentBackdrop)
                                    else Modifier
                                ),
                            hasMiniPlayer = hasMiniPlayer,
                            onTrackClick = { tracks, index, playlistName ->
                                playerViewModel.setQueueAndPlay(tracks, index, playlistName)
                            },
                            onPlaylistRemixClick = { tracks, playlistName ->
                                playerViewModel.setQueueAndPlayRemixed(tracks, playlistName)
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showNowPlaying,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(0)),
                modifier = Modifier.fillMaxSize()
            ) {
                if (useMiuix) {
                    NowPlayingSheet(
                        playerViewModel = playerViewModel,
                        onDismiss = { showNowPlaying = false }
                    )
                } else {
                    NowPlayingSheetM3(
                        playerViewModel = playerViewModel,
                        onDismiss = { showNowPlaying = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainDock(
    appleStyle: Boolean,
    useMiuix: Boolean,
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
) {
    when {
        !useMiuix -> M3NavigationDock(selectedIndex = selectedIndex, onSelected = onSelected)
        appleStyle -> AppleFloatingDock(selectedIndex = selectedIndex, onSelected = onSelected)
        else -> MiuixNavigationDock(selectedIndex = selectedIndex, onSelected = onSelected)
    }
}

@Composable
private fun M3NavigationDock(
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    androidx.compose.material3.ShortNavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
        arrangement = androidx.compose.material3.ShortNavigationBarArrangement.EqualWeight,
    ) {
        bottomNavDestinations.forEachIndexed { index, dest ->
            val label = when (dest) {
                Destination.Home -> strings.home
                Destination.Music -> strings.music
                Destination.Playlists -> strings.playlists
                Destination.Settings -> strings.settings
            }
            androidx.compose.material3.ShortNavigationBarItem(
                selected = selectedIndex() == index,
                onClick = { onSelected(index) },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = if (selectedIndex() == index) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                    )
                },
                label = { androidx.compose.material3.Text(label) },
            )
        }
    }
}

@Composable
private fun MiuixNavigationDock(
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
) {
    val blurOn = LocalBlurEnabled.current
    val backdrop = LocalContentBackdrop.current
    val blurActive = blurOn && backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixAppTheme.colorScheme.surface
    val strings = LocalStrings.current

    Box(
        modifier = Modifier
            .then(
                if (blurActive && backdrop != null) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        blurRadius = 25f,
                        colors = BlurColors(
                            blendColors = listOf(
                                BlendColorEntry(color = MiuixAppTheme.colorScheme.surface.copy(0.8f)),
                            ),
                        ),
                    )
                } else {
                    Modifier
                }
            )
            .background(barColor)
    ) {
        NavigationBar(color = barColor) {
            bottomNavDestinations.forEachIndexed { index, dest ->
                val label = when (dest) {
                    Destination.Home -> strings.home
                    Destination.Music -> strings.music
                    Destination.Playlists -> strings.playlists
                    Destination.Settings -> strings.settings
                }
                NavigationBarItem(
                    selected = selectedIndex() == index,
                    onClick = { onSelected(index) },
                    icon = if (selectedIndex() == index) dest.selectedIcon else dest.unselectedIcon,
                    label = label,
                )
            }
        }
    }
}

@Composable
private fun AppleFloatingDock(
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
) {
    val blurOn = LocalBlurEnabled.current
    val backdrop = LocalContentBackdrop.current
    val mode = when {
        blurOn && backdrop != null && isLiquidGlassSupported -> FloatingBottomBarMode.LiquidGlass
        blurOn && backdrop != null -> FloatingBottomBarMode.Blur
        else -> FloatingBottomBarMode.None
    }
    val strings = LocalStrings.current
    val dummyBackdrop = rememberLayerBackdrop()
    val effectiveBackdrop = backdrop ?: dummyBackdrop

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingBottomBar(
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            backdrop = effectiveBackdrop,
            tabsCount = bottomNavDestinations.size,
            mode = mode
        ) {
            bottomNavDestinations.forEachIndexed { index, dest ->
                val selected = index == selectedIndex()
                val label = when (dest) {
                    Destination.Home -> strings.home
                    Destination.Music -> strings.music
                    Destination.Playlists -> strings.playlists
                    Destination.Settings -> strings.settings
                }
                FloatingBottomBarItem(
                    onClick = { onSelected(index) },
                ) {
                    dev.shephard.player.ui.miuix.Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label
                    )
                    dev.shephard.player.ui.miuix.Text(
                        text = label,
                        style = MiuixAppTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerHost(
    playerViewModel: PlayerViewModel,
    visible: Boolean,
    useMiuix: Boolean,
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
        if (useMiuix) {
            MiniPlayer(
                state = playerState,
                progressFlow = playerViewModel.progress,
                onClick = onOpenNowPlaying,
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onNextClick = { playerViewModel.skipToNext() },
                onPreviousClick = { playerViewModel.skipToPrevious() }
            )
        } else {
            MiniPlayerM3(
                state = playerState,
                progressFlow = playerViewModel.progress,
                onClick = onOpenNowPlaying,
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onNextClick = { playerViewModel.skipToNext() },
                onPreviousClick = { playerViewModel.skipToPrevious() }
            )
        }
    }
}
