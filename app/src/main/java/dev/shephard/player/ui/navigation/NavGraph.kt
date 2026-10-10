// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateOf
import dev.shephard.player.ui.components.LocalBottomOverlayHeight
import dev.shephard.player.ui.components.LocalDetailMiniPlayer
import dev.shephard.player.ui.components.LocalPlaylistDetailOpen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar as M3NavigationBar
import androidx.compose.material3.NavigationBarItem as M3NavigationBarItem
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigationevent.NavigationEventInfo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection
import dev.shephard.player.theme.effectiveExitDirection
import dev.shephard.player.ui.animation.predictiveback.installerNavTransition
import dev.shephard.player.ui.components.MiniPlayer
import dev.shephard.player.ui.components.M3MiniPlayer
import dev.shephard.player.ui.glass.FloatingBottomBar
import dev.shephard.player.ui.glass.FloatingBottomBarDefaults
import dev.shephard.player.ui.glass.FloatingBottomBarMode
import dev.shephard.player.ui.glass.LocalAppBackdrop
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.LocalWallpaperEnabled
import dev.shephard.player.ui.glass.isLiquidGlassSupported
import dev.shephard.player.ui.glass.rememberCombinedBackdrop
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.screens.AboutSettingsScreen
import dev.shephard.player.ui.screens.m3.AboutSettingsScreenM3
import dev.shephard.player.ui.screens.HomeScreen
import dev.shephard.player.ui.screens.m3.HomeScreenM3
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.m3.MusicScreenM3
import dev.shephard.player.ui.screens.PlayerSettingsScreen
import dev.shephard.player.ui.screens.m3.PlayerSettingsScreenM3
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.m3.PlaylistScreenM3
import dev.shephard.player.ui.screens.SettingsScreen
import dev.shephard.player.ui.screens.m3.SettingsScreenM3
import dev.shephard.player.ui.screens.StatsScreen
import dev.shephard.player.ui.screens.m3.StatsScreenM3
import dev.shephard.player.ui.screens.ThemeSettingsScreen
import dev.shephard.player.ui.screens.m3.ThemeSettingsScreenM3
import dev.shephard.player.ui.util.rememberDeviceCornerRadius
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

data object MainRoute : NavKey
data object ThemeRoute : NavKey
data object PlayerRoute : NavKey
data object AboutRoute : NavKey
data object StatsRoute : NavKey

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavGraph(
    backStack: SnapshotStateList<NavKey>,
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
    hasMiniPlayer: Boolean = false,
    useMiuix: Boolean = true,
    preferences: PreferencesManager,
    mainPagerState: MainPagerState,
    onOpenNowPlaying: () -> Unit = {},
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> }
) {
    val predictiveBackAnimation by preferences.predictiveBackAnimation
        .collectAsState(initial = PredictiveBackAnimation.MIUIX)
    val storedExitDirection by preferences.predictiveBackExitDirection
        .collectAsState(initial = PredictiveBackExitDirection.FOLLOW_GESTURE)
    // MIUIX always exits to the right; the stored value only matters for styles
    // that expose the option (AOSP, Scale), so a stale "always left" can never leak in.
    val predictiveBackExitDirection = effectiveExitDirection(predictiveBackAnimation, storedExitDirection)

    // Same wiring as InstallerX Revived: the transition decides how pages move while NavDisplay
    // drives the back gesture. The same transitions are used by both the Material 3 and MIUIX UI.
    val transition = remember(predictiveBackAnimation, predictiveBackExitDirection) {
        installerNavTransition(
            animation = predictiveBackAnimation,
            exitDirection = predictiveBackExitDirection,
        )
    }
    val swipeBackDirection = when (LocalLayoutDirection.current) {
        LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
        else -> NavSwipeDirection.LeftToRight
    }
    // NONE: no predictive preview. Each entry takes the gesture itself and pops normally.
    val interceptPredictiveBack =
        predictiveBackAnimation == PredictiveBackAnimation.NONE && backStack.size > 1

    val roundAllCorners = predictiveBackAnimation == PredictiveBackAnimation.AOSP ||
        predictiveBackAnimation == PredictiveBackAnimation.SCALE ||
        predictiveBackAnimation == PredictiveBackAnimation.CLASSIC
    val backdropColor = if (useMiuix) {
        MiuixAppTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    // Same rule as InstallerX: clip to the device corner radius (Leading), or 32dp when every
    // corner is rounded and the device reports no radius (All). Without this the Miuix submenus
    // had square corners because Leading mode clipped at 0dp.
    val navCornerRadius = rememberDeviceCornerRadius()
    val effects = remember(roundAllCorners, backdropColor, navCornerRadius) {
        NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
            cornerClipMode = if (roundAllCorners) NavCornerClipMode.All else NavCornerClipMode.Leading,
            dimAmount = 0.5f,
            backdropColor = backdropColor,
            blockInputDuringTransition = false,
        )
    }

    val lastPopTime = remember { mutableLongStateOf(0L) }

    fun pop() {
        // NavDisplay and the entry-level handler can both report one back press; ignore the echo.
        val now = System.currentTimeMillis()
        if (now - lastPopTime.longValue < 100L) return
        if (backStack.size <= 1) return
        lastPopTime.longValue = now
        backStack.removeAt(backStack.lastIndex)
    }

    val overlayHeight = remember { mutableStateOf(0.dp) }
    val overlayDensity = LocalDensity.current
    // True while a playlist detail is open; hides the global mini player + dock (like submenus do).
    val playlistDetailOpen = remember { mutableStateOf(false) }
    val detailMiniPlayer: @Composable (Modifier) -> Unit = { detailModifier ->
        DetailMiniPlayerHost(
            playerViewModel = playerViewModel,
            useMiuix = useMiuix,
            onOpenNowPlaying = onOpenNowPlaying,
            modifier = detailModifier
        )
    }
    // DSL block for NavDisplay (the public overload). entryProvider() itself is internal in miuix-nav.
    val entryContent = remember(backStack, useMiuix, swipeBackDirection, interceptPredictiveBack) {
        val block: NavEntryBuilder.() -> Unit = {
            entry<MainRoute> {
                NavEntryBackHandler(interceptPredictiveBack, ::pop) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val contentBackdrop = LocalContentBackdrop.current
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                // While playlist detail is open it sits above the dock and mini player
                                // (which stay in place, covered like settings submenus).
                                .zIndex(if (playlistDetailOpen.value) 2f else 0f)
                                .then(
                                    if (contentBackdrop != null) Modifier.layerBackdrop(contentBackdrop)
                                    else Modifier
                                )
                        ) {
                            HorizontalPager(
                                state = mainPagerState.pagerState,
                                // Page swiping is disabled while playlist detail is open.
                                userScrollEnabled = !playlistDetailOpen.value,
                                beyondViewportPageCount = 1,
                            ) { page ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (!useMiuix) {
                                                Modifier.m3PagerPageTransition(mainPagerState.pagerState, page)
                                            } else Modifier
                                        )
                                ) {
                                    when (page) {
                                        0 -> if (useMiuix) {
                                            HomeScreen(
                                                libraryViewModel = viewModel(),
                                                playerViewModel = playerViewModel,
                                                hasMiniPlayer = hasMiniPlayer,
                                                onTrackClick = onTrackClick,
                                            )
                                        } else {
                                            HomeScreenM3(
                                                libraryViewModel = viewModel(),
                                                playerViewModel = playerViewModel,
                                                hasMiniPlayer = hasMiniPlayer,
                                                onTrackClick = onTrackClick,
                                            )
                                        }
                                        1 -> if (useMiuix) {
                                            MusicScreen(
                                                libraryViewModel = viewModel(),
                                                playerViewModel = playerViewModel,
                                                onTrackClick = { tracks, index -> onTrackClick(tracks, index, null) },
                                                hasMiniPlayer = hasMiniPlayer,
                                            )
                                        } else {
                                            MusicScreenM3(
                                                libraryViewModel = viewModel(),
                                                playerViewModel = playerViewModel,
                                                onTrackClick = { tracks, index -> onTrackClick(tracks, index, null) },
                                                hasMiniPlayer = hasMiniPlayer,
                                            )
                                        }
                                        2 -> if (useMiuix) {
                                            PlaylistScreen(
                                                libraryViewModel = viewModel(),
                                                onTrackClick = onTrackClick,
                                                onPlaylistRemixClick = onPlaylistRemixClick,
                                                hasMiniPlayer = hasMiniPlayer,
                                            )
                                        } else {
                                            PlaylistScreenM3(
                                                libraryViewModel = viewModel(),
                                                onTrackClick = onTrackClick,
                                                onPlaylistRemixClick = onPlaylistRemixClick,
                                                hasMiniPlayer = hasMiniPlayer,
                                            )
                                        }
                                        3 -> if (useMiuix) {
                                            SettingsScreen(
                                                playerViewModel = playerViewModel,
                                                onOpenThemeSettings = { backStack.add(ThemeRoute) },
                                                onOpenPlayerSettings = { backStack.add(PlayerRoute) },
                                                onOpenAbout = { backStack.add(AboutRoute) },
                                                onOpenStats = { backStack.add(StatsRoute) },
                                            )
                                        } else {
                                            SettingsScreenM3(
                                                playerViewModel = playerViewModel,
                                                onOpenThemeSettings = { backStack.add(ThemeRoute) },
                                                onOpenPlayerSettings = { backStack.add(PlayerRoute) },
                                                onOpenAbout = { backStack.add(AboutRoute) },
                                                onOpenStats = { backStack.add(StatsRoute) },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = true,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .zIndex(1f),
                            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it }),
                            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it })
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { overlayHeight.value = with(overlayDensity) { it.height.toDp() } }
                            ) {
                                MiniPlayerHost(
                                    playerViewModel = playerViewModel,
                                    useMiuix = useMiuix,
                                    onOpenNowPlaying = onOpenNowPlaying,
                                )
                                MainDock(
                                    preferences = preferences,
                                    useMiuix = useMiuix,
                                    selectedIndex = { mainPagerState.selectedPage },
                                    onSelected = { index -> mainPagerState.animateToPage(index) },
                                )
                            }
                        }
                    }
                }
            }
            entry<ThemeRoute>(swipeDismiss = swipeBackDirection) {
                NavEntryBackHandler(interceptPredictiveBack, ::pop) {
                    if (useMiuix) {
                        ThemeSettingsScreen(onBack = ::pop)
                    } else {
                        CompositionLocalProvider(LocalWallpaperEnabled provides false) {
                            ThemeSettingsScreenM3(onBack = ::pop)
                        }
                    }
                }
            }
            entry<PlayerRoute>(swipeDismiss = swipeBackDirection) {
                NavEntryBackHandler(interceptPredictiveBack, ::pop) {
                    if (useMiuix) {
                        PlayerSettingsScreen(onBack = ::pop)
                    } else {
                        CompositionLocalProvider(LocalWallpaperEnabled provides false) {
                            PlayerSettingsScreenM3(onBack = ::pop)
                        }
                    }
                }
            }
            entry<AboutRoute>(swipeDismiss = swipeBackDirection) {
                NavEntryBackHandler(interceptPredictiveBack, ::pop) {
                    if (useMiuix) {
                        AboutSettingsScreen(onBack = ::pop)
                    } else {
                        CompositionLocalProvider(LocalWallpaperEnabled provides false) {
                            AboutSettingsScreenM3(onBack = ::pop)
                        }
                    }
                }
            }
            entry<StatsRoute>(swipeDismiss = swipeBackDirection) {
                NavEntryBackHandler(interceptPredictiveBack, ::pop) {
                    if (useMiuix) {
                        StatsScreen(playerViewModel = playerViewModel, onBack = ::pop)
                    } else {
                        CompositionLocalProvider(LocalWallpaperEnabled provides false) {
                            StatsScreenM3(playerViewModel = playerViewModel, onBack = ::pop)
                        }
                    }
                }
            }
        }
        block
    }

    val submenuNavConfig = remember(predictiveBackAnimation, transition, effects) {
        SubmenuNavConfig(animation = predictiveBackAnimation, transition = transition, effects = effects)
    }
    CompositionLocalProvider(
        LocalBottomOverlayHeight provides overlayHeight,
        LocalPlaylistDetailOpen provides playlistDetailOpen,
        LocalDetailMiniPlayer provides detailMiniPlayer,
        LocalSubmenuNavConfig provides submenuNavConfig,
    ) {
        Box(modifier = modifier) {
            key(useMiuix) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = ::pop,
                    transition = transition,
                    effects = effects,
                    content = entryContent,
                )
            }
        }
    }
}

@Composable
private fun MainDock(
    preferences: PreferencesManager,
    useMiuix: Boolean,
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
) {
    val initialAppleStyle = remember { runBlocking { preferences.useAppleFloatingBar.first() } }
    val appleStyle by preferences.useAppleFloatingBar.collectAsState(initial = initialAppleStyle)
    when {
        appleStyle && useMiuix -> AppleFloatingDock(
            useMiuix = true,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
        )
        !useMiuix -> M3NavigationDock(preferences = preferences, selectedIndex = selectedIndex, onSelected = onSelected)
        else -> MiuixNavigationDock(selectedIndex = selectedIndex, onSelected = onSelected)
    }
}

@Composable
private fun M3NavigationDock(
    preferences: PreferencesManager,
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    val cornerRadius by preferences.m3NavBarCornerRadius.collectAsState(initial = 24)
    val insets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    )
    M3NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = cornerRadius.dp, topEnd = cornerRadius.dp)),
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
                } else Modifier
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
) {
    val blurOn = LocalBlurEnabled.current
    val wallpaperBackdrop = LocalAppBackdrop.current
    val contentBackdrop = LocalContentBackdrop.current
    val effectiveBackdrop: top.yukonga.miuix.kmp.blur.Backdrop = when {
        wallpaperBackdrop != null && contentBackdrop != null ->
            rememberCombinedBackdrop(wallpaperBackdrop, contentBackdrop)
        wallpaperBackdrop != null -> wallpaperBackdrop
        contentBackdrop != null -> contentBackdrop
        else -> rememberLayerBackdrop()
    }
    val mode = when {
        useMiuix && blurOn && effectiveBackdrop != null && isLiquidGlassSupported -> FloatingBottomBarMode.LiquidGlass
        useMiuix && blurOn && effectiveBackdrop != null -> FloatingBottomBarMode.Blur
        else -> FloatingBottomBarMode.None
    }
    val strings = LocalStrings.current
    val contentColor = wallpaperAdaptiveTextColor(
        fallback = if (useMiuix) MiuixAppTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface
    )
    val barColors = if (useMiuix) {
        FloatingBottomBarDefaults.colors(contentColor = contentColor)
    } else {
        FloatingBottomBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            indicatorColor = MaterialTheme.colorScheme.primary,
            contentColor = contentColor,
        )
    }
    val labelOf: (Destination) -> String = { dest ->
        when (dest) {
            Destination.Home -> strings.home
            Destination.Music -> strings.music
            Destination.Playlists -> strings.playlists
            Destination.Settings -> strings.settings
        }
    }

    // Miuix dock: InstallerX Revived's FloatingBottomBar (ported to ui/glass).
    FloatingBottomBar(
        items = bottomNavDestinations,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        backdrop = effectiveBackdrop,
        mode = mode,
        colors = barColors,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
        iconContent = { dest, index ->
            val icon = if (index == selectedIndex()) dest.selectedIcon else dest.unselectedIcon
            if (useMiuix) {
                dev.shephard.player.ui.miuix.Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            } else {
                M3Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        },
        labelContent = { dest, _ ->
            if (useMiuix) {
                dev.shephard.player.ui.miuix.Text(
                    text = labelOf(dest),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                )
            } else {
                M3Text(
                    text = labelOf(dest),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                )
            }
        },
    )
}

/**
 * Mini player for playlist detail screens. Uses the large Miuix variant; M3 keeps its normal size.
 * Respects the navigation bar inset itself, so nothing is composed (and no space reserved) without a track.
 */
@Composable
private fun DetailMiniPlayerHost(
    playerViewModel: PlayerViewModel,
    useMiuix: Boolean,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier
) {
    val playerState by playerViewModel.uiState.collectAsState()
    if (playerState.currentTrack == null) return

    Box(modifier = modifier.navigationBarsPadding()) {
        if (useMiuix) {
            // This mini player lives inside the pager, whose layer is the content backdrop. Blurring
            // with that same backdrop from inside it is self-referential (native crash), so the
            // blur is turned off here and the solid surface is used instead.
            CompositionLocalProvider(LocalContentBackdrop provides null) {
                MiniPlayer(
                    state = playerState,
                    progressFlow = playerViewModel.progress,
                    onClick = onOpenNowPlaying,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    onNextClick = { playerViewModel.skipToNext() },
                    onPreviousClick = { playerViewModel.skipToPrevious() },
                    large = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            M3MiniPlayer(
                state = playerState,
                progressFlow = playerViewModel.progress,
                onClick = onOpenNowPlaying,
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onNextClick = { playerViewModel.skipToNext() },
                onPreviousClick = { playerViewModel.skipToPrevious() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MiniPlayerHost(
    playerViewModel: PlayerViewModel,
    useMiuix: Boolean,
    onOpenNowPlaying: () -> Unit
) {
    val visible by remember(playerViewModel) {
        playerViewModel.uiState
            .map { it.currentTrack != null }
            .distinctUntilChanged()
    }.collectAsState(
        // Seed from the current state so a re-composed host (e.g. after returning from a submenu)
        // does not replay the enter animation while a track is already loaded.
        initial = playerViewModel.uiState.value.currentTrack != null
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = androidx.compose.animation.core.spring(0.85f, 300f)
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
            M3MiniPlayer(
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

/**
 * Takes the back gesture for the entry it wraps (InstallerX InstallerNavEntry). When predictive
 * back is NONE the gesture is handled here, so NavDisplay does not run a predictive preview.
 */
@Composable
internal fun NavEntryBackHandler(
    interceptPredictiveBack: Boolean,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = interceptPredictiveBack,
        onBackCompleted = onBack,
    )
    content()
}
