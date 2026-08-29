// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.nowplaying.util

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.util.UnstableApi

/**
 * Object that mirrors the small slice of `yos.music.player.data.objects
 * .MainViewModelObject` used by the imported `YosLyricView`.
 *
 * Lambda Player's [dev.shephard.player.player.PlayerViewModel] already
 * exposes the per-track `lyrics` / `syncedLyrics` lists; this object just
 * holds the small global UI state the lyric view needs (the currently
 * selected `nowPage` and the translation toggle).
 */
@UnstableApi
object MainViewModelObject {
    val nowPage = mutableStateOf("Album")
    val translation = mutableStateOf(false)
    val blurred = mutableStateOf(true)
    val syncLyricIndex = mutableIntStateOf(0)
}
