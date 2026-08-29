// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.nowplaying.util

import android.net.Uri
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.util.UnstableApi

/**
 * Object that mirrors the small slice of `yos.music.player.data.objects
 * .MediaViewModelObject` used by the imported `YosLyricView`.
 *
 * `lrcEntries` is a list of (timestamp_ms, text) pairs that the lyric
 * view scrolls through; `bitmap` is the current track's album art URI;
 * `bitrate` / `samplingRate` / `isDolby` are populated by the player
 * service when audio metadata becomes available.
 *
 * The values are wired from the now-playing sheet / PlayerViewModel at
 * runtime, so the object just provides the `MutableState` slots.
 */
@UnstableApi
object MediaViewModelObject {
    val lrcEntries: MutableState<List<List<Pair<Float, String>>>> = mutableStateOf(emptyList())
    val bitmap: MutableState<Uri?> = mutableStateOf(null)

    val bitrate: MutableIntState = mutableIntStateOf(0)
    val samplingRate: MutableIntState = mutableIntStateOf(0)
    val isDolby: MutableState<Boolean> = mutableStateOf(false)

    val otherSideForLines: MutableState<List<Boolean>> = mutableStateOf(emptyList())
    val mainLyricLines: MutableState<List<String>> = mutableStateOf(emptyList())
}
