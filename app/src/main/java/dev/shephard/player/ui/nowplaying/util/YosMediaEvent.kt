// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.nowplaying.util

/**
 * Media event callback used by the imported Flamingo `YosLyricView` to
 * forward seek requests from the lyric timeline to the underlying player.
 * Translated from `yos.music.player.code.utils.lrc.YosMediaEvent`.
 */
interface YosMediaEvent {
    fun onSeek(position: Int)
}
