package dev.shephard.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.slideForwardInQueue
import dev.shephard.player.data.trackById
import dev.shephard.player.player.PlayerUiState
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.miuixBlurSurface

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return

    // Geçiş yönü AnimatedContent içinde kuyruk konumuna göre belirlenir.

    val fraction = if (state.durationMs > 0L)
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 250),
        label = "miniProgress"
    )
    val glow = Color(state.glowColorArgb)
    val animatedGlow by animateColorAsState(
        targetValue = glow,
        animationSpec = tween(durationMillis = 450),
        label = "miniGlowColor"
    )

    val liquidGlassOn = LocalBlurEnabled.current
    val contentBackdrop = LocalContentBackdrop.current
    val miniPlayerShape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(miniPlayerShape)
                .then(
                    if (liquidGlassOn && contentBackdrop != null) {
                        Modifier
                            .miuixBlurSurface(
                                backdrop = contentBackdrop,
                                shape = miniPlayerShape,
                                blurRadius = 28f,
                                tintAlpha = 0.58f,
                                fallbackColor = MiuixAppTheme.colorScheme.surfaceVariant
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        animatedGlow.copy(alpha = 0.22f),
                                        Color.Transparent,
                                        animatedGlow.copy(alpha = 0.10f)
                                    )
                                ),
                                miniPlayerShape
                            )
                    } else {
                        Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    animatedGlow.copy(alpha = 0.34f),
                                    MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                    animatedGlow.copy(alpha = 0.18f)
                                )
                            )
                        )
                    }
                )
                .bounceClick(pressScale = 0.97f) { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Album art + track info: TEK bir AnimatedContent içinde birlikte animasyonlanır.
            // Önceden kapak ve metin ayrı AnimatedContent bloklarıydı; aynı transitionSpec'i
            // kullansalar da Compose bunları bağımsız iki "oyuncu" gibi ele alıyordu, bu da
            // şarkı değişiminde ikisinin birbirinden kopuk hareket ediyormuş gibi görünmesine
            // yol açıyordu. Artık tek content bloğu olduğu için ikisi de aynı animasyonun
            // parçası ve tam senkron kayıyorlar.
            AnimatedContent(
                targetState = track.id,
                transitionSpec = {
                    val dir = if (slideForwardInQueue(state.queue, initialState, targetState))
                        AnimatedContentTransitionScope.SlideDirection.Left
                    else
                        AnimatedContentTransitionScope.SlideDirection.Right
                    (slideIntoContainer(dir, tween(300)) + fadeIn() + scaleIn(initialScale = 0.90f))
                        .togetherWith(slideOutOfContainer(dir, tween(300)) + fadeOut() + scaleOut(targetScale = 0.90f))
                },
                label = "miniArtAndInfo",
                modifier = Modifier.weight(1f)
            ) { trackId ->
                val displayTrack = state.queue.trackById(trackId) ?: track
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MiuixAppTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        var artLoaded by remember(trackId) { mutableStateOf(false) }
                        AsyncImage(
                            model = displayTrack.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            onState = { imageState ->
                                artLoaded = imageState is AsyncImagePainter.State.Success
                            }
                        )
                        if (!artLoaded) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = MiuixAppTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            text = displayTrack.title,
                            style = MiuixAppTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixAppTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = displayTrack.artist,
                            style = MiuixAppTheme.typography.bodyMedium,
                            color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Previous
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .bounceClick(pressScale = 0.92f) { onPreviousClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = MiuixAppTheme.colorScheme.onBackground
                )
            }

            // Play/Pause
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .bounceClick(pressScale = 0.92f) { onPlayPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = state.isPlaying,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(150)))
                            .togetherWith(fadeOut(animationSpec = tween(100)))
                    },
                    label = "miniPlayPauseIcon"
                ) { isPlaying ->
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MiuixAppTheme.colorScheme.onBackground
                    )
                }
            }

            // Next
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .bounceClick(pressScale = 0.92f) { onNextClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = MiuixAppTheme.colorScheme.onBackground
                )
            }
        }

            BoldProgressBar(
                fraction = animatedFraction,
                activeColor = animatedGlow,
                inactiveColor = animatedGlow.copy(alpha = 0.18f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                height = 3.dp
            )
        }
    }
}

@Composable
fun BoldProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 4.dp,
    activeColor: Color = MiuixAppTheme.colorScheme.primary,
    inactiveColor: Color = MiuixAppTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(inactiveColor)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val w = size.width * fraction.coerceIn(0f, 1f)
            drawRect(
                color = activeColor,
                size = androidx.compose.ui.geometry.Size(w, size.height)
            )
        }
    }
}
