// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package dev.shephard.player.theme

/**
 * Predictive back animation styles, identical to InstallerX Revived's
 * `PredictiveBackAnimation` model so both UI engines can share the setting.
 */
enum class PredictiveBackAnimation(val value: String, val displayName: String) {
    NONE("none", "None"),
    AOSP("aosp", "AOSP"),
    MIUIX("miuix", "MIUIX"),
    CLASSIC("ksu_classic", "Classic"),
    SCALE("scale", "Scale"),
    ;

    companion object {
        // Any unknown stored value silently falls back to MIUIX.
        fun fromValueOrDefault(value: String) = entries.find { it.value == value } ?: MIUIX
    }
}

/**
 * Which edge the predictive pop travels towards.
 *
 * There is no user-facing picker for this any more: MIUIX always pops towards the right and
 * every other style follows the gesture. See [effectiveExitDirection].
 */
enum class PredictiveBackExitDirection {
    FOLLOW_GESTURE,
    ALWAYS_RIGHT,
}

/** Exit direction for [animation]: MIUIX always exits to the right, the rest follow the gesture. */
fun effectiveExitDirection(animation: PredictiveBackAnimation): PredictiveBackExitDirection = when (animation) {
    PredictiveBackAnimation.MIUIX -> PredictiveBackExitDirection.ALWAYS_RIGHT
    else -> PredictiveBackExitDirection.FOLLOW_GESTURE
}
