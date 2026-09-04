package dev.shephard.player.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection

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

    // ─────────────────────────────────────────────────────────────────────────
    // Material 3 transitions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Material 3 submenu motion: "fade-through" — the incoming page fades in
     * with a subtle slide, the outgoing page slides a little and fades out.
     */
    private val m3Tween = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)
    private val m3Fade = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)

    val m3EnterSubmenu: EnterTransition =
        slideInHorizontally(animationSpec = m3Tween) { it / 3 } +
            fadeIn(animationSpec = m3Fade)

    val m3ExitSubmenu: ExitTransition =
        slideOutHorizontally(animationSpec = m3Tween) { -it / 6 } +
            fadeOut(animationSpec = m3Fade)

    val m3PopEnterSubmenu: EnterTransition =
        slideInHorizontally(animationSpec = m3Tween) { -it / 6 } +
            fadeIn(animationSpec = m3Fade)

    val m3PopExitSubmenu: ExitTransition =
        slideOutHorizontally(animationSpec = m3Tween) { it / 3 } +
            fadeOut(animationSpec = m3Fade)

    val m3SubmenuMetadata: Map<String, Any> =
        NavDisplay.transitionSpec {
            ContentTransform(m3EnterSubmenu, m3ExitSubmenu)
        } + NavDisplay.popTransitionSpec {
            ContentTransform(m3PopEnterSubmenu, m3PopExitSubmenu)
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Predictive back transition specifications (InstallerX Revived styles)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the predictive-pop metadata map for a submenu entry. When the
     * animation is NONE the spec returns null so NavDisplay falls back to its
     * normal (non-predictive) pop transition.
     */
    fun predictiveBackSubmenuMetadata(
        animation: PredictiveBackAnimation,
        exitDirection: PredictiveBackExitDirection,
    ): Map<String, Any> {

        fun directionSign(edge: Int): Int = when (exitDirection) {
            PredictiveBackExitDirection.ALWAYS_RIGHT -> 1
            PredictiveBackExitDirection.ALWAYS_LEFT -> -1
            PredictiveBackExitDirection.FOLLOW_GESTURE ->
                if (edge == NavigationEvent.EDGE_RIGHT) -1 else 1
        }

        val spec: androidx.compose.animation.AnimatedContentTransitionScope<Scene<*>>.(
            @androidx.navigationevent.NavigationEvent.SwipeEdge Int,
        ) -> ContentTransform? = { edge ->
            when (animation) {
                PredictiveBackAnimation.NONE -> null

                PredictiveBackAnimation.AOSP,
                PredictiveBackAnimation.SCALE,
                -> ContentTransform(
                    targetContentEnter = fadeIn(
                        animationSpec = tween(durationMillis = 220, delayMillis = 90),
                    ) + scaleIn(
                        animationSpec = tween(durationMillis = 220, delayMillis = 90),
                        initialScale = 0.94f,
                    ),
                    initialContentExit = fadeOut(animationSpec = tween(durationMillis = 90)),
                )

                PredictiveBackAnimation.CLASSIC -> {
                    val sign = directionSign(edge)
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        ) { -sign * it / 8 } + fadeIn(animationSpec = tween(durationMillis = 220)),
                        initialContentExit = slideOutHorizontally(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        ) { sign * it / 3 } + fadeOut(animationSpec = tween(durationMillis = 220)),
                    )
                }

                PredictiveBackAnimation.MIUIX -> {
                    val sign = directionSign(edge)
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            animationSpec = tween(durationMillis = 550, easing = LinearEasing),
                        ) { -sign * it / 4 },
                        initialContentExit = slideOutHorizontally(
                            animationSpec = tween(durationMillis = 550, easing = LinearEasing),
                        ) { sign * it },
                    )
                }
            }
        }

        return NavDisplay.predictivePopTransitionSpec(spec)
    }
}
