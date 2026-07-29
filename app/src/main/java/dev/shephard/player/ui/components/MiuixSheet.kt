package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.shephard.player.ui.glass.blurSheetSurface

/**
 * Miuix/MIUI-style bottom-sheet handle/header.
 *
 * It is intentionally not clickable: tapping the handle does nothing; dragging is handled by
 * ModalBottomSheet itself. This mirrors MIUI drawers and avoids press/ripple/semantics noise.
 */
@Composable
fun MiuixSheetHandle(
    liquidGlassOn: Boolean,
    modifier: Modifier = Modifier
) {
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
            .padding(top = 14.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f))
        )
    }
}

object MiuixSheetDefaults {
    val Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    @Composable
    fun containerColor(liquidGlassOn: Boolean): Color =
        if (liquidGlassOn) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh
}
