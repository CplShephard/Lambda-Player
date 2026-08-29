// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package dev.shephard.player.ui.components.m3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.shephard.player.ui.theme.CornerRadius

val LocalSegmentedItemShape = compositionLocalOf<Shape> { RoundedCornerShape(CornerRadius) }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BaseWidget(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    iconPlaceholder: Boolean = true,
    title: String,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    description: String? = null,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    descriptionColor: Color? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    clickHaptic: HapticFeedbackType? = HapticFeedbackType.VirtualKey,
    foreContent: @Composable BoxScope.() -> Unit = {},
    trailingContent: @Composable BoxScope.(interactionSource: MutableInteractionSource) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val alpha = if (enabled) 1f else 0.38f

    val interactionSource = remember { MutableInteractionSource() }

    val density = LocalDensity.current
    val dynamicInternalPadding = (4 * density.fontScale).dp

    val baseShape = LocalSegmentedItemShape.current

    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }

    val baseContentColor = if (selected) {
        MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primaryContainer)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val resolvedIconColor = iconColor
        ?: if (selected) {
            baseContentColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    val finalDescriptionColor = when {
        isError -> MaterialTheme.colorScheme.error
        descriptionColor != null -> descriptionColor
        else -> baseContentColor.copy(alpha = 0.7f)
    }

    val colors = ListItemDefaults.colors(
        containerColor = backgroundColor,
        contentColor = baseContentColor,
        leadingContentColor = resolvedIconColor,
        trailingContentColor = resolvedIconColor,
        supportingContentColor = finalDescriptionColor,

        selectedContainerColor = backgroundColor,
        selectedContentColor = baseContentColor,
        selectedLeadingContentColor = resolvedIconColor,
        selectedTrailingContentColor = resolvedIconColor,
        selectedSupportingContentColor = finalDescriptionColor,

        disabledContainerColor = backgroundColor,
        disabledContentColor = baseContentColor,
        disabledLeadingContentColor = resolvedIconColor,
        disabledTrailingContentColor = resolvedIconColor,
        disabledSupportingContentColor = finalDescriptionColor
    )

    val shapes = ListItemDefaults.shapes(
        shape = baseShape,
        pressedShape = RoundedCornerShape(CornerRadius),
        selectedShape = baseShape,
        focusedShape = baseShape,
        hoveredShape = baseShape
    )

    val itemModifier = modifier.fillMaxWidth()

    val leadingContent: (@Composable () -> Unit)? =
        if (icon != null || iconPlaceholder) {
            {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = resolvedIconColor
                        )
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            }
        } else {
            null
        }

    val supportingContent: (@Composable () -> Unit)? =
        description?.let { text ->
            {
                Text(
                    text = text,
                    style = descriptionStyle,
                    modifier = Modifier
                        .alpha(alpha)
                        .padding(bottom = dynamicInternalPadding)
                )
            }
        }

    val trailing: @Composable () -> Unit = {
        Box(
            modifier = Modifier.alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            trailingContent(interactionSource)
        }
    }

    val headline: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .alpha(alpha)
                .padding(
                    top = dynamicInternalPadding,
                    bottom = if (description == null) dynamicInternalPadding else 0.dp
                )
        ) {
            Text(
                text = title,
                style = titleStyle
            )

            foreContent()
        }
    }

    if (onClick != null) {
        ListItem(
            selected = selected,
            modifier = itemModifier,
            onClick = {
                clickHaptic?.let { haptic.performHapticFeedback(it) }
                onClick()
            },
            enabled = enabled,
            colors = colors,
            shapes = shapes,
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = leadingContent,
            supportingContent = supportingContent,
            trailingContent = trailing,
            interactionSource = interactionSource,
            content = headline
        )
    } else {
        ListItem(
            headlineContent = headline,
            modifier = itemModifier
                .clip(baseShape)
                .then(
                    if (!enabled) {
                        Modifier.semantics { disabled() }
                    } else {
                        Modifier
                    }
                ),
            colors = colors,
            leadingContent = leadingContent,
            supportingContent = supportingContent,
            trailingContent = trailing
        )
    }
}
