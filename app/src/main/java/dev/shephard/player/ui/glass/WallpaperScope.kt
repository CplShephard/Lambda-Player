// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.shephard.player.ui.miuix.MiuixAppTheme

/**
 * The wallpaper chosen in Theme Settings belongs to the four main pages (Home, Musics,
 * Playlists, Settings) only. Every screen pushed on top of them — the settings submenus
 * (Theme / Playback / About / Stats) and the playlist detail views — is wrapped in this scope.
 *
 * Inside it the app behaves exactly as if no wallpaper were set:
 *  - [LocalWallpaperEnabled] is false, so Material 3 scaffolds use their solid surface colour;
 *  - [LocalWallpaperContentColor] is unspecified, so text uses the normal theme colours instead
 *    of the colour picked for the wallpaper brightness;
 *  - [LocalAppBackdrop] (the wallpaper backdrop) is null, so glass surfaces don't blur the
 *    wallpaper and fall back to their solid colours;
 *  - an opaque theme background is painted underneath, so the wallpaper layer behind the
 *    navigation host can never show through, even in a frame where the page has not drawn yet.
 */
@Composable
fun NoWallpaperScope(useMiuix: Boolean, content: @Composable () -> Unit) {
    val background: Color = if (useMiuix) {
        MiuixAppTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.surface
    }
    CompositionLocalProvider(
        LocalWallpaperEnabled provides false,
        LocalWallpaperContentColor provides Color.Unspecified,
        LocalAppBackdrop provides null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
        ) {
            content()
        }
    }
}
