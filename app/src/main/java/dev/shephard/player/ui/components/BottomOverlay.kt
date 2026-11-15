package dev.shephard.player.ui.components

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Space needed after scrollable content so its final item can move clear of the live bottom dock.
 * The fallback covers the first frame and screens rendered without the main dock; subsequent
 * composition uses the measured mini-player + dock height instead of a device-specific constant.
 */
@androidx.compose.runtime.Composable
fun bottomOverlayContentPadding(fallback: Dp, extraSpace: Dp = 16.dp): Dp {
    val measuredHeight = LocalBottomOverlayHeight.current.value
    return maxOf(fallback, measuredHeight + extraSpace)
}

/**
 * Measured height of everything docked at the bottom of the main screen (mini player + nav dock,
 * including system navigation insets). Screens with floating buttons read it so those buttons sit
 * above the whole stack instead of relying on fixed offsets that drift from the real layout.
 */
val LocalBottomOverlayHeight = compositionLocalOf<State<Dp>> { mutableStateOf(0.dp) }
