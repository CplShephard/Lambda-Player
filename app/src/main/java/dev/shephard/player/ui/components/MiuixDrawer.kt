package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun MiuixDrawer(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    backgroundColor: Color = MiuixDrawerDefaults.backgroundColor(),
    cornerRadius: Dp = MiuixDrawerDefaults.CornerRadius,
    insideMargin: DpSize = MiuixDrawerDefaults.InsideMargin,
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    content: @Composable () -> Unit,
) {

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    WindowBottomSheet(
        show = visible,
        modifier = modifier,
        title = title,
        startAction = startAction,
        endAction = endAction,
        backgroundColor = backgroundColor,
        cornerRadius = cornerRadius,
        insideMargin = insideMargin,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        onDismissRequest = {

            if (allowDismiss) visible = false
        },
        onDismissFinished = { currentOnDismissRequest() },
        content = content,
    )
}

@Composable
fun MiuixDrawerActionHeader(
    title: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixDrawerCircleAction(
            imageVector = MiuixIcons.Close,
            contentDescription = "Cancel",
            containerColor = MiuixTheme.colorScheme.surfaceVariant,
            contentColor = MiuixTheme.colorScheme.onSurface,
            onClick = onCancel,
        )
        Text(
            text = title,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onBackground,
        )
        MiuixDrawerCircleAction(
            imageVector = MiuixIcons.Ok,
            contentDescription = "Apply",
            containerColor = if (confirmEnabled) MiuixTheme.colorScheme.primary
            else MiuixTheme.colorScheme.surfaceVariant,
            contentColor = if (confirmEnabled) MiuixTheme.colorScheme.onPrimary
            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            enabled = confirmEnabled,
            onClick = onConfirm,
        )
    }
}

@Composable
private fun MiuixDrawerCircleAction(
    imageVector: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor)
            .pressScaleClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

object MiuixDrawerDefaults {

    val CornerRadius: Dp = BottomSheetDefaults.cornerRadius

    val InsideMargin: DpSize = DpSize(0.dp, 0.dp)

    @Composable
    fun backgroundColor(): Color = MiuixTheme.colorScheme.surfaceContainer
}

@Composable
fun rememberDrawerDismiss(): () -> Unit {
    val dismiss = LocalDismissState.current
    return remember(dismiss) { { dismiss?.invoke() } }
}

@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    var last by remember { mutableStateOf(value) }
    if (value != null) last = value
    return last
}
