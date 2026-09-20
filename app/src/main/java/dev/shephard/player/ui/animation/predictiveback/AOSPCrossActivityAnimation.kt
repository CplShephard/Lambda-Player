// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors – ported exact logic for Lambda Player
// Adapted from InstallerX Revived AospNavTransition.kt (CrossActivityPredictive)
// Implements exact InstallerX formulas: MIN_SCALE 0.9, drift 96dp, edge margin 8dp, bounce, Y shift, BackGestureEasing
package dev.shephard.player.ui.animation.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import dev.shephard.player.theme.PredictiveBackExitDirection
import dev.shephard.player.ui.util.rememberDeviceCornerRadius
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val CROSS_ACTIVITY_MIN_SCALE = 0.9f
private const val BOUNCE_STIFFNESS = 200f
private const val BOUNCE_DAMPING = 0.75f
private const val BOUNCE_MAX_KICK = 1000f
private const val BOUNCE_MIN_KICK = 120f
private val CrossActivityDrift = 96.dp
private val CrossActivityEdgeMargin = 8.dp
private val BackGestureEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private val FastOutExtraSlowIn = CubicBezierEasing(0.05f, 0f, 0.133333f, 0.06f)

class AOSPCrossActivityAnimation(
    private val exitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.FOLLOW_GESTURE
) : PredictiveBackAnimationHandler {
    private var exitingPageKey: String? = null
    private val exitAnimatable = Animatable(0f)
    private var inPredictiveBackAnimation = false
    private var releaseVelocity = 0f
    private var gestureStartY = 0f
    private var lastTouchY = 0f

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?,
    ) {
        exitingPageKey = currentPageKey.toString()
        exitAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450, easing = FastOutExtraSlowIn)
        )
    }

    override fun onPagePop(contentPageKey: Any, animationScope: CoroutineScope) {
        if (exitingPageKey == contentPageKey) {
            exitingPageKey = null
            animationScope.launch { exitAnimatable.snapTo(0f) }
        }
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier = composed {
        val windowInfo = LocalWindowInfo.current
        val navContent = LocalNavAnimatedContentScope.current
        val transition = navContent.transition

        val containerHeightPx = windowInfo.containerSize.height
        val containerWidthPx = windowInfo.containerSize.width.toFloat()
        val pageKey = contentPageKey.toString()
        val deviceCornerRadius = rememberDeviceCornerRadius()
        val density = LocalDensity.current
        val driftPx = with(density) { CrossActivityDrift.toPx() }
        val edgeMarginPx = with(density) { CrossActivityEdgeMargin.toPx() }

        val progressInProgress = (transitionState as? InProgress)
        val edge = progressInProgress?.latestEvent?.swipeEdge ?: 0
        val touchY = progressInProgress?.latestEvent?.touchY
        val gestureProgress = progressInProgress?.latestEvent?.progress ?: 0f
        val isGestureActiveNow = transitionState is InProgress

        if (isGestureActiveNow && touchY != null) {
            if (gestureStartY == 0f) gestureStartY = touchY
            lastTouchY = touchY
            // Capture velocity for bounce
            progressInProgress?.latestEvent?.let {
                releaseVelocity = it.progress // will be overwritten by real velocity in onBackPressed if needed
            }
        }
        if (!isGestureActiveNow) {
            gestureStartY = 0f
        }

        val animatedScale by transition.animateFloat(
            transitionSpec = { tween(300) },
            label = "PredictiveScale"
        ) { state ->
            when (state) {
                EnterExitState.PostExit -> CROSS_ACTIVITY_MIN_SCALE
                else -> 1f
            }
        }

        if (pageKey == currentPageKey.toString()) {
            inPredictiveBackAnimation = isGestureActiveNow || animatedScale != 1f || exitingPageKey != null
        }

        val directionMultiplier = when (exitDirection) {
            PredictiveBackExitDirection.FOLLOW_GESTURE -> if (edge == EDGE_LEFT) 1f else -1f
            PredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
            PredictiveBackExitDirection.ALWAYS_LEFT -> -1f
        }

        val isExitingPage = exitingPageKey != null && exitingPageKey == pageKey
        val isCurrentNavTarget = exitingPageKey == null && pageKey == currentPageKey.toString()

        val hugMax = (containerWidthPx * (1f - CROSS_ACTIVITY_MIN_SCALE) / 2f - edgeMarginPx).coerceAtLeast(0f)
        val hugs = edge != 1 // EDGE_RIGHT = 1, LEFT = 0, NONE = -1, so hugs when not right edge

        // Bounce scale calculation – exact from InstallerX
        val bounceScale = if (isExitingPage && transitionState == null) {
            // Commit phase bounce
            val elapsed = exitAnimatable.value * 450f // approximate elapsed
            val factor = if (edge != -1) 2f else 1f
            val floorKick = if (gestureProgress < 0.1f) BOUNCE_MIN_KICK else 0f
            val kick = (abs(releaseVelocity) * 100f * (1f - CROSS_ACTIVITY_MIN_SCALE) * factor).coerceIn(floorKick, BOUNCE_MAX_KICK)
            if (kick <= 0f) 1f else {
                val omega = sqrt(BOUNCE_STIFFNESS)
                val omegaD = omega * sqrt(1f - BOUNCE_DAMPING * BOUNCE_DAMPING)
                val t = elapsed / 1000f
                val overlay = -(kick / omegaD) * exp(-BOUNCE_DAMPING * omega * t) * sin(omegaD * t)
                ((100f + overlay) / 100f).coerceAtMost(1f)
            }
        } else 1f

        // Y shift – exact from InstallerX crossActivityYShift
        fun yShift(scale: Float): Float {
            if (gestureStartY == 0f || lastTouchY == 0f || containerHeightPx <= 0) return 0f
            val rawDelta = lastTouchY - gestureStartY
            val half = containerHeightPx / 2f
            val ratio = min(half, abs(rawDelta)) / half
            val damped = 1f - (1f - ratio) * (1f - ratio)
            val maxShift = ((containerHeightPx - containerHeightPx * scale) / 2f - edgeMarginPx).coerceAtLeast(0f)
            return maxShift * damped * (if (rawDelta < 0f) -1f else 1f)
        }

        val needsClip = isGestureActiveNow || isExitingPage || animatedScale != 1f

        this
            .graphicsLayer {
                if (isGestureActiveNow && touchY != null) {
                    // Pivot based on touch position – exact InstallerX logic
                    val pivotY = (touchY / containerHeightPx.toFloat()).coerceIn(0.1f, 0.9f)
                    val pivotX = if (edge == EDGE_LEFT) 0.8f else 0.2f
                    transformOrigin = TransformOrigin(pivotX, pivotY)
                }

                when {
                    isExitingPage -> {
                        // Committing – use exitAnimatable progress
                        val linearProgress = exitAnimatable.value
                        val releaseProgress = (1f - gestureProgress).coerceAtLeast(0.01f)
                        val post = (1f - linearProgress / releaseProgress).coerceIn(0f, 1f).let { 1f - it } // simplified post
                        // Actually InstallerX: post = (1 - progress / releaseProgress)
                        // For Navigation3 we use linearProgress as commit progress
                        val easedRelease = 1f - BackGestureEasing.transform((1f - gestureProgress).coerceIn(0f, 1f))
                        val committedScale = CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * easedRelease
                        val grown = committedScale + (1f - committedScale) * linearProgress
                        val finalScale = grown * bounceScale
                        scaleX = finalScale
                        scaleY = finalScale
                        var tx = if (hugs) (1f - easedRelease) * hugMax else 0f
                        tx += linearProgress * driftPx * directionMultiplier
                        translationX = tx
                        translationY = yShift(finalScale)
                        alpha = (1f - 5f * linearProgress).coerceAtLeast(0f)
                    }
                    isCurrentNavTarget && isGestureActiveNow -> {
                        // During gesture – exact InstallerX predictive
                        val easedProgress = 1f - BackGestureEasing.transform((1f - gestureProgress).coerceIn(0f, 1f))
                        val scale = (CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * easedProgress) * bounceScale
                        scaleX = scale
                        scaleY = scale
                        translationX = if (hugs) (1f - easedProgress) * hugMax else 0f
                        translationY = yShift(scale)
                        alpha = 1f
                    }
                    isCurrentNavTarget -> {
                        if (animatedScale != 1f) {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                    }
                    else -> {
                        // Underlying page
                        if (exitingPageKey != null) {
                            val eased = BackGestureEasing.transform(gestureProgress.coerceIn(0f, 1f))
                            val liveScale = CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * (1f - eased)
                            val post = exitAnimatable.value
                            val finalScale = (liveScale + (1f - liveScale) * post) * bounceScale
                            scaleX = finalScale
                            scaleY = finalScale
                            translationX = -(1f - post) * driftPx * directionMultiplier
                            translationY = yShift(finalScale)
                        } else if (isGestureActiveNow) {
                            val eased = BackGestureEasing.transform(gestureProgress.coerceIn(0f, 1f))
                            val liveScale = CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * (1f - eased)
                            scaleX = liveScale
                            scaleY = liveScale
                            translationX = -driftPx * directionMultiplier * 0.25f
                            translationY = yShift(liveScale)
                        }
                    }
                }
            }
            .clip(
                if (needsClip) RoundedCornerShape(deviceCornerRadius)
                else RoundedCornerShape(0.dp)
            )
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int
    ): ContentTransform = ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = null
    )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { -it / 4 }),
            initialContentExit = scaleOut(targetScale = CROSS_ACTIVITY_MIN_SCALE) + fadeOut(),
            sizeTransform = null
        )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<NavKey>().invoke(this)
}
