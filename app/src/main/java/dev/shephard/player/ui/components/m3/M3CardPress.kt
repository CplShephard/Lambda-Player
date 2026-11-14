// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components.m3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Bounded Material ripple plus a subtle animated state layer above card content.
 * The same shape clips both the ripple and the overlay, including image-backed cards.
 * Nested buttons retain their own interaction sources and click handling.
 */
@Composable
fun Modifier.m3CardPress(
    shape: Shape,
    enabled: Boolean = true,
    overlayColor: Color = MaterialTheme.colorScheme.onSurface,
    interactionSource: MutableInteractionSource? = null,
    showOverlay: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val defaultInteractions = remember { MutableInteractionSource() }
    val interactions = interactionSource ?: defaultInteractions
    return this
        .clip(shape)
        .clickable(
            enabled = enabled,
            interactionSource = interactions,
            indication = ripple(color = overlayColor),
            onClick = onClick,
        )
        .then(if (showOverlay) Modifier.m3CardPressLayer(interactions, shape, overlayColor, enabled) else Modifier)
}

/** An independently clipped state layer, e.g. for artwork inside a larger clickable tile. */
@Composable
fun Modifier.m3CardPressLayer(
    interactions: MutableInteractionSource,
    shape: Shape,
    color: Color,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactions.collectIsPressedAsState()
    val opacity by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.11f else 0f,
        animationSpec = tween(if (pressed) 80 else 180),
        label = "m3CardPressedLayer",
    )
    return this.clip(shape).drawWithContent {
        drawContent()
        if (opacity > 0f) drawRect(color.copy(alpha = opacity))
    }
}
