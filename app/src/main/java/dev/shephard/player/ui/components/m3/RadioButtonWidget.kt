// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.components.m3

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun RadioButtonWidget(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = true,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    BaseWidget(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.RadioButton
            this.selected = selected
        },
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        description = description,
        selected = selected,
        enabled = enabled,
        onClick = onClick
    ) { interactionSource ->
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.clearAndSetSemantics {},
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = LocalContentColor.current
            ),
            interactionSource = interactionSource
        )
    }
}
