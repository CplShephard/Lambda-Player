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
    ;

    companion object {
        // Any stored value that no longer exists (e.g. the removed "scale" style)
        // silently falls back to MIUIX.
        fun fromValueOrDefault(value: String) = entries.find { it.value == value } ?: MIUIX
    }
}

/**
 * Which edge the AOSP predictive pop travels towards.
 *
 * MIUIX always pops towards the right, so it does not expose this setting and
 * [effectiveExitDirection] pins it to [ALWAYS_RIGHT] regardless of what was
 * last stored for another style.
 */
enum class PredictiveBackExitDirection(val value: String, val displayName: String) {
    FOLLOW_GESTURE("follow_gesture", "Follow Gesture"),
    ALWAYS_RIGHT("always_right", "Always Right"),
    ALWAYS_LEFT("always_left", "Always Left"),
    ;

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.value == value } ?: FOLLOW_GESTURE
    }
}

/**
 * Exit direction that actually applies for [animation]. Selecting MIUIX forces
 * [PredictiveBackExitDirection.ALWAYS_RIGHT]; the stored preference is only
 * honoured by styles that expose the option.
 */
fun effectiveExitDirection(
    animation: PredictiveBackAnimation,
    stored: PredictiveBackExitDirection,
): PredictiveBackExitDirection = when (animation) {
    PredictiveBackAnimation.MIUIX -> PredictiveBackExitDirection.ALWAYS_RIGHT
    else -> stored
}
