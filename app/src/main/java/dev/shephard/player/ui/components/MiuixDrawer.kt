package dev.shephard.player.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.theme.PredictiveBackAnimation
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import dev.shephard.player.ui.i18n.LocalStrings

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
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val predictiveBack by prefs.predictiveBackAnimation.collectAsState(initial = PredictiveBackAnimation.MIUIX)
    val isPredictiveEnabled = predictiveBack != PredictiveBackAnimation.NONE


    if (!isPredictiveEnabled) {
        // NONE: predictive back fully disabled – use non-predictive Dialog bottom sheet
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

        // Handle dismiss outside AnimatedVisibility to avoid touch block
        LaunchedEffect(visible) {
            if (!visible) {
                kotlinx.coroutines.delay(260)
                currentOnDismissRequest()
            }
        }

        // Use legacy BackHandler (non-predictive) when predictive is disabled
        BackHandler(enabled = visible && allowDismiss) {
            visible = false
        }

        Dialog(
            onDismissRequest = {
                if (allowDismiss) {
                    visible = false
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .then(
                        if (allowDismiss) Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = { visible = false }
                            )
                        else Modifier
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
                            .background(backgroundColor)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            LocalDismissState provides {
                                visible = false
                            }
                        ) {
                            content()
                        }
                    }
                }
            }
        }
        return
    }


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
            contentDescription = LocalStrings.current.cancel,
            containerColor = Color.Transparent,
            contentColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
            contentDescription = LocalStrings.current.apply,
            containerColor = Color.Transparent,
            contentColor = if (confirmEnabled) MiuixTheme.colorScheme.onBackground
            else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f),
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
            modifier = Modifier.size(22.dp),
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
