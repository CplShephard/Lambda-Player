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
    SCALE("scale", "Scale"),
    CLASSIC("ksu_classic", "Classic"),
    ;

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.value == value } ?: MIUIX
    }
}

/**
 * Which edge the scaled/classic predictive pop travels towards.
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
