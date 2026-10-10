// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
//
// Material 3 mini player popup in PixelPlayer style
// (UnifiedPlayerSheetShared.MiniPlayerContentInternal): a rounded floating
// card with a 64dp row — 44dp circular album art, two-line labels and
// 36dp circular controls (previous/next in onPrimary with primary icons,
// play/pause solid primary) — plus a thin rounded accent progress line.
package dev.shephard.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlaybackProgress
import dev.shephard.player.player.PlayerUiState
import kotlinx.coroutines.flow.StateFlow
import dev.shephard.player.ui.i18n.LocalStrings

/** PixelPlayer MiniPlayerHeight. */
private val MiniPlayerM3Height = 64.dp
/** PixelPlayer corner radius for the player/nav bar surfaces. */
private val MiniPlayerM3CornerRadius = 20.dp

@Composable
fun M3MiniPlayer(
    state: PlayerUiState,
    progressFlow: StateFlow<PlaybackProgress>,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track: AudioTrack = state.currentTrack ?: return
    val progress by progressFlow.collectAsState()
    val coverAccent by animateColorAsState(
        targetValue = Color(state.glowColorArgb),
        animationSpec = tween(520),
        label = "miniPlayerCoverAccent",
    )
    val miniSurface by animateColorAsState(
        targetValue = lerp(MaterialTheme.colorScheme.surfaceContainerHigh, coverAccent, 0.32f),
        animationSpec = tween(520),
        label = "miniPlayerCoverSurface",
    )
    val miniForeground = miniPlayerContrastColor(miniSurface)
    val accentForeground = miniPlayerContrastColor(coverAccent)
    val progressTrack = lerp(MaterialTheme.colorScheme.surfaceContainerHighest, coverAccent, 0.24f)
    // Smoothed between progress ticks, same as the other players.
    val fraction = rememberSmoothProgressFraction(
        positionMs = progress.positionMs,
        durationMs = progress.durationMs,
        isPlaying = state.isPlaying,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(MiniPlayerM3CornerRadius),
            color = miniSurface,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                MiniPlayerM3Row(
                    track = track,
                    isPlaying = state.isPlaying,
                    accentColor = coverAccent,
                    accentForeground = accentForeground,
                    surfaceForeground = miniForeground,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                )
                M3MiniWavyProgress(
                    fraction = fraction,
                    isPlaying = state.isPlaying,
                    activeColor = coverAccent,
                    trackColor = progressTrack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
                )
            }
        }
    }
}

/**
 * Wavy progress line from the M3 Now Playing sheet (LinearWavyProgressIndicator). Display only:
 * it does not take touches, so taps on it still open the player. The wave flows while playing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3MiniWavyProgress(
    fraction: Float,
    isPlaying: Boolean,
    activeColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val waveAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "miniWavyAmplitude",
    )
    val strokePx = with(LocalDensity.current) { 3.dp.toPx() }
    val stroke = remember(strokePx) { Stroke(width = strokePx, cap = StrokeCap.Round) }
    LinearWavyProgressIndicator(
        progress = { fraction },
        modifier = modifier,
        color = activeColor,
        trackColor = trackColor,
        stroke = stroke,
        trackStroke = stroke,
        gapSize = (12f * (1f + 0.1573f * waveAmplitude * waveAmplitude)).dp,
        stopSize = 3.dp,
        // Gentler wave: about 60% of the previous height.
        amplitude = { p -> if (p > 0f) waveAmplitude * 0.6f else 0f },
        wavelength = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
        // Slightly slower than the default wave speed (was 1.0x wavelength).
        waveSpeed = WavyProgressIndicatorDefaults.LinearDeterminateWavelength * 0.6f,
    )
}

@Composable
private fun MiniPlayerM3Row(
    track: AudioTrack,
    isPlaying: Boolean,
    accentColor: Color,
    accentForeground: Color,
    surfaceForeground: Color,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MiniPlayerM3Height)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 44dp circular album art (keyed by track id like PixelPlayer)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(lerp(MaterialTheme.colorScheme.surfaceContainerHighest, accentColor, 0.30f)),
            contentAlignment = Alignment.Center
        ) {
            if (track.albumArtUri != null) {
                androidx.compose.runtime.key(track.id) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = accentForeground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                ),
                color = surfaceForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    letterSpacing = 0.sp,
                ),
                color = surfaceForeground.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        MiniPlayerM3CircleButton(
            size = 36.dp,
            containerColor = accentForeground.copy(alpha = 0.92f),
            contentColor = accentColor,
            onClick = onPreviousClick,
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = LocalStrings.current.previous,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        MiniPlayerM3CircleButton(
            size = 36.dp,
            containerColor = accentColor,
            contentColor = accentForeground,
            onClick = onPlayPauseClick,
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150))
                        .togetherWith(fadeOut(animationSpec = tween(100)))
                },
                label = "miniM3PlayPauseIcon",
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) LocalStrings.current.pause else LocalStrings.current.play,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        MiniPlayerM3CircleButton(
            size = 36.dp,
            containerColor = accentForeground.copy(alpha = 0.92f),
            contentColor = accentColor,
            onClick = onNextClick,
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = LocalStrings.current.next,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun MiniPlayerM3CircleButton(
    size: Dp,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

private fun miniPlayerContrastColor(color: Color): Color =
    if (color.luminance() >= 0.56f) Color(0xFF17131A) else Color.White
