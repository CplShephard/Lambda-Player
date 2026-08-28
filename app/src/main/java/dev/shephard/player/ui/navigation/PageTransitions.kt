package dev.shephard.player.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.ui.NavDisplay

object PageTransitions {

    val slideSpec: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 400f,
        visibilityThreshold = IntOffset(1, 1)
    )

    private val fadeSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 400f
    )

    private const val ENTER_OFFSET = 0.28f

    private const val EXIT_OFFSET = 0.12f

    private const val COVERED_ALPHA = 0.9f

    private const val ENTER_SCALE = 0.92f

    private const val EXIT_SCALE = 1.08f

    val enterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { (it * ENTER_OFFSET).toInt() } +
            fadeIn(animationSpec = fadeSpec, initialAlpha = COVERED_ALPHA) +
            scaleIn(animationSpec = fadeSpec, initialScale = ENTER_SCALE)

    val exitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { (it * -EXIT_OFFSET).toInt() } +
            fadeOut(animationSpec = fadeSpec, targetAlpha = COVERED_ALPHA) +
            scaleOut(animationSpec = fadeSpec, targetScale = EXIT_SCALE)

    val popEnterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { (it * -EXIT_OFFSET).toInt() } +
            fadeIn(animationSpec = fadeSpec, initialAlpha = COVERED_ALPHA) +
            scaleIn(animationSpec = fadeSpec, initialScale = ENTER_SCALE)

    val popExitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { (it * ENTER_OFFSET).toInt() } +
            fadeOut(animationSpec = fadeSpec, targetAlpha = COVERED_ALPHA) +
            scaleOut(animationSpec = fadeSpec, targetScale = EXIT_SCALE)

    private val miuixDefaultTween = androidx.compose.animation.core.tween<IntOffset>(
        durationMillis = 500,
        easing = NavAnimationEasing
    )

    val enterSubmenu: EnterTransition =
        slideInHorizontally(animationSpec = miuixDefaultTween) { it }

    val exitSubmenu: ExitTransition =
        slideOutHorizontally(animationSpec = miuixDefaultTween) { -it / 4 }

    val popEnterSubmenu: EnterTransition =
        slideInHorizontally(animationSpec = miuixDefaultTween) { -it / 4 }

    val popExitSubmenu: ExitTransition =
        slideOutHorizontally(animationSpec = miuixDefaultTween) { it }

    val submenuMetadata: Map<String, Any> =
        NavDisplay.transitionSpec {
            ContentTransform(enterSubmenu, exitSubmenu)
        } + NavDisplay.popTransitionSpec {
            ContentTransform(popEnterSubmenu, popExitSubmenu)
        }
}
