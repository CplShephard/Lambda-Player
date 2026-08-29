// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar as M3NavigationBar
import androidx.compose.material3.NavigationBarItem as M3NavigationBarItem
import androidx.compose.material3.Text as M3Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.MiniPlayer
import dev.shephard.player.ui.components.MiniPlayerM3
import dev.shephard.player.ui.glass.FloatingBottomBar
import dev.shephard.player.ui.glass.FloatingBottomBarDefaults
import dev.shephard.player.ui.glass.FloatingBottomBarItem
import dev.shephard.player.ui.glass.FloatingBottomBarMode
import dev.shephard.player.ui.glass.LocalAppBackdrop
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.isLiquidGlassSupported
import dev.shephard.player.ui.glass.rememberAppBlurBackdrop
import dev.shephard.player.ui.glass.rememberWallpaperBlurBackdrop
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.screens.NowPlayingSheet
import dev.shephard.player.ui.screens.NowPlayingSheetM3
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

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
    val initialUseMiuix = remember { runBlocking { prefs.useMiuix.first() } }
    val useMiuix by prefs.useMiuix.collectAsState(initial = initialUseMiuix)
    val appleFloatingBar by prefs.useAppleFloatingBar.collectAsState(initial = false)

    // Resolve the persisted "last main page" synchronously on the very first
    // composition so rememberPagerState is built with a stable initial value.
    // We deliberately do NOT collect lastMainPage as state — doing so would
    // cause a recomposition loop and re-create the pager state on every page
    // change, which in turn causes a noticeable hitch when leaving Settings.
    val initialLastMainPage = remember { runBlocking { prefs.lastMainPage.first() } }

    // A dedicated backdrop whose layer captures the wallpaper. The Apple-style
    // floating dock uses this so the wallpaper stays visible behind its
    // liquid-glass surface (see rememberWallpaperBlurBackdrop). The wallpaper
    // itself is rendered by the Box wrapped in Modifier.layerBackdrop(...)
    // below — the drawContent() call inside the backdrop is what captures it.
    val wallpaperBackdrop = rememberWallpaperBlurBackdrop(blurEnabled)
    // Backdrop used by the top bars / content pages. We always create the
    // standard one too, so screens that aren't bound to the wallpaper still
    // get a glass surface.
    val backgroundBackdrop = rememberAppBlurBackdrop(blurEnabled)
    val contentBackdrop = rememberAppBlurBackdrop(blurEnabled)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalAppBackdrop provides backgroundBackdrop,
        LocalContentBackdrop provides contentBackdrop,
    ) {
        val backStack = remember { mutableStateListOf<NavKey>(MainRoute) }
        var showNowPlaying by remember { mutableStateOf(false) }

        // Build the pager state once with a stable initial value. The
        // rememberPagerState factory is keyed only on pageCount, so this won't
        // be rebuilt on subsequent recompositions.
        val pagerState = rememberPagerState(
            initialPage = initialLastMainPage.coerceIn(0, bottomNavDestinations.size - 1),
            pageCount = { bottomNavDestinations.size },
        )
        val mainPagerState = rememberMainPagerState(pagerState)
        val settledPage = mainPagerState.pagerState.settledPage

        // Persist the settled page, debounced so we don't write to DataStore on
        // every intermediate frame of a swipe gesture. This is the main fix for
        // the Settings -> Playlists transition hitch.
        LaunchedEffect(settledPage) {
            if (initialLastMainPage != settledPage) {
                delay(250)
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

        // When the user has set a wallpaper we want the page content to sit on
        // top of it rather than on a solid Miuix/MA3 background. Otherwise fall
        // back to the theme's normal background so the dark/light theme still
        // looks correct.
        val themeBackground = if (useMiuix) {
            MiuixAppTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
        val containerBackground = if (wallpaper.isNotEmpty()) Color.Transparent else themeBackground

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (backgroundBackdrop != null) Modifier.layerBackdrop(backgroundBackdrop)
                        else Modifier
                    )
            ) {
                // Solid base layer in the theme color. When a wallpaper is set
                // we leave this opaque to avoid any flash of a wrong color, but
                // the wallpaper layer below covers it completely.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeBackground)
                )
                if (wallpaper.isNotEmpty()) {
                    // The wallpaper layer is wrapped in a `layerBackdrop` so
                    // any glass effect (Apple dock, mini player pop-up) bound
                    // to the same backdrop will see the wallpaper underneath.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (wallpaperBackdrop != null) Modifier.layerBackdrop(wallpaperBackdrop)
                                else Modifier
                            )
                    ) {
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
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
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
                                wallpaperBackdrop = wallpaperBackdrop,
                            )
                        }

                        NavGraph(
                            backStack = backStack,
                            playerViewModel = playerViewModel,
                            useMiuix = useMiuix,
                            mainPagerState = mainPagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(containerBackground)
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
    wallpaperBackdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop? = null,
) {
    when {
        appleStyle -> AppleFloatingDock(
            useMiuix = useMiuix,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            wallpaperBackdrop = wallpaperBackdrop,
        )
        !useMiuix -> M3NavigationDock(selectedIndex = selectedIndex, onSelected = onSelected)
        else -> MiuixNavigationDock(selectedIndex = selectedIndex, onSelected = onSelected)
    }
}

@Composable
private fun M3NavigationDock(
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    val insets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    )
    M3NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = insets,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        bottomNavDestinations.forEachIndexed { index, dest ->
            val label = when (dest) {
                Destination.Home -> strings.home
                Destination.Music -> strings.music
                Destination.Playlists -> strings.playlists
                Destination.Settings -> strings.settings
            }
            M3NavigationBarItem(
                selected = selectedIndex() == index,
                onClick = { onSelected(index) },
                icon = {
                    M3Icon(
                        imageVector = if (selectedIndex() == index) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                    )
                },
                label = {
                    M3Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
    useMiuix: Boolean,
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
    wallpaperBackdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop? = null,
) {
    val blurOn = LocalBlurEnabled.current
    // Prefer the wallpaper-aware backdrop so the liquid-glass surface
    // actually shows the wallpaper behind it. Fall back to the content
    // backdrop (which contains the page contents but not the wallpaper) and
    // finally to a freshly created empty backdrop as a last resort.
    val effectiveBackdrop: top.yukonga.miuix.kmp.blur.Backdrop = wallpaperBackdrop
        ?: LocalContentBackdrop.current
        ?: rememberLayerBackdrop()
    val mode = when {
        useMiuix && blurOn && effectiveBackdrop != null && isLiquidGlassSupported -> FloatingBottomBarMode.LiquidGlass
        useMiuix && blurOn && effectiveBackdrop != null -> FloatingBottomBarMode.Blur
        else -> FloatingBottomBarMode.None
    }
    val strings = LocalStrings.current

    val barColors = if (useMiuix) {
        FloatingBottomBarDefaults.colors()
    } else {
        FloatingBottomBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            indicatorColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(
                bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding()
            ),
        contentAlignment = Alignment.Center
    ) {
        FloatingBottomBar(
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            backdrop = effectiveBackdrop,
            tabsCount = bottomNavDestinations.size,
            isBlurEnabled = useMiuix && blurOn && effectiveBackdrop != null,
            mode = mode,
            colors = barColors,
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
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    if (useMiuix) {
                        dev.shephard.player.ui.miuix.Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = label
                        )
                        dev.shephard.player.ui.miuix.Text(
                            text = label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Visible
                        )
                    } else {
                        M3Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = label
                        )
                        M3Text(
                            text = label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Visible
                        )
                    }
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

