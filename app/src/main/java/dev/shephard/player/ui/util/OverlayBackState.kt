package dev.shephard.player.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf

/**
 * Counts the overlays that are currently open (drawers, bottom sheets, dialogs, dropdowns).
 *
 * While at least one is open, the app-level plain BackHandler in MainContainer steps aside, so the
 * overlay's own back handling gets the gesture and its predictive-back animation runs. Otherwise the
 * plain handler would close the overlay (or change the page) immediately without any preview.
 */
object OverlayBackState {
    private val openCount = mutableIntStateOf(0)

    val hasOpenOverlay: Boolean
        get() = openCount.intValue > 0

    internal fun onOpened() {
        openCount.intValue++
    }

    internal fun onClosed() {
        if (openCount.intValue > 0) openCount.intValue--
    }
}

/**
 * Marks an overlay as open for as long as this composable is in the composition (and [active] is true).
 */
@Composable
fun TrackOverlayForBack(active: Boolean = true) {
    DisposableEffect(active) {
        if (active) {
            OverlayBackState.onOpened()
            onDispose { OverlayBackState.onClosed() }
        } else {
            onDispose { }
        }
    }
}
