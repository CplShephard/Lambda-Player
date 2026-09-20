// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
// Adapted for Lambda Player
package dev.shephard.player.ui.animation.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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

class ScalePredictiveBackAnimation(
    private val exitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.ALWAYS_RIGHT
) : PredictiveBackAnimationHandler {
    private var exitingPageKey: String? = null
    private val exitAnimatable = Animatable(0f)
    private var inPredictiveBackAnimation = false

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?,
    ) {
        // Always trigger exit animation on back press, regardless of inPredictive flag
        exitingPageKey = currentPageKey.toString()
        exitAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        )
        exitAnimatable.snapTo(0f)
    }

    override fun onPagePop(contentPageKey: Any, animationScope: CoroutineScope) {
        if (exitingPageKey == contentPageKey) {
            exitingPageKey = null
        }
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier {
        val windowInfo = LocalWindowInfo.current
        val navContent = LocalNavAnimatedContentScope.current

        val containerHeightPx = windowInfo.containerSize.height
        val containerWidthPx = windowInfo.containerSize.width.toFloat()
        val pageKey = contentPageKey.toString()
        val transition = navContent.transition
        val deviceCornerRadius = rememberDeviceCornerRadius()

        val isGestureActiveNow = transitionState is InProgress
        val progressInProgress = transitionState as? InProgress
        val edge = progressInProgress?.latestEvent?.swipeEdge ?: 0
        val touchY = progressInProgress?.latestEvent?.touchY
        val gestureProgress = progressInProgress?.latestEvent?.progress ?: 0f

        val modifier =
            if (pageKey == currentPageKey.toString() || exitingPageKey == pageKey) {
                val animatedScale by transition.animateFloat(
                    transitionSpec = { tween(300) },
                    label = "PredictiveScale"
                ) { state ->
                    when (state) {
                        EnterExitState.PostExit -> 0.85f
                        else -> 1f
                    }
                }

                inPredictiveBackAnimation = isGestureActiveNow || animatedScale != 1f || exitingPageKey != null

                val currentPivotY = if (touchY != null && containerHeightPx > 0) {
                    (touchY / containerHeightPx).coerceIn(0.1f, 0.9f)
                } else 0.5f

                val currentPivotX = if (edge == EDGE_LEFT) 0.8f else 0.2f

                val directionMultiplier = when (exitDirection) {
                    PredictiveBackExitDirection.FOLLOW_GESTURE -> if (edge == EDGE_LEFT) 1f else -1f
                    PredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
                    PredictiveBackExitDirection.ALWAYS_LEFT -> -1f
                }

                val exitProgress =
                    if (pageKey != currentPageKey.toString()) 1f else exitAnimatable.value

                // During gesture, use gestureProgress for scale, not just exitAnimatable
                val gestureScale = 1f - (1f - 0.85f) * gestureProgress
                val scaleToUse = if (isGestureActiveNow) gestureScale else animatedScale
                val animatedTranslationX = if (isGestureActiveNow) {
                    // During gesture, don't translate yet, just scale
                    0f
                } else {
                    containerWidthPx * exitProgress * directionMultiplier
                }
                val needsClip = inPredictiveBackAnimation

                this
                    .graphicsLayer {
                        scaleX = scaleToUse
                        scaleY = scaleToUse
                        translationX = animatedTranslationX
                        transformOrigin = TransformOrigin(currentPivotX, currentPivotY)
                    }
                    .clip(
                        if (needsClip) RoundedCornerShape(deviceCornerRadius)
                        else RoundedCornerShape(0.dp)
                    )
            } else {
                val renderModifier = if (isGestureActiveNow) {
                    val progress = if (!inPredictiveBackAnimation) gestureProgress else exitAnimatable.value
                    val dynamicAlpha = 0.5f * (1f - progress)

                    this
                        .graphicsLayer()
                        .drawWithContent {
                            drawContent()
                            drawRect(color = Color.Black.copy(alpha = dynamicAlpha))
                        }
                } else Modifier

                renderModifier
            }

        return modifier
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
            targetContentEnter = slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn(),
            initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
            sizeTransform = null
        )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<NavKey>().invoke(this)
}
