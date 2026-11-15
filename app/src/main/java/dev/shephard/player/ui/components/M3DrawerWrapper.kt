// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import dev.shephard.player.ui.util.TrackOverlayForBack
import top.yukonga.miuix.kmp.nav.gesture.WindowNavigationEventBridge

/**
 * Native M3 drawer sheet. Drawer sheets are an intentional exception to the
 * predictive-back preference: keep the framework sheet and its draggable
 * handle active so both dragging and predictive dismissal continue to work.
 * Predictive back runs through M3's own sheet handler, bridged to this window below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3BottomSheetWrapper(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    TrackOverlayForBack()
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val windowWidth = LocalConfiguration.current.screenWidthDp.dp
    val drawerWidth = minOf(420.dp, windowWidth * 0.88f)
    val leftAlignedOffset = -(windowWidth - drawerWidth) / 2
    var closing by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetMaxWidth = drawerWidth,
        containerColor = containerColor,
        modifier = Modifier.absoluteOffset(x = leftAlignedOffset).then(modifier),
    ) {
        // ModalBottomSheet is a separate Android window, but its built-in PredictiveBackHandler
        // registers on the inherited LocalNavigationEventDispatcherOwner (our NavDisplay entry
        // dispatcher). Without this bridge that handler never sees the sheet window's back
        // gesture, so the sheet closed instantly with no predictive scale/slide preview.
        WindowNavigationEventBridge()
        CompositionLocalProvider(LocalDrawerCloseAction provides { action ->
            if (!closing) {
                closing = true
                scope.launch {
                    try {
                        sheetState.hide()
                        if (!sheetState.isVisible) {
                            currentOnDismiss()
                            action()
                        }
                    } finally { closing = false }
                }
            }
        }) { content() }
    }
}
