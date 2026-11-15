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
                // Detect any word payload: Liriqo's Apple Music / QQ / NetEase may have words even when syncLevel is line
                val hasWordsInPayload = hasWordsPayload(lines, source.optJSONObject("words"))
                val hasNativeWords = (syncLevel == "word" || syncLevel == "syllable" || hasWordsInPayload) &&
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

    private fun hasWordsPayload(lines: JSONArray?, topWords: JSONObject?): Boolean {
        if (lines == null) return false
        for (i in 0 until minOf(lines.length(), 20)) {
            val line = lines.optJSONObject(i) ?: continue
            if (line.optJSONArray("words")?.length() ?: 0 > 0) return true
            val id = line.optString("id")
            if (id.isNotBlank() && topWords?.optJSONArray(id)?.length() ?: 0 > 0) return true
        }
        return (topWords?.length() ?: 0) > 0
    }

    private fun time(ms: Long): String = String.format(Locale.US, "%02d:%02d.%03d", ms / 60000, ms / 1000 % 60, ms % 1000)

    private fun toEnhancedLrc(lines: JSONArray, topWords: JSONObject?, hasNativeWords: Boolean): String? {
        val output = StringBuilder()
        for (i in 0 until minOf(lines.length(), 1500)) {
            val line = lines.optJSONObject(i) ?: continue
            val text = line.optString("text").replace('\n', ' ').replace('\r', ' ').trim()
            val start = line.optLong("start", -1)
            if (text.isBlank() || start < 0 || start > 5_999_999) continue
            // Collect words from per-line array, topWords map, and source-level fallback
            val words = if (hasNativeWords) {
                line.optJSONArray("words")
                    ?: topWords?.optJSONArray(line.optString("id"))
                    ?: topWords?.optJSONArray(line.optString("lineId"))
            } else null

            val segments = if (words != null && words.length() > 0) {
                // Try strict matching first, then lenient/fallback to preserve word-by-word like QQ QRC
                buildSegmentsStrict(text, words) ?: buildSegmentsFallback(text, words)
            } else null

            output.append('[').append(time(start)).append(']')
            if (segments.isNullOrEmpty()) {
                output.append(text.replace("<", "‹").replace(">", "›"))
            } else {
                // Reconstruct with word timings exactly like QQ original: <start>word<end>
                output.append(text.substring(0, segments.first().first))
                segments.forEachIndexed { j, part ->
                    val (from, to, timing) = part
                    output.append('<').append(time(timing.first)).append('>')
                    output.append(text.substring(from, to).replace("<", "‹").replace(">", "›"))
                    val next = segments.getOrNull(j + 1)
                    // End tag clamped to next word start to avoid overlap
                    output.append('<').append(time(timing.second.coerceAtMost(next?.third?.first ?: timing.second))).append('>')
                    output.append(text.substring(to, next?.first ?: text.length).replace("<", "‹").replace(">", "›"))
                }
            }
            output.append('\n')
        }
        return output.toString().takeIf { it.isNotBlank() }
    }

    // Strict: exact indexOf matching as before – fast and precise when upstream is perfect
    private fun buildSegmentsStrict(text: String, words: JSONArray): List<Triple<Int, Int, Pair<Long, Long>>>? {
        val segments = mutableListOf<Triple<Int, Int, Pair<Long, Long>>>()
        var cursor = 0
        var lastStart = -1L
        for (j in 0 until minOf(words.length(), 500)) {
            val word = words.optJSONObject(j) ?: continue
            val fragment = word.optString("text")
            val ws = word.optLong("start", -1)
            val we = word.optLong("end", -1)
            if (fragment.isBlank() || ws < 0 || we < ws || ws < lastStart || we > 5_999_999) return null
            // Try exact then case-insensitive, trimmed
            var match = text.indexOf(fragment.trim(), cursor)
            if (match < 0) match = text.lowercase().indexOf(fragment.trim().lowercase(), cursor)
            if (match < 0) {
                // Lenient: strip punctuation from both sides for this fragment
                val cleanFragment = fragment.trim().replace(Regex("[\\p{Punct}]"), "")
                if (cleanFragment.isNotBlank()) {
                    match = text.indexOf(cleanFragment, cursor)
                    if (match < 0) match = text.lowercase().indexOf(cleanFragment.lowercase(), cursor)
                }
            }
            if (match < 0) return null
            val end = match + fragment.trim().length
            if (end > text.length) return null
            segments.add(Triple(match, end, ws to we))
            cursor = end
            lastStart = ws
        }
        return segments.takeIf { it.isNotEmpty() }
    }

    // Fallback: assume words are sequential in text order; distribute by length when exact match fails
    // This preserves word-by-word for Apple Music/QQ/NetEase where text vs fragments differ slightly
    private fun buildSegmentsFallback(text: String, words: JSONArray): List<Triple<Int, Int, Pair<Long, Long>>>? {
        val segments = mutableListOf<Triple<Int, Int, Pair<Long, Long>>>()
        var cursor = 0
        var lastStart = -1L
        for (j in 0 until minOf(words.length(), 500)) {
            val word = words.optJSONObject(j) ?: continue
            val fragment = word.optString("text")
            val ws = word.optLong("start", -1)
            val we = word.optLong("end", -1)
            if (fragment.isBlank() || ws < 0 || we < ws || ws < lastStart || we > 5_999_999) return null
            val trimmed = fragment.trim()
            // If we can still find it, use exact position; otherwise approximate sequentially
            var from = text.indexOf(trimmed, cursor)
            if (from < 0) from = cursor.coerceAtMost(text.length)
            val to = (from + trimmed.length).coerceAtMost(text.length)
            // Skip if we overrun
            if (from >= text.length) continue
            segments.add(Triple(from, to, ws to we))
            cursor = to
            // Skip one space/punctuation between words if present
            if (cursor < text.length && text[cursor] == ' ') cursor++
            lastStart = ws
        }
        return segments.takeIf { it.isNotEmpty() }
    }
}
