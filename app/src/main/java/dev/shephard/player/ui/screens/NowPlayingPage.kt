// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.screens

/**
 * String constants used to identify the active page inside the
 * Flamingo-style now-playing sheet. Kept as plain string constants
 * (rather than an enum) so the imported `nowPageLambda: () -> String`
 * contract from the original code continues to work without a rewrite.
 */
object NowPlayingPage {
    const val Album = "Album"
    const val PlayingList = "PlayingList"
    const val Lyric = "Lyric"
}
