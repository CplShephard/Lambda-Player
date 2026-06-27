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

/**
 * Şarkı geçiş yönünü kuyruktaki konuma göre belirler — sayısal id karşılaştırması
 * yerine kullanılır. Şarkı ileri (sonraki) gidiyorsa true (soldan girer),
 * geri (önceki) gidiyorsa false (sağdan girer) döner.
 *
 * Hem NowPlayingSheet hem de MiniPlayer ortak bu mantığı kullanır.
 */
fun slideForwardInQueue(queue: List<AudioTrack>, fromId: Long, toId: Long): Boolean {
    if (fromId == toId) return true
    val from = queue.indexOfFirst { it.id == fromId }
    val to = queue.indexOfFirst { it.id == toId }
    return if (from >= 0 && to >= 0) to > from else true
}

/** Kuyruktan id'ye göre parçayı bulur (geçiş sırasında doğru kapak/metin göstermek için). */
fun List<AudioTrack>.trackById(id: Long): AudioTrack? = firstOrNull { it.id == id }

