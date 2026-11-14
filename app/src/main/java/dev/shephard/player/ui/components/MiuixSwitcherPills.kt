// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text

/** Horizontally scrollable switcher shared by Miuix stats and library. */
@Composable
internal fun <T> MiuixSwitcherPills(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowScrollState = rememberScrollState()
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rowScrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val cs = MiuixAppTheme.colorScheme
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) cs.primary else cs.surfaceVariant.copy(alpha = 0.6f))
                    .clickable { onSelected(value) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = if (isSelected) cs.onPrimary else cs.onSurfaceVariant,
                    style = MiuixAppTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
