package dev.shephard.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Two-step "remove from queue" button shared by the Miuix and Material 3 queue lists.
 *
 * First tap turns the trash icon into a red "Confirm?" pill; a second tap calls [onRemove].
 * If the second tap doesn't come within a few seconds the button falls back to the icon.
 */
@Composable
fun QueueRemoveButton(
    trackId: Long,
    contentDescription: String,
    confirmLabel: String,
    color: Color,
    labelStyle: TextStyle,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by remember(trackId) { mutableStateOf(false) }
    LaunchedEffect(confirming) {
        if (confirming) {
            delay(3_000)
            confirming = false
        }
    }
    val shape = RoundedCornerShape(18.dp)
    AnimatedContent(
        targetState = confirming,
        transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
        label = "queueRemoveConfirm",
        modifier = modifier,
    ) { isConfirming ->
        Box(
            modifier = Modifier
                .height(36.dp)
                .widthIn(min = 36.dp)
                .clip(shape)
                .background(if (isConfirming) color.copy(alpha = 0.14f) else Color.Transparent, shape)
                .clickable(role = Role.Button) {
                    if (isConfirming) {
                        confirming = false
                        onRemove()
                    } else {
                        confirming = true
                    }
                }
                .padding(horizontal = if (isConfirming) 12.dp else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isConfirming) {
                BasicText(
                    text = confirmLabel,
                    style = labelStyle.copy(color = color, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
            } else {
                Image(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = contentDescription,
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
