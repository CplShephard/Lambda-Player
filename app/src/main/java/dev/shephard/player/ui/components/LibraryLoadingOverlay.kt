package dev.shephard.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Icon as MiuixIcon
import dev.shephard.player.ui.miuix.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixLibraryLoadingOverlay() {
    val infinite = rememberInfiniteTransition(label = "miuixLoadingSpin")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixAppTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiuixIcon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = MiuixAppTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .rotate(rotation)
            )
            Spacer(Modifier.height(14.dp))
            MiuixText(
                text = "Loading library",
                style = MiuixTheme.textStyles.title3.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                color = MiuixAppTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun M3LibraryLoadingOverlay() {
    val infinite = rememberInfiniteTransition(label = "m3LoadingSpin")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .rotate(rotation)
            )
            Spacer(Modifier.height(14.dp))
            M3Text(
                text = "Loading library",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LibraryLoadingOverlay(useMiuix: Boolean) {
    if (useMiuix) MiuixLibraryLoadingOverlay() else M3LibraryLoadingOverlay()
}
