package dev.shephard.player.data

import android.net.Uri

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArtUri: Uri?
)

fun AudioTrack.formattedDuration(): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun slideForwardInQueue(queue: List<AudioTrack>, fromId: Long, toId: Long): Boolean {
    if (fromId == toId) return true
    val from = queue.indexOfFirst { it.id == fromId }
    val to = queue.indexOfFirst { it.id == toId }
    return if (from >= 0 && to >= 0) to > from else true
}

fun List<AudioTrack>.trackById(id: Long): AudioTrack? = firstOrNull { it.id == id }
