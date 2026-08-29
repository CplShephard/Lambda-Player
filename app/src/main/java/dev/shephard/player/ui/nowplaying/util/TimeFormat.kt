// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.nowplaying.util

/**
 * Format a duration given in seconds as M:SS.
 *
 * Used in the Miuix now-playing sheet (Flamingo-style time labels). Returns
 * "0:00" for non-positive inputs.
 */
fun formatTimeSeconds(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val minutes = safe / 60
    val secs = safe % 60
    return "$minutes:${if (secs < 10) "0$secs" else "$secs"}"
}
