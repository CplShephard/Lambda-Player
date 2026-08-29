// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.nowplaying.util

import androidx.compose.runtime.Stable

/**
 * UI configuration bag passed to the imported Flamingo `YosLyricView`.
 * Translated from `yos.music.player.code.utils.lrc.YosUIConfig`.
 */
@Stable
data class YosUIConfig(
    val edgeFade: Boolean = true,
    val formatText: Boolean = true,
    val noLrcText: String = "No lyrics available",
    val blankHeight: Int = 70,
    val mainTextSize: Int = 34,
    val subTextSize: Int = mainTextSize - 18,
    val mainTextBasicColor: Long = 0xFFF2F2F2,
    val subTextBasicColor: Long = 0xFF919191,
    val normalMainTextAlpha: Float = 0.4f,
    val normalSubTextAlpha: Float = 0.3f,
    val currentMainTextAlpha: Float = 0.9f,
    val currentSubTextAlpha: Float = 0.6f,
)
