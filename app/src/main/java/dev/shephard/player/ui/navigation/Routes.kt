// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import top.yukonga.miuix.kmp.nav.core.NavKey

sealed interface Route : NavKey {
    data object Main : Route
    data class PlaylistDetails(val playlistIndex: Int) : Route
    data object Theme : Route
    data object Player : Route
    data object About : Route
}
