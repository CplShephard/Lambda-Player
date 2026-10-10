package dev.shephard.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier

/**
 * True while a playlist detail is on the back stack. The main bottom stack (global mini player + dock)
 * is hidden while this is true, the same way submenus cover it. Written by PlaylistDetailNavHost.
 */
val LocalPlaylistDetailOpen = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

/**
 * Mini player shown inside playlist detail screens. Provided by NavGraph (it needs the player
 * view model). The implementation hides itself when no track is loaded.
 */
val LocalDetailMiniPlayer = compositionLocalOf<@Composable (Modifier) -> Unit> { { _ -> } }
