package dev.shephard.player.player

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Community hosted, unofficial multi-source endpoint; millisecond times are absolute. */
internal object LiriqoLyrics {
    fun search(query: String): List<LyricsSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val connection = (URL("https://api.liriqo-alfarrizi.my.id/v1/lyrics?Q=$encoded").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LambdaPlayer/2.5")
        }
        try {
            if (connection.responseCode !in 200..299) throw IOException("LiriQo HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { reader ->
                val chars = CharArray(8192)
                val result = StringBuilder()
                while (true) {
                    val n = reader.read(chars)
                    if (n < 0) break
                    if (result.length + n > 2_000_000) throw IOException("LiriQo response too large")
                    result.append(chars, 0, n)
                }
                result.toString()
            }
            return parse(JSONObject(body))
        } finally { connection.disconnect() }
    }

    internal fun parse(root: JSONObject): List<LyricsSearchResult> {
        val metadata = root.optJSONObject("metadata") ?: return emptyList()
        val title = metadata.optString("title").trim()
        val artist = metadata.optString("artist").trim()
        if (title.isBlank() || artist.isBlank()) return emptyList()
        val duration = metadata.optDouble("duration", Double.NaN).takeIf { it.isFinite() && it > 0 }
        val tracks = root.optJSONArray("tracks") ?: JSONArray().also { array -> root.optJSONObject("primary")?.let { primary -> array.put(primary) } }
        return buildList {
            for (i in 0 until minOf(tracks.length(), 16)) {
                val source = tracks.optJSONObject(i) ?: continue
                val provider = source.optString("provider").takeIf { it.isNotBlank() } ?: continue
                val lines = source.optJSONArray("timed")
                val syncLevel = source.optString("syncLevel").lowercase(Locale.ROOT)
                val hasNativeWords = (syncLevel == "word" || syncLevel == "syllable") &&
                    provider != "lrclib" && source.optBoolean("nativeWordTiming", true)
                val synced = if (lines != null) toEnhancedLrc(lines, source.optJSONObject("words"), hasNativeWords) else null
                val plain = source.optString("plain").takeIf { it.isNotBlank() && it != "null" }
                if (synced.isNullOrBlank() && plain.isNullOrBlank()) continue
                val attribution = source.optString("upstreamProvider").takeIf { it.isNotBlank() && it != "null" }
                    ?: source.optString("lyricsSource").takeIf { it.isNotBlank() && it != "null" }
                    ?: provider
                add(LyricsSearchResult(id = null, title = title, artist = artist,
                    album = metadata.optString("album"), durationSeconds = duration, instrumental = false,
                    plainLyrics = plain, syncedLyrics = synced, source = "LiriQo · $attribution",
                    sourceId = "${root.optString("videoId")}|$provider"))
            }
        }
    }

    private fun time(ms: Long): String = String.format(Locale.US, "%02d:%02d.%03d", ms / 60000, ms / 1000 % 60, ms % 1000)
    private fun toEnhancedLrc(lines: JSONArray, topWords: JSONObject?, hasNativeWords: Boolean): String? {
        val output = StringBuilder()
        for (i in 0 until minOf(lines.length(), 1500)) {
            val line = lines.optJSONObject(i) ?: continue
            val text = line.optString("text").replace('\n', ' ').replace('\r', ' ').trim()
            val start = line.optLong("start", -1)
            if (text.isBlank() || start < 0 || start > 5_999_999) continue
            val words = if (hasNativeWords) line.optJSONArray("words") ?: topWords?.optJSONArray(line.optString("id")) else null
            val segments = mutableListOf<Triple<Int, Int, Pair<Long, Long>>>()
            var cursor = 0
            var lastStart = -1L
            if (words != null) for (j in 0 until minOf(words.length(), 500)) {
                val word = words.optJSONObject(j) ?: continue
                val fragment = word.optString("text")
                val ws = word.optLong("start", -1)
                val we = word.optLong("end", -1)
                if (fragment.isBlank() || ws < 0 || we < ws || ws < lastStart || we > 5_999_999) { segments.clear(); break }
                val match = text.indexOf(fragment.trim(), cursor)
                if (match < 0) { segments.clear(); break }
                val end = match + fragment.trim().length
                segments.add(Triple(match, end, ws to we)); cursor = end; lastStart = ws
            }
            output.append('[').append(time(start)).append(']')
            if (segments.isEmpty()) output.append(text.replace("<", "‹").replace(">", "›"))
            else {
                output.append(text.substring(0, segments.first().first))
                segments.forEachIndexed { j, part ->
                    val (from, to, timing) = part
                    output.append('<').append(time(timing.first)).append('>')
                    output.append(text.substring(from, to).replace("<", "‹").replace(">", "›"))
                    val next = segments.getOrNull(j + 1)
                    output.append('<').append(time(timing.second.coerceAtMost(next?.third?.first ?: timing.second))).append('>')
                    output.append(text.substring(to, next?.first ?: text.length).replace("<", "‹").replace(">", "›"))
                }
            }
            output.append('\n')
        }
        return output.toString().takeIf { it.isNotBlank() }
    }
}
