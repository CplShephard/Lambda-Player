package dev.shephard.player.ui.components

import dev.shephard.player.ui.components.m3.m3CardPress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.ListenEvent
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import java.util.Calendar
import dev.shephard.player.player.PreferencesManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A local daily mix based on favourites, listening history and the current library. */
@Composable
fun rememberHomeMix(tracks: List<AudioTrack>, likedIds: List<Long>, history: List<ListenEvent>): List<AudioTrack> {
    val calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
    // Ranking sorts the whole library and can run while the pager is animating.
    // Keep the last result visible, and compute the new one off the UI thread.
    val mix by produceState(tracks.take(60), tracks, likedIds, history, day) {
        value = withContext(Dispatchers.Default) {
            dev.shephard.player.data.buildYourMix(tracks, likedIds.toSet(), history)
        }
    }
    return mix
}

/** Shuffle on a worker so opening Home does not copy a large library on the UI thread. */
@Composable
fun rememberFeaturedTracks(tracks: List<AudioTrack>, limit: Int): List<AudioTrack> {
    val featured by produceState(tracks.take(limit), tracks, limit) {
        value = withContext(Dispatchers.Default) { tracks.shuffled().take(limit) }
    }
    return featured
}

/** History matching/sorting must not run on the frame thread when a pager page is composed. */
@Composable
fun rememberRecentTracks(tracks: List<AudioTrack>, history: List<ListenEvent>, limit: Int): List<AudioTrack> {
    val recent by produceState(tracks.take(limit), tracks, history, limit) {
        value = withContext(Dispatchers.Default) {
            val byId = HashMap<Long, AudioTrack>(tracks.size)
            val byTitleArtist = HashMap<Pair<String, String>, AudioTrack>(tracks.size)
            tracks.forEach { track ->
                byId.putIfAbsent(track.id, track)
                byTitleArtist.putIfAbsent(track.title to track.artist, track)
            }
            val matched = history.sortedByDescending { it.timestampMs }
                .mapNotNull { event -> byId[event.trackId] ?: byTitleArtist[event.title to event.artist] }
                .distinctBy { it.id }
            if (matched.isNotEmpty()) matched.take(limit) else tracks.take(limit)
        }
    }
    return recent
}

/** Pixel-inspired mix header and album-art preview, using Lambda's actual local queue. */
@Composable
fun HomeYourMix(
    tracks: List<AudioTrack>,
    useMiuix: Boolean,
    onRemix: (List<AudioTrack>) -> Unit,
    onPlay: (List<AudioTrack>, Int) -> Unit,
) {
    val strings = LocalStrings.current
    val day = Calendar.getInstance().let { it.get(Calendar.YEAR) * 366 + it.get(Calendar.DAY_OF_YEAR) }
    val pattern = CollagePattern.entries[day % CollagePattern.entries.size]
    val surface = if (useMiuix) MiuixAppTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer
    val ink = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val accent = if (useMiuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth()) {
        // Pixel's hero: a two-line title with a prominent shuffle button.
        Box(Modifier.fillMaxWidth().height(256.dp).padding(16.dp)) {
            Column(Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 12.dp)) {
                Text(strings.yourMixTitle, fontWeight = FontWeight.Black, fontSize = 64.sp, lineHeight = 62.sp, color = ink)
                Text(strings.yourMixSubtitle, color = ink.copy(alpha = .7f),
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 12.dp).size(72.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (tracks.isNotEmpty()) accent else surface)
                    .clickable(enabled = tracks.isNotEmpty()) {
                        onRemix(tracks)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = strings.shuffle,
                    tint = if (tracks.isNotEmpty()) Color.White else ink.copy(alpha = .45f),
                    modifier = Modifier.size(36.dp))
            }
        }
        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp)).background(surface), contentAlignment = Alignment.Center) {
                Text(strings.noSongsToPlay, color = ink.copy(alpha = .65f), modifier = Modifier.padding(16.dp))
            }
        } else {
            PixelMixCollage(tracks = tracks, surface = surface, accent = accent, pattern = pattern, useMiuix = useMiuix, onPlay = onPlay)
        }
    }

}

/** PixelPlayerOSS collage positions, using Lambda's artwork. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PixelMixCollage(
    tracks: List<AudioTrack>,
    surface: Color,
    accent: Color,
    pattern: CollagePattern,
    useMiuix: Boolean,
    onPlay: (List<AudioTrack>, Int) -> Unit,
) {
    @Composable
    fun Tile(index: Int, width: Dp, height: Dp, modifier: Modifier, shape: Shape, rotation: Float) {
        val track = tracks.getOrNull(index) ?: return
        Box(modifier.size(minOf(width, height)).graphicsLayer { rotationZ = rotation }.clip(shape)
            .background(surface).then(
                if (useMiuix) Modifier.clickable { onPlay(tracks, index) }
                else Modifier.m3CardPress(shape = shape, overlayColor = Color.White) { onPlay(tracks, index) }
            ), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MusicNote, null, tint = accent.copy(alpha = .5f), modifier = Modifier.size(32.dp))
            AsyncImage(model = dev.shephard.player.ui.components.rememberStaticArtwork(track.albumArtUri), contentDescription = track.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(400.dp).padding(14.dp)) {
        val h = maxHeight
        val base = minOf(300.dp, h)
        val configs = remember(pattern, base, h) { buildCollageConfigs(pattern, base, h) }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(h * .6f)) {
                configs.take(3).forEachIndexed { index, cfg ->
                    Tile(index, cfg.width, cfg.height, Modifier.align(cfg.align).offset(cfg.offsetX, cfg.offsetY), cfg.shape, cfg.rot)
                }
            }
            Box(Modifier.fillMaxWidth().height(h * .4f)) {
                configs.drop(3).forEachIndexed { index, cfg ->
                    Tile(index + 3, cfg.width, cfg.height, Modifier.align(cfg.align).offset(cfg.offsetX, cfg.offsetY), cfg.shape, cfg.rot)
                }
            }
        }
    }
}

/** Live listening total, linked to the existing statistics page. */
@Composable
fun HomeListeningTime(totalMs: Long, useMiuix: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val accent = if (useMiuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val surface = if (useMiuix) MiuixAppTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
    val ink = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onPrimaryContainer
    val totalSeconds = (totalMs / 1000).coerceAtLeast(0)
    val time = if (totalSeconds >= 3600) "%d%s %02d%s".format(totalSeconds / 3600, strings.hourShort, totalSeconds / 60 % 60, strings.minuteShort)
        else "%d%s %02d%s".format(totalSeconds / 60, strings.minuteShort, totalSeconds % 60, strings.secondShort)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(surface, accent.copy(alpha = .24f))))
            .then(if (useMiuix) Modifier.clickable(onClick = onClick)
                else Modifier.m3CardPress(shape = RoundedCornerShape(26.dp), onClick = onClick))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Headphones, null, tint = accent, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(strings.totalListeningTime, color = ink.copy(alpha = .72f), style = MaterialTheme.typography.labelLarge, maxLines = 2)
            Text(time, color = ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Filled.AutoAwesome, null, tint = accent, modifier = Modifier.size(23.dp))
    }
}
