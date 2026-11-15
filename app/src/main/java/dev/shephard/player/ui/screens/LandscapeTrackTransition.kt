// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package dev.shephard.player.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/** The directional slide/fade used by landscape Now Playing when the active track changes. */
internal fun <S> AnimatedContentTransitionScope<S>.landscapeTrackTransition(
    navigationDirection: Int,
): ContentTransform {
    val forward = navigationDirection >= 0
    val slideSpec = tween<IntOffset>(
        durationMillis = 320,
        easing = FastOutSlowInEasing,
    )
    val fadeSpec = tween<Float>(
        durationMillis = 220,
        easing = LinearOutSlowInEasing,
    )
    return (slideInHorizontally(slideSpec) { if (forward) it else -it } + fadeIn(fadeSpec)) togetherWith
        (slideOutHorizontally(slideSpec) { if (forward) -it else it } + fadeOut(fadeSpec))
}
