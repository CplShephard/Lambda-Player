// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shephard.player.R
import dev.shephard.player.ui.miuix.Icon
import kotlinx.coroutines.launch

/** Shared Now Playing transport: same artwork, press bounce and play/pause transition. */
@Composable
internal fun MiuixPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    playDescription: String,
    pauseDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 72.dp,
    iconSize: Dp = 40.dp,
    pressScale: Float = 0.86f,
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier.size(buttonSize)
            .bounceClick(pressScale = pressScale) {
                scope.launch {
                    scale.animateTo(0.85f, tween(80))
                    scale.animateTo(1.1f, spring(dampingRatio = 0.45f, stiffness = 600f))
                    scale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 600f))
                }
                onClick()
            }
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (fadeIn(tween(150)) + scaleIn(initialScale = 0.5f)) togetherWith
                    (fadeOut(tween(100)) + scaleOut(targetScale = 1.5f))
            },
            label = "miuixPlayPauseIcon",
        ) { playing ->
            Icon(
                painter = painterResource(if (playing) R.drawable.ic_nowplaying_pause else R.drawable.ic_nowplaying_play),
                contentDescription = if (playing) pauseDescription else playDescription,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
