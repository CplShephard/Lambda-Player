// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package dev.shephard.player.ui.util

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dynamically calculates the device's physical corner radius.
 * @param defaultRadius The fallback radius to use if the API is lower than 31
 * or if the device does not report rounded corners.
 * @return The calculated corner radius in Dp.
 */
@Composable
fun rememberDeviceCornerRadius(defaultRadius: Dp = 28.dp): Dp {
    val view = LocalView.current
    val density = LocalDensity.current

    return remember(view, density) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val insets = view.rootWindowInsets
            if (insets != null) {
                val corner = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                    ?: insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
                    ?: insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
                    ?: insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)

                if (corner != null) {
                    with(density) {
                        return@remember corner.radius.toDp()
                    }
                }
            }
        }
        defaultRadius
    }
}
