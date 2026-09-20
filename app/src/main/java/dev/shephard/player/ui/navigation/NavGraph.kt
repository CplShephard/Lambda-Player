// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.activity.compose.BackHandler
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection
import dev.shephard.player.ui.animation.predictiveback.predictiveBackHandler
import dev.shephard.player.ui.components.MiniPlayer
import dev.shephard.player.ui.components.M3MiniPlayer
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
    val predictiveBackAnimation by preferences.predictiveBackAnimation
        .collectAsState(initial = PredictiveBackAnimation.MIUIX)
    val predictiveBackExitDirection by preferences.predictiveBackExitDirection
        .collectAsState(initial = PredictiveBackExitDirection.FOLLOW_GESTURE)

    val isPredictiveEnabled = predictiveBackAnimation != PredictiveBackAnimation.NONE

    val handler = remember(predictiveBackAnimation, predictiveBackExitDirection) {
        predictiveBackHandler(predictiveBackAnimation, predictiveBackExitDirection)
    }

    val navigationScope = rememberCoroutineScope()

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    val entryProvider = remember(backStack, useMiuix) {
        entryProvider<NavKey> {
            entry<MainRoute> {
                Box(modifier = Modifier.fillMaxSize()) {
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
            entry<ThemeRoute> {
                if (useMiuix) ThemeSettingsScreen(onBack = ::pop) else ThemeSettingsScreenM3(onBack = ::pop)
            }
            entry<PlayerRoute> {
                if (useMiuix) PlayerSettingsScreen(onBack = ::pop) else PlayerSettingsScreenM3(onBack = ::pop)
            }
            entry<AboutRoute> {
                if (useMiuix) AboutSettingsScreen(onBack = ::pop) else AboutSettingsScreenM3(onBack = ::pop)
            }
            entry<StatsRoute> {
                if (useMiuix) StatsScreen(playerViewModel = playerViewModel, onBack = ::pop)
                else StatsScreenM3(playerViewModel = playerViewModel, onBack = ::pop)
            }
        }
    }

    Box(modifier = modifier) {
        key(useMiuix) {
            var gestureState: NavigationEventState<androidx.navigation3.scene.SceneInfo<NavKey>>? = null

            val entries = rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    NavEntryDecorator(
                        onPop = { key ->
                            handler.onPagePop(
                                contentPageKey = key,
                                animationScope = navigationScope
                            )
                        }
                    ) { content ->
                        with(handler) {
                            Box(
                                modifier = Modifier.predictiveBackAnimationDecorator(
                                    transitionState = gestureState?.transitionState,
                                    contentPageKey = content.contentKey,
                                    currentPageKey = backStack.lastOrNull()
                                )
                            ) {
                                content.Content()
                            }
                        }
                    }
                ),
                entryProvider = entryProvider,
            )

            val sceneState = rememberSceneState(
                entries = entries,
                sceneStrategies = listOf(SinglePaneSceneStrategy()),
                onBack = {
                    navigationScope.launch {
                        if (isPredictiveEnabled) {
                            handler.onBackPressed(
                                transitionState = gestureState?.transitionState,
                                currentPageKey = backStack.lastOrNull()
                            )
                        }
                        pop()
                    }
                }
            )

            val currentScene = sceneState.currentScene
            val previousScenes = sceneState.previousScenes
            gestureState = if (isPredictiveEnabled) {
                rememberNavigationEventState(
                    currentInfo = SceneInfo(currentScene),
                    backInfo = previousScenes.map { SceneInfo(it) }
                )
            } else null

            val currentGestureState = gestureState
            if (isPredictiveEnabled && currentGestureState != null) {
                NavigationBackHandler(
                    state = currentGestureState!!,
                    isBackEnabled = backStack.size > 1,
                    onBackCompleted = {
                        navigationScope.launch {
                            handler.onBackPressed(
                                transitionState = currentGestureState!!.transitionState,
                                currentPageKey = backStack.lastOrNull()
                            )
                            pop()
                        }
                    },
                    onBackCancelled = {}
                )
            } else {
                // NONE: predictive back fully disabled, use simple BackHandler
                if (backStack.size > 1) {
                    BackHandler { pop() }
                }
            }

            NavDisplay(
                sceneState = sceneState,
                navigationEventState = gestureState,
                contentAlignment = Alignment.TopStart,
                transitionEffects = NavDisplayTransitionEffects(
                    blockInputDuringTransition = true
                ),
                predictivePopTransitionSpec = { swipeEdge ->
                    if (!isPredictiveEnabled) {
                        // NONE: no predictive preview at all
                        androidx.compose.animation.ContentTransform(
                            targetContentEnter = androidx.compose.animation.EnterTransition.None,
                            initialContentExit = androidx.compose.animation.ExitTransition.None
                        )
                    } else if (useMiuix) {
                        with(handler) { onPredictivePopTransitionSpec(swipeEdge) }
                    } else {
                        // M3 uses its own PlaylistDetailView animation (m3Enter/m3Exit) for all submenus
                        androidx.compose.animation.ContentTransform(
                            targetContentEnter = dev.shephard.player.ui.navigation.PageTransitions.m3PopEnterSubmenu,
                            initialContentExit = dev.shephard.player.ui.navigation.PageTransitions.m3PopExitSubmenu
                        )
                    }
                },
                popTransitionSpec = {
                    if (useMiuix) {
                        with(handler) { onPopTransitionSpec() }
                    } else {
                        androidx.compose.animation.ContentTransform(
                            targetContentEnter = dev.shephard.player.ui.navigation.PageTransitions.m3PopEnterSubmenu,
                            initialContentExit = dev.shephard.player.ui.navigation.PageTransitions.m3PopExitSubmenu
                        )
                    }
                },
                transitionSpec = {
                    if (useMiuix) {
                        with(handler) { onTransitionSpec() }
                    } else {
                        androidx.compose.animation.ContentTransform(
                            targetContentEnter = dev.shephard.player.ui.navigation.PageTransitions.m3EnterSubmenu,
                            initialContentExit = dev.shephard.player.ui.navigation.PageTransitions.m3ExitSubmenu
                        )
                    }
                }
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
    val initialAppleStyle = remember { runBlocking { preferences.useAppleFloatingBar.first() } }
    val appleStyle by preferences.useAppleFloatingBar.collectAsState(initial = initialAppleStyle)
    when {
        appleStyle && useMiuix -> AppleFloatingDock(
            useMiuix = true,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(
                bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
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
    val visible by remember(playerViewModel) {
        playerViewModel.uiState
            .map { it.currentTrack != null }
            .distinctUntilChanged()
    }.collectAsState(initial = false)

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
