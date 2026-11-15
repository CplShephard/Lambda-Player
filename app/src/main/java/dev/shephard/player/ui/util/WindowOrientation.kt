// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** True when the app window is wider than it is tall, including freeform/resizable windows. */
@Composable
fun isLandscapeWindow(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenWidthDp > configuration.screenHeightDp
}
