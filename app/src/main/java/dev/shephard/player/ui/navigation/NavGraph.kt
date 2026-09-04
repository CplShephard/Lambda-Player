// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar as M3NavigationBar
import androidx.compose.material3.NavigationBarItem as M3NavigationBarItem
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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

    // Predictive back animations (shared setting used by both UI engines).
    val predictiveBackAnimation by preferences.predictiveBackAnimation
        .collectAsState(initial = PredictiveBackAnimation.MIUIX)
    val predictiveBackExitDirection by preferences.predictiveBackExitDirection
        .collectAsState(initial = PredictiveBackExitDirection.FOLLOW_GESTURE)

    // Submenu transitions: Miuix gets the Miuix slide, Material 3 gets the
    // Material 3 fade-through motion; both carry the chosen predictive back
    // pop specification.
    val submenuMetadata = remember(useMiuix, predictiveBackAnimation, predictiveBackExitDirection) {
        val base = if (useMiuix) PageTransitions.submenuMetadata else PageTransitions.m3SubmenuMetadata
        base + PageTransitions.predictiveBackSubmenuMetadata(
            animation = predictiveBackAnimation,
            exitDirection = predictiveBackExitDirection,
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
                            // Miuix engine: plain slide (Miuix behaviour).
                            // Material 3 engine: M3 crossfade+depth page motion.
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

            entry<ThemeRoute>(metadata = submenuMetadata) {
                if (useMiuix) {
                    ThemeSettingsScreen(onBack = ::pop)
                } else {
                    ThemeSettingsScreenM3(onBack = ::pop)
                }
            }
            entry<PlayerRoute>(metadata = submenuMetadata) {
                if (useMiuix) {
                    PlayerSettingsScreen(onBack = ::pop)
                } else {
                    PlayerSettingsScreenM3(onBack = ::pop)
                }
            }
            entry<AboutRoute>(metadata = submenuMetadata) {
                if (useMiuix) {
                    AboutSettingsScreen(onBack = ::pop)
                } else {
                    AboutSettingsScreenM3(onBack = ::pop)
                }
            }
            entry<StatsRoute>(metadata = submenuMetadata) {
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
        // Miuix engine: the Apple option still means the Apple-style floating
        // pill dock (unchanged behaviour).
        appleStyle && useMiuix -> AppleFloatingDock(
            useMiuix = true,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
        )
        // Material 3: the Apple dock is no longer available. The same option
        // toggles the M3 floating bar instead: ON -> M3 floating bar
        // (PixelPlayer-style), OFF -> the classic Material 3 navigation bar.
        appleStyle -> M3FloatingDock(selectedIndex = selectedIndex, onSelected = onSelected)
        !useMiuix -> M3NavigationDock(selectedIndex = selectedIndex, onSelected = onSelected)
        else -> MiuixNavigationDock(selectedIndex = selectedIndex, onSelected = onSelected)
    }
}

/**
 * Classic Material 3 navigation bar — the pre-PixelPlayer M3 dock, still used
 * when the "floating bar" option is OFF.
 */
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

/**
 * Material 3 floating bottom bar, ported from PixelPlayer's
 * [pixelplay.presentation.components.PlayerInternalNavigationBar]:
 * a floating rounded surface with a spring "pill" indicator per item
 * (secondaryContainer pill that fades/scales in behind the icon, which
 * scales to 1.1x), icon in primary when selected and a 13sp label below.
 * Used when the (M3-only) "floating bar" setting is ON, so M3 gets
 * Material 3's own floating bar instead of the Apple dock.
 */
@Composable
private fun M3FloatingDock(
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    val navBarInset = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp + navBarInset)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavDestinations.forEachIndexed { index, dest ->
                    val selected = index == selectedIndex()
                    val label = when (dest) {
                        Destination.Home -> strings.home
                        Destination.Music -> strings.music
                        Destination.Playlists -> strings.playlists
                        Destination.Settings -> strings.settings
                    }
                    M3FloatingNavItem(
                        selected = selected,
                        onClick = { onSelected(index) },
                        icon = {
                            M3Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = label,
                            )
                        },
                        label = label,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Port of PixelPlayer's CustomNavigationBarItem: 64x32dp secondaryContainer
 * pill indicator (spring fade/scale), 48x24dp icon slot clipped to 12dp
 * corners with a bouncy 1.1x scale when selected, and the 13sp label that
 * fades in below.
 */
@Composable
private fun RowScope.M3FloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 150),
        label = "m3FloatingIconColor",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 150),
        label = "m3FloatingTextColor",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "m3FloatingIconScale",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Indicator pill + icon slot
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 64.dp, height = 32.dp),
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = tween(100)) +
                    scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ),
                exit = fadeOut(animationSpec = tween(100)) +
                    scaleOut(animationSpec = tween(100)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp),
                        )
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 48.dp, height = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            ) {
                CompositionLocalProvider(LocalContentColor provides iconColor) {
                    icon()
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(200, delayMillis = 50)),
            exit = fadeOut(animationSpec = tween(100)),
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.padding(top = 4.dp)) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.labelMedium.copy(
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                ) {
                    M3Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
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
