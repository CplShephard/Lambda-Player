package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.shephard.player.ui.glass.blurSheetSurface
import kotlinx.coroutines.launch

/**
 * Bottom-sheet handle that never closes on tap. It closes only after an intentional
 * downward drag past [thresholdDp]. No ripple/press indication, no TalkBack "drag handle"
 * label/press animation noise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlledSheetDragHandle(
    sheetState: SheetState,
    liquidGlassOn: Boolean,
    onAllowDismiss: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    thresholdDp: androidx.compose.ui.unit.Dp = 64.dp,
) {
    val scope = rememberCoroutineScope()
    val thresholdPx = with(LocalDensity.current) { thresholdDp.toPx() }
    var dragAmountY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (liquidGlassOn) {
                    Modifier.blurSheetSurface(
                        enabled = true,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                } else Modifier
            )
            .padding(vertical = 10.dp)
            .clearAndSetSemantics { }
            .pointerInput(sheetState) {
                detectVerticalDragGestures(
                    onDragStart = { dragAmountY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0f) {
                            change.consume()
                            dragAmountY += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (dragAmountY > thresholdPx) {
                            onAllowDismiss()
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                        dragAmountY = 0f
                    },
                    onDragCancel = { dragAmountY = 0f }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
        )
    }
}
