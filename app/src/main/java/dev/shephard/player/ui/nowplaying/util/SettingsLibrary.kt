// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.nowplaying.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.shephard.player.player.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Settings singleton that bridges the imported Flamingo now-playing
 * settings (`SettingsLibrary.NowPlayingTranslation`, `LyricBlurEffect`,
 * `NowPlayingShowVolumeBar`, `NowplayingBackgroundEffect`) with Lambda
 * Player's persistent [PreferencesManager].
 *
 * Each property is backed by a [MutableStateFlow] that is seeded on first
 * access from the DataStore. The corresponding [PreferencesManager] key
 * is kept in sync inside [bind] — call [bind] from a Composable (e.g.
 * [NowPlayingSheet]) so the in-memory flows are refreshed whenever the
 * underlying value changes.
 */
object SettingsLibrary {
    private val _nowPlayingTranslation = MutableStateFlow(false)
    val NowPlayingTranslationFlow: StateFlow<Boolean> = _nowPlayingTranslation.asStateFlow()
    var NowPlayingTranslation: Boolean
        get() = _nowPlayingTranslation.value
        set(value) { _nowPlayingTranslation.value = value }

    private val _lyricBlurEffect = MutableStateFlow(true)
    val LyricBlurEffectFlow: StateFlow<Boolean> = _lyricBlurEffect.asStateFlow()
    var LyricBlurEffect: Boolean
        get() = _lyricBlurEffect.value
        set(value) { _lyricBlurEffect.value = value }

    private val _nowPlayingShowVolumeBar = MutableStateFlow(false)
    val NowPlayingShowVolumeBarFlow: StateFlow<Boolean> = _nowPlayingShowVolumeBar.asStateFlow()
    var NowPlayingShowVolumeBar: Boolean
        get() = _nowPlayingShowVolumeBar.value
        set(value) { _nowPlayingShowVolumeBar.value = value }

    private val _nowplayingBackgroundEffect = MutableStateFlow(true)
    val NowplayingBackgroundEffectFlow: StateFlow<Boolean> = _nowplayingBackgroundEffect.asStateFlow()
    var NowplayingBackgroundEffect: Boolean
        get() = _nowplayingBackgroundEffect.value
        set(value) { _nowplayingBackgroundEffect.value = value }

    // Lyric font weight name (matches androidx.compose.ui.text.font.FontWeight names):
    //   "Normal", "Medium", "SemiBold", "Bold". Default is "Medium" which is
    //   visually closest to the Flamingo lyric view's look.
    private val _lyricFontWeight = MutableStateFlow("Medium")
    val LyricFontWeightFlow: StateFlow<String> = _lyricFontWeight.asStateFlow()
    var LyricFontWeight: String
        get() = _lyricFontWeight.value
        set(value) { _lyricFontWeight.value = value }

    // Whether the lyric view should use balanced line breaks
    // (LineBreak.Strategy.Balanced vs Simple). Defaults to true.
    private val _lyricLineBalance = MutableStateFlow(true)
    val LyricLineBalanceFlow: StateFlow<Boolean> = _lyricLineBalance.asStateFlow()
    var LyricLineBalance: Boolean
        get() = _lyricLineBalance.value
        set(value) { _lyricLineBalance.value = value }

    /**
     * Hook the in-memory flows to the on-disk [PreferencesManager] values.
     * Call this from a Composable so the seed values are picked up on
     * the first composition and updates from the settings page flow
     * through into the now-playing sheet.
     */
    @Composable
    fun Bind() {
        val context = LocalContext.current
        val prefs = remember { PreferencesManager(context) }
        val translation by prefs.nowPlayingTranslation.collectAsState(initial = false)
        val blur by prefs.lyricBlurEffect.collectAsState(initial = true)
        val volume by prefs.nowPlayingShowVolumeBar.collectAsState(initial = false)
        val bg by prefs.nowplayingBackgroundEffect.collectAsState(initial = true)
        LaunchedEffect(translation) { _nowPlayingTranslation.value = translation }
        LaunchedEffect(blur) { _lyricBlurEffect.value = blur }
        LaunchedEffect(volume) { _nowPlayingShowVolumeBar.value = volume }
        LaunchedEffect(bg) { _nowplayingBackgroundEffect.value = bg }
    }
}
