// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.shephard.player.player.LayoutMode

/** Matches the list/grid animation already used by Material playlists. */
@Composable
internal fun LayoutSwitchAnimation(layout: Int, modifier: Modifier = Modifier, content: @Composable (Int) -> Unit) {
    AnimatedContent(targetState = layout, modifier = modifier, transitionSpec = {
        val toGrid = targetState == LayoutMode.GRID
        (slideInHorizontally(animationSpec = tween(380)) { if (toGrid) it / 5 else -it / 5 } +
            fadeIn(animationSpec = tween(260)) + scaleIn(animationSpec = tween(380), initialScale = .94f)) togetherWith
            (slideOutHorizontally(animationSpec = tween(380)) { if (toGrid) -it / 5 else it / 5 } +
                fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(380), targetScale = .94f)) using
            SizeTransform(clip = false)
    }, label = "libraryLayout") { value -> content(value) }
}
