// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
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
    mainPagerState: MainPagerState,
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
