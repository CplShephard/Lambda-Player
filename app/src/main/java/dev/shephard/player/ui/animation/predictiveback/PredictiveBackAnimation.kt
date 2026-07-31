// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package dev.shephard.player.ui.animation.predictiveback

/**
 * Define Predictive Back Animation types
 */
enum class PredictiveBackAnimation(val value: String) {
    None("none"),
    AOSP("aosp"),
    MIUIX("miuix"),
    Scale("scale"),
    Classic("ksu_classic");

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.value == value } ?: MIUIX
    }
}
