// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.animation.predictiveback

import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

fun installerNavTransition(animation: PredictiveBackAnimation, exitDirection: PredictiveBackExitDirection): NavTransition = when (animation) {
    PredictiveBackAnimation.NONE -> NoPredictiveBackTransition
    PredictiveBackAnimation.MIUIX -> NavTransitions.MiuixDefault
    PredictiveBackAnimation.AOSP -> AospNavTransition
    PredictiveBackAnimation.SCALE -> scaleNavTransition(exitDirection)
    PredictiveBackAnimation.CLASSIC -> ClassicNavTransition
}
