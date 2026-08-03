package dev.shephard.player.data

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArtUri: Uri?
) {
    /** Şarkıyı JSON'a serileştirir (kütüphane önbelleği için). */
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("artist", artist)
        put("album", album)
        put("durationMs", durationMs)
        put("uri", uri.toString())
        put("albumArtUri", albumArtUri?.toString() ?: "")
    }

    companion object {
        /** JSON'dan şarkıyı çözer; bozuk/eksik alanlarda null döner. */
        fun fromJson(obj: JSONObject): AudioTrack? = try {
            val uriStr = obj.optString("uri")
            if (uriStr.isEmpty()) return null
            val artStr = obj.optString("albumArtUri").takeIf { it.isNotEmpty() }
            AudioTrack(
                id = obj.getLong("id"),
                title = obj.getString("title"),
                artist = obj.getString("artist"),
                album = obj.getString("album"),
                durationMs = obj.optLong("durationMs"),
                uri = Uri.parse(uriStr),
                albumArtUri = artStr?.let { Uri.parse(it) }
            )
        } catch (_: Exception) { null }
    }
}

/** Şarkı listesini JSON dizisine serileştirir (kütüphane önbelleği). */
fun List<AudioTrack>.toCacheJson(): String {
    val arr = JSONArray()
    forEach { arr.put(it.toJson()) }
    return arr.toString()
}

/** JSON dizisinden şarkı listesini çözer. Geçersiz girdide boş liste döner. */
fun parseTrackCacheJson(json: String): List<AudioTrack> = try {
    val arr = JSONArray(json)
    (0 until arr.length()).mapNotNull { AudioTrack.fromJson(arr.getJSONObject(it)) }
} catch (_: Exception) { emptyList() }

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

