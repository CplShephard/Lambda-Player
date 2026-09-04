package dev.shephard.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.data.slideForwardInQueue
import dev.shephard.player.data.trackById
import dev.shephard.player.player.PlaybackProgress
import dev.shephard.player.player.PlayerUiState
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.miuixBlurSurface
import kotlinx.coroutines.flow.StateFlow
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults as MiuixProgressIndicatorDefaults

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    progressFlow: StateFlow<PlaybackProgress>,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return

    val liquidGlassOn = LocalBlurEnabled.current
    val contentBackdrop = LocalContentBackdrop.current
    val miniPlayerShape = RoundedCornerShape(14.dp)

    val themeAccent = MiuixAppTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(miniPlayerShape)
                .then(
                    if (liquidGlassOn && contentBackdrop != null) {
                        Modifier.miuixBlurSurface(
                            backdrop = contentBackdrop,
                            shape = miniPlayerShape,
                            blurRadius = 28f,
                            tintAlpha = 0.58f,
                            fallbackColor = MiuixAppTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant)
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

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .bounceClick(pressScale = 0.92f) { onPreviousClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = dev.shephard.player.R.drawable.ic_nowplaying_rewind),
                        contentDescription = "Previous",
                        tint = MiuixAppTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

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
                            painter = painterResource(
                                id = if (isPlaying) dev.shephard.player.R.drawable.ic_nowplaying_pause
                                else dev.shephard.player.R.drawable.ic_nowplaying_play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MiuixAppTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .bounceClick(pressScale = 0.92f) { onNextClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = dev.shephard.player.R.drawable.ic_nowplaying_fforward),
                        contentDescription = "Next",
                        tint = MiuixAppTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            MiniPlayerProgressBar(
                progressFlow = progressFlow,
                activeColor = themeAccent,
                inactiveColor = themeAccent.copy(alpha = 0.18f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    progressFlow: StateFlow<PlaybackProgress>,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val progress by progressFlow.collectAsState()
    val fraction = if (progress.durationMs > 0L)
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 250),
        label = "miniProgress"
    )
    MiuixLinearProgressIndicator(
        progress = animatedFraction,
        modifier = modifier,
        height = 3.dp,
        colors = MiuixProgressIndicatorDefaults.progressIndicatorColors(
            foregroundColor = activeColor,
            backgroundColor = inactiveColor,
        )
    )
}
