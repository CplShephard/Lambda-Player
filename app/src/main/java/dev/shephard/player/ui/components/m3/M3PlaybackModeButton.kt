// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components.m3

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Round when off; a softly rounded square in the active state. */
@Composable
fun M3PlaybackModeButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val radius by animateDpAsState(if (active) 14.dp else 24.dp, tween(220), label = "playbackModeShape")
    val background by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        tween(220), label = "playbackModeBackground",
    )
    val foreground by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(220), label = "playbackModeForeground",
    )
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp).clip(RoundedCornerShape(radius)).background(background),
    ) {
        Icon(icon, contentDescription = description, tint = foreground)
    }
}
