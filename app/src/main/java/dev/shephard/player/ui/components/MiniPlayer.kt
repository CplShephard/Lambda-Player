// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.slideForwardInQueue
import dev.shephard.player.data.trackById
import dev.shephard.player.player.PlaybackProgress
import dev.shephard.player.player.PlayerUiState
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.miuixBlurSurface
import dev.shephard.player.ui.theme.PlayerTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Miuix mini player.
 *
 * Visual style is taken from the Flamingo Player now-playing pop-up:
 * a compact blurred card with a square album thumbnail on the left, the
 * title + artist in the middle, and a row of three controls (previous,
 * play/pause, next) on the right. A thin progress bar runs along the
 * bottom edge of the card.
 *
 * Designed to mirror the rest of the Miuix surface (status bar padding,
 * 14.dp card corners, Miuix theming) so it sits naturally above the
 * navigation bar without depending on any Flamingo-specific drawables.
 */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    progressFlow: StateFlow<PlaybackProgress>,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack ?: return

    val liquidGlassOn = LocalBlurEnabled.current
    val contentBackdrop = LocalContentBackdrop.current
    val miniPlayerShape = RoundedCornerShape(14.dp)

    val activeColor = PlayerTheme.colorScheme.primary
    val inactiveColor = activeColor.copy(alpha = 0.22f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                            fallbackColor = PlayerTheme.colorScheme.surfaceVariant,
                        )
                    } else {
                        Modifier.background(PlayerTheme.colorScheme.surfaceVariant)
                    }
                )
                .bounceClick(pressScale = 0.97f) { onClick() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = track.id,
                    transitionSpec = {
                        val dir = if (slideForwardInQueue(state.queue, initialState, targetState))
                            AnimatedContentTransitionScope.SlideDirection.Left
                        else
                            AnimatedContentTransitionScope.SlideDirection.Right
                        (slideIntoContainer(dir, tween(300)) + fadeIn() + scaleIn(initialScale = 0.92f))
                            .togetherWith(slideOutOfContainer(dir, tween(300)) + fadeOut() + scaleOut(targetScale = 0.92f))
                    },
                    label = "miniArtAndInfo",
                    modifier = Modifier.weight(1f),
                ) { trackId ->
                    val displayTrack = state.queue.trackById(trackId) ?: track
                    MiniPlayerArtAndTitle(displayTrack)
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(pressScale = 0.92f) { onPreviousClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = dev.shephard.player.R.drawable.ic_nowplaying_rewind),
                        contentDescription = "Previous",
                        tint = PlayerTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .bounceClick(pressScale = 0.92f) { onPlayPauseClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = state.isPlaying,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(150)))
                                .togetherWith(fadeOut(animationSpec = tween(100)))
                        },
                        label = "miniPlayPauseIcon",
                    ) { isPlaying ->
                        Icon(
                            painter = painterResource(
                                id = if (isPlaying) dev.shephard.player.R.drawable.ic_nowplaying_pause
                                else dev.shephard.player.R.drawable.ic_nowplaying_play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = PlayerTheme.colorScheme.onBackground,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick(pressScale = 0.92f) { onNextClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = dev.shephard.player.R.drawable.ic_nowplaying_fforward),
                        contentDescription = "Next",
                        tint = PlayerTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Thin progress bar at the bottom edge (overlayed on top of the
            // card). Matches the Flamingo "playback indicator" look.
            MiniPlayerProgressBar(
                progressFlow = progressFlow,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MiniPlayerArtAndTitle(track: AudioTrack) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PlayerTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            var artLoaded by remember(track.id) { mutableStateOf(false) }
            AsyncImage(
                model = track.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
                onState = { imageState ->
                    artLoaded = imageState is AsyncImagePainter.State.Success
                },
            )
            if (!artLoaded) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = PlayerTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            androidx.compose.material3.Text(
                text = track.title,
                fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp),
                fontWeight = FontWeight.SemiBold,
                color = PlayerTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            androidx.compose.material3.Text(
                text = track.artist,
                fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = PlayerTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    progressFlow: StateFlow<PlaybackProgress>,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    val progress by progressFlow.collectAsState()
    val fraction = if (progress.durationMs > 0L)
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 250),
        label = "miniProgress",
    )
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(inactiveColor),
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val w = size.width * animatedFraction.coerceIn(0f, 1f)
            drawRect(
                color = activeColor,
                size = androidx.compose.ui.geometry.Size(w, size.height),
            )
        }
    }
}
