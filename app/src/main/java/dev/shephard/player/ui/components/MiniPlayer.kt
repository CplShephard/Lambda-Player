package dev.shephard.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerUiState
import kotlinx.coroutines.launch

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

    // Yatay kaydırma: sağa → önceki, sola → sonraki (çoğu müzik uygulaması gibi).
    // Bar parmakla birlikte kayar, bırakınca yumuşakça yerine döner.
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 64.dp.toPx() }
    val swipeX = remember { Animatable(0f) }
    var swipeHandled by remember { mutableStateOf(false) }
    val swipeScope = rememberCoroutineScope()

    // Şarkı geçiş yönü takibi
    var lastTrackId by remember { mutableStateOf<Long?>(null) }
    var slideForward by remember { mutableStateOf(true) }
    val currentId = track.id
    if (lastTrackId != null && lastTrackId != currentId) {
        slideForward = currentId > (lastTrackId ?: 0L)
    }
    if (lastTrackId != currentId) lastTrackId = currentId

    // Smoothly animate the progress fraction so the bold bar feels alive,
    // even between position-tick updates.
    val fraction = if (state.durationMs > 0L)
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 250),
        label = "miniProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeX.value }
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                // Pop-up bar is locked: swipe gestures disabled for stability
                .bounceClick { onClick() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Albüm kapağı — geçiş animasyonuyla
            AnimatedContent(
                targetState = track.id,
                transitionSpec = {
                    val dir = if (slideForward)
                        AnimatedContentTransitionScope.SlideDirection.Left
                    else
                        AnimatedContentTransitionScope.SlideDirection.Right
                    (slideIntoContainer(dir, tween(300)) + fadeIn() + scaleIn(initialScale = 0.90f))
                        .togetherWith(slideOutOfContainer(dir, tween(300)) + fadeOut() + scaleOut(targetScale = 0.90f))
                },
                label = "miniArt"
            ) { trackId ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    var artLoaded by remember(trackId) { mutableStateOf(false) }
                    AsyncImage(
                        model = track.albumArtUri,
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Şarkı başlığı + sanatçı — geçiş animasyonuyla
            AnimatedContent(
                targetState = track.id,
                transitionSpec = {
                    val dir = if (slideForward)
                        AnimatedContentTransitionScope.SlideDirection.Left
                    else
                        AnimatedContentTransitionScope.SlideDirection.Right
                    (slideIntoContainer(dir, tween(300)) + fadeIn() + scaleIn(initialScale = 0.90f))
                        .togetherWith(slideOutOfContainer(dir, tween(300)) + fadeOut() + scaleOut(targetScale = 0.90f))
                },
                label = "miniPlayerTrackInfo",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) { _ ->
                Column {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Play/Pause butonu — bounceClick ile
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .bounceClick { onPlayPauseClick() },
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
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Next butonu — bounceClick ile
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .bounceClick { onNextClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Spotify-style bold progress bar at the bottom of the mini player.
        // 4.dp tall (thicker than the inline MinimalSeekBar inside the sheet),
        // the inactive half uses surfaceVariant and the active half uses
        // primary so it pops against the dark background.
        BoldProgressBar(
            fraction = animatedFraction,
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        )
    }
}

/**
 * Thick, rounded progress bar meant to live beneath the mini player. Inspired
 * by Spotify's bold accent line. Drawn on a Canvas so it stays crisp at any
 * width and avoids the Material slider overhead.
 */
@Composable
fun BoldProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 4.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
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
