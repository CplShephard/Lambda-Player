// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.ui.i18n.Strings
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text

@Composable
fun MiuixLayoutSelector(
    layout: Int,
    strings: Strings,
    onLayoutChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MiuixAppTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LayoutSegment(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.ViewList,
            label = strings.list,
            selected = layout == LayoutMode.LIST,
            onClick = { onLayoutChange(LayoutMode.LIST) },
        )
        LayoutSegment(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.GridView,
            label = strings.grid,
            selected = layout == LayoutMode.GRID,
            onClick = { onLayoutChange(LayoutMode.GRID) },
        )
    }
}

@Composable
private fun RowScope.LayoutSegment(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            color = color,
            style = MiuixAppTheme.typography.labelLarge,
        )
    }
}
