// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.animation.predictiveback

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEventTransitionState

/**
 * No predictive back - disables predictive gesture entirely.
 * In Lambda Player we use NavigationBackHandler at NavDisplay level,
 * but this decorator also adds a BackHandler to intercept when needed.
 * For simplicity, we provide a variant that takes canPop lambda.
 */
class NoPredictiveBackAnimation(
    private val canPopProvider: () -> Boolean = { true },
    private val onPop: () -> Unit = {}
) : PredictiveBackAnimationHandler {
    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?
    ) {
        // Ignore predictive back gesture progress completely.
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier {
        // Intercept back when we can pop, preventing system predictive dispatch
        if (canPopProvider()) {
            BackHandler {
                onPop()
            }
        }
        return this
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int
    ): ContentTransform = ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = null
    )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform =
        defaultPopTransitionSpec<NavKey>().invoke(this)

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<NavKey>().invoke(this)
}

/**
 * Simpler No animation for use in entryProvider where we don't have composition local.
 * Fixed: must still show submenu close animation when going back (NONE should disable
 * predictive gesture, not disable close animation). Previously used defaultPopTransitionSpec
 * which on some Navigation3 versions returns None, causing missing close animation.
 */
class NoPredictiveBackAnimationSimple : PredictiveBackAnimationHandler {
    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?
    ) {
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier = this

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int
    ): ContentTransform = ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = null
    )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform {
        // Ensure close animation always shows even when predictive = NONE
        // Use same animation as normal submenu pop (InstallerX Revived style)
        return ContentTransform(
            targetContentEnter = dev.shephard.player.ui.navigation.PageTransitions.popEnterSubmenu,
            initialContentExit = dev.shephard.player.ui.navigation.PageTransitions.popExitSubmenu
        )
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform {
        return ContentTransform(
            targetContentEnter = dev.shephard.player.ui.navigation.PageTransitions.enterSubmenu,
            initialContentExit = dev.shephard.player.ui.navigation.PageTransitions.exitSubmenu
        )
    }
}
