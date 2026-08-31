// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar as M3NavigationBar
import androidx.compose.material3.NavigationBarItem as M3NavigationBarItem
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import dev.shephard.player.data.AudioTrack
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
import dev.shephard.player.ui.glass.rememberCombinedBackdrop
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.screens.AboutSettingsScreen
import dev.shephard.player.ui.screens.AboutSettingsScreenM3
import dev.shephard.player.ui.screens.HomeScreen
import dev.shephard.player.ui.screens.HomeScreenM3
import dev.shephard.player.ui.screens.MusicScreen
import dev.shephard.player.ui.screens.MusicScreenM3
import dev.shephard.player.ui.screens.PlayerSettingsScreen
import dev.shephard.player.ui.screens.PlayerSettingsScreenM3
import dev.shephard.player.ui.screens.PlaylistScreen
import dev.shephard.player.ui.screens.PlaylistScreenM3
import dev.shephard.player.ui.screens.SettingsScreen
import dev.shephard.player.ui.screens.SettingsScreenM3
import dev.shephard.player.ui.screens.StatsScreen
import dev.shephard.player.ui.screens.StatsScreenM3
import dev.shephard.player.ui.screens.ThemeSettingsScreen
import dev.shephard.player.ui.screens.ThemeSettingsScreenM3
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

object MainRoute : NavKey
object ThemeRoute : NavKey
object PlayerRoute : NavKey
object AboutRoute : NavKey
object StatsRoute : NavKey

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
    val transitionEffects = remember {
        NavDisplayTransitionEffects(
            enableCornerClip = true,
            dimAmount = 0.5f,
            blockInputDuringTransition = true,
            popDirectionFollowsSwipeEdge = false,
        )
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    val entryProvider = remember(backStack, useMiuix) {
        entryProvider<NavKey> {
            entry<MainRoute> {
                Box(modifier = Modifier.fillMaxSize()) {
                    // The pages are captured into the content backdrop so the
                    // dock / mini player glass can blur whatever scrolls behind
                    // them. Keeping the dock OUTSIDE this captured layer avoids
                    // the dock blurring its own pixels.
                    val contentBackdrop = LocalContentBackdrop.current
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (contentBackdrop != null) Modifier.layerBackdrop(contentBackdrop)
                                else Modifier
                            )
                    ) {
                        HorizontalPager(
                            state = mainPagerState.pagerState,
                            userScrollEnabled = true,
                            beyondViewportPageCount = 1,
                        ) { page ->
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

                    // The dock + mini player live INSIDE the MainRoute entry, drawn
                    // above the pager. When a submenu (Theme/Player/About/Stats) is
                    // pushed it becomes a later entry in the back stack, so NavDisplay
                    // renders it on a higher z-index and it slides over this whole
                    // entry — covering the dock and mini player naturally, with no
                    // manual z-index flipping (which previously made them pop in/out).
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .zIndex(1f)
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

            entry<ThemeRoute>(metadata = PageTransitions.submenuMetadata) {
                if (useMiuix) {
                    ThemeSettingsScreen(onBack = ::pop)
                } else {
                    ThemeSettingsScreenM3(onBack = ::pop)
                }
            }
            entry<PlayerRoute>(metadata = PageTransitions.submenuMetadata) {
                if (useMiuix) {
                    PlayerSettingsScreen(onBack = ::pop)
                } else {
                    PlayerSettingsScreenM3(onBack = ::pop)
                }
            }
            entry<AboutRoute>(metadata = PageTransitions.submenuMetadata) {
                if (useMiuix) {
                    AboutSettingsScreen(onBack = ::pop)
                } else {
                    AboutSettingsScreenM3(onBack = ::pop)
                }
            }
            entry<StatsRoute>(metadata = PageTransitions.submenuMetadata) {
                if (useMiuix) {
                    StatsScreen(
                        playerViewModel = playerViewModel,
                        onBack = ::pop,
                    )
                } else {
                    StatsScreenM3(
                        playerViewModel = playerViewModel,
                        onBack = ::pop,
                    )
                }
            }
        }
    }

    Box(modifier = modifier) {
        // FIX: Create the decorated entries (and thus the SaveableStateHolder decorator)
        // INSIDE key(useMiuix) so that every UI-engine switch gets a brand new state holder.
        // Previously the decorator was remembered outside this key(), which meant the same
        // SaveableStateHolder was reused by the recreated NavDisplay and threw
        // "IllegalArgumentException: Key MainRoute was used multiple times".
        key(useMiuix) {
            val entries = rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                entryProvider = entryProvider,
            )
            NavDisplay(
                entries = entries,
                onBack = ::pop,
                transitionEffects = transitionEffects,
            )
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
    // Read the Apple-style preference inside the dock so a change made in Settings
    // is picked up as soon as the user returns to the main page (values captured in
    // the navigation entry lambda are frozen, so we must collect the state here).
    // The persisted value is also read synchronously once so the dock doesn't flash
    // its fallback navbar form for a second on every launch while DataStore warms up.
    val initialAppleStyle = remember { runBlocking { preferences.useAppleFloatingBar.first() } }
    val appleStyle by preferences.useAppleFloatingBar.collectAsState(initial = initialAppleStyle)
    when {
        appleStyle -> AppleFloatingDock(
            useMiuix = useMiuix,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
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
) {
    val blurOn = LocalBlurEnabled.current
    // The dock must blur what is actually BEHIND it: the wallpaper AND the live
    // page content scrolling underneath (the music list, cards, etc.). The
    // wallpaper is captured by LocalAppBackdrop (which now points at the
    // wallpaper layer), while the pager content is captured by
    // LocalContentBackdrop. We combine the two so the liquid-glass surface shows
    // elements sliding past it — not a frozen wallpaper snapshot. Neither
    // recording includes the dock itself (both recording nodes are siblings of
    // this dock), so there is no self-referential RenderNode and no crash.
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

    // The dock sits directly on the (possibly bright) wallpaper, so its item
    // colour must adapt to the wallpaper like the top-bar titles do. Otherwise
    // white items vanish against a bright wallpaper and the dock looks empty.
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
    useMiuix: Boolean,
    onOpenNowPlaying: () -> Unit
) {
    // Collect visibility here (instead of receiving it as a parameter) so the
    // mini player appears/disappears live: parameters captured by the navigation
    // entry lambda are frozen, but this state is read fresh on every composition.
    val visible by remember(playerViewModel) {
        playerViewModel.uiState
            .map { it.currentTrack != null }
            .distinctUntilChanged()
    }.collectAsState(initial = false)

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
