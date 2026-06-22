package dev.shephard.player.data

import android.content.Context
import android.net.Uri
import dev.shephard.player.player.LyricLine
import java.io.BufferedReader
import java.io.InputStreamReader

object LyricsParser {

    /**
     * Tries to load lyrics for the given track.
     * Looks for a .lrc file alongside the audio file (same name, .lrc extension).
     * Returns an empty list if no lyrics file is found or parsing fails.
     */
    fun loadForTrack(context: Context, track: AudioTrack): List<LyricLine> {
        return try {
            val lrcUri = getLrcUri(track) ?: return emptyList()
            val stream = context.contentResolver.openInputStream(lrcUri) ?: return emptyList()
            parseLrc(BufferedReader(InputStreamReader(stream, Charsets.UTF_8)))
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Derives a .lrc URI from the track's URI by replacing the file extension.
     */
    private fun getLrcUri(track: AudioTrack): Uri? {
        val uriString = track.uri.toString()
        val dotIndex = uriString.lastIndexOf('.')
        if (dotIndex < 0) return null
        val lrcString = uriString.substring(0, dotIndex) + ".lrc"
        return try { Uri.parse(lrcString) } catch (_: Exception) { null }
    }

    /**
     * Parses an LRC file into a sorted list of LyricLine entries.
     *
     * Supports standard timestamp format: [mm:ss.xx] or [mm:ss.xxx]
     * Lines without valid timestamps are ignored.
     */
    private fun parseLrc(reader: BufferedReader): List<LyricLine> {
        val timeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")
        val lines = mutableListOf<LyricLine>()

        reader.use { br ->
            br.lineSequence().forEach { rawLine ->
                val matches = timeRegex.findAll(rawLine)
                val text = rawLine.replace(timeRegex, "").trim()
                if (text.isEmpty()) return@forEach

                matches.forEach { match ->
                    val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                    val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                    val centisStr = match.groupValues[3]
                    val millis = when (centisStr.length) {
                        2 -> (centisStr.toLongOrNull() ?: 0L) * 10L
                        else -> centisStr.toLongOrNull() ?: 0L
                    }
                    val timeMs = minutes * 60_000L + seconds * 1_000L + millis
                    lines.add(LyricLine(timeMs, text))
                }
            }
        }

        return lines.sortedBy { it.timeMs }
    }
}
