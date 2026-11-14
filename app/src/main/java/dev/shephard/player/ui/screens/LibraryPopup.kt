// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.shephard.player.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.shephard.player.ui.util.TrackOverlayForBack
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.nav.gesture.WindowNavigationEventBridge
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.window.WindowListPopup

/** The same anchored popup primitives used by the UI-engine selectors. */
@Composable
internal fun LibraryPopupButton(miuix: Boolean, icon: ImageVector, description: String, color: Color,
    choices: List<Triple<String, Boolean, () -> Unit>>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    Box(modifier) {
        LibraryHeaderButton(miuix, icon, description, color) { expanded = true }
        if (miuix) {
            TrackOverlayForBack(active = expanded)
            WindowListPopup(show = expanded, alignment = PopupPositionProvider.Align.End,
                onDismissRequest = { expanded = false },
                onDismissFinished = { pending?.invoke(); pending = null }) {
                WindowNavigationEventBridge()
                val dismiss = LocalDismissState.current
                val colors = DropdownDefaults.dropdownColors()
                ListPopupColumn {
                    choices.forEachIndexed { index, choice ->
                        DropdownImpl(text = choice.first, optionSize = choices.size,
                            isSelected = choice.second, index = index, dropdownColors = colors,
                            onSelectedIndexChange = { selected ->
                                pending = choices.getOrNull(selected)?.third
                                if (dismiss != null) dismiss.invoke() else expanded = false
                            })
                    }
                }
            }
        } else {
            if (expanded) TrackOverlayForBack()
            DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                    choices.forEachIndexed { index, choice ->
                        SelectableDropdownMenuItem(selected = choice.second,
                            onClick = { expanded = false; choice.third() },
                            text = { androidx.compose.material3.Text(choice.first) },
                            shapes = MenuDefaults.itemShape(index = index, count = choices.size))
                    }
                }
            }
        }
    }
}
