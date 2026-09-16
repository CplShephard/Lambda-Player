// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
// Adapted for Lambda Player
package dev.shephard.player.ui.animation.predictiveback

import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection

fun predictiveBackHandler(
    animation: PredictiveBackAnimation,
    exitDirection: PredictiveBackExitDirection
): PredictiveBackAnimationHandler = when (animation) {
    PredictiveBackAnimation.NONE -> NoPredictiveBackAnimationSimple()
    PredictiveBackAnimation.MIUIX -> MiuixPredictiveBackAnimation()
    PredictiveBackAnimation.AOSP -> AOSPCrossActivityAnimation(exitDirection)
    PredictiveBackAnimation.SCALE -> ScalePredictiveBackAnimation(exitDirection)
    PredictiveBackAnimation.CLASSIC -> ClassicPredictiveBackAnimation()
}
