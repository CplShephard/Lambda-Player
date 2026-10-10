package dev.shephard.player.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shephard.player.ui.util.TrackOverlayForBack
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.nav.gesture.WindowNavigationEventBridge
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

/**
 * Miuix spinner row (same look, padding and anchored popup as the library's
 * `WindowSpinnerPreference` in popup mode) with working predictive back.
 *
 * Why not `WindowSpinnerPreference` directly: the popup is a separate Android window, and
 * miuix-ui 0.9.3 registers its predictive-back handler (shrink + fade while swiping) on the
 * *inherited* `LocalNavigationEventDispatcherOwner`. Our miuix-nav 0.9.4 `NavDisplay` provides
 * its own entry dispatcher, so that handler never received the popup window's back events and the
 * popup was closed instantly by the platform dialog callback, without any predictive animation.
 * Here the popup content is ours, so [WindowNavigationEventBridge] can forward the popup window's
 * back gesture to the dispatcher the library handler is registered on.
 */
@Composable
internal fun MiuixSpinnerRow(
    title: String,
    summary: String? = null,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var holdDown by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    val currentOnSelectedIndexChange by rememberUpdatedState(onSelectedIndexChange)
    val hasItems = items.isNotEmpty()

    // Keeps the app-level plain BackHandler out of the way while the popup is open.
    TrackOverlayForBack(active = expanded)

    val actionColor = if (hasItems) {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    } else {
        MiuixTheme.colorScheme.disabledOnSecondaryVariant
    }
    val dropdownColors = DropdownDefaults.dropdownColors()

    BasicComponent(
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        title = title,
        summary = summary,
        endActions = {
            val selectedText = items.getOrNull(selectedIndex)
            if (!selectedText.isNullOrBlank()) {
                Text(
                    text = selectedText,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .weight(1f, fill = false),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = actionColor,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownArrowEndAction(actionColor = actionColor)
            if (hasItems) {
                WindowListPopup(
                    show = expanded,
                    alignment = PopupPositionProvider.Align.End,
                    onDismissRequest = { expanded = false },
                    onDismissFinished = { holdDown = false },
                ) {
                    // One bridge per window: forwards this popup window's predictive back
                    // events to the dispatcher that the library's popup back handler uses.
                    WindowNavigationEventBridge()
                    val dismiss = LocalDismissState.current
                    ListPopupColumn {
                        items.forEachIndexed { index, text ->
                            key(index) {
                                DropdownImpl(
                                    text = text,
                                    optionSize = items.size,
                                    isSelected = index == selectedIndex,
                                    index = index,
                                    dropdownColors = dropdownColors,
                                    onSelectedIndexChange = { picked ->
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                        currentOnSelectedIndexChange(picked)
                                        dismiss?.invoke()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        onClick = {
            if (hasItems) {
                expanded = !expanded
                if (expanded) {
                    holdDown = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            }
        },
        role = Role.DropdownList,
        holdDownState = holdDown,
        enabled = hasItems,
    )
}
