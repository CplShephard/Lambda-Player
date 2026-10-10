package dev.shephard.player.player

import dev.shephard.player.data.AudioTrack
import kotlin.math.abs

/**
 * How lyrics are chosen automatically when a track starts (or when the lyrics button is first pressed).
 * A lyrics result the user picked manually always wins over every mode.
 */
enum class LyricsAutoSearchMode(val value: String) {
    /** Automatic online search is disabled: saved -> embedded -> .lrc file, as before. */
    OFF("off"),

    /** Embedded lyrics in the file are preferred. Online search runs only when none are embedded. */
    EMBEDDED_FIRST("embedded_first"),

    /** Online candidates are scored and the best match is applied ("En uygun"). */
    BEST_MATCH("best_match"),

    /** The first time lyrics are opened for a track, go straight to the online results screen. */
    ONLINE_FIRST("online_first");

    companion object {
        fun fromValueOrDefault(value: String?): LyricsAutoSearchMode =
            entries.firstOrNull { it.value == value } ?: OFF
    }
}

/** Minimum score a candidate needs before it is applied automatically. */
internal const val MIN_AUTO_LYRICS_SCORE = 5.0

/** Lowercase, keep letters and digits only, so "Daft Punk" and "daft-punk" compare equal. */
internal fun normalizeForLyricsMatch(text: String): String =
    text.lowercase().filter { it.isLetterOrDigit() }

/**
 * Scores an online lyrics candidate against the local track.
 *  - Duration closeness is the strongest signal (within 2 s: +4, 5 s: +2, 10 s: +0.5, otherwise -4).
 *  - Synced (timestamped) lyrics: +2.
 *  - Artist name match: +3.
 *  - Title match: +2.
 * Returns null when the candidate has no usable lyrics or does not reach [MIN_AUTO_LYRICS_SCORE].
 */
internal fun scoreLyricsCandidate(candidate: LyricsSearchResult, track: AudioTrack): Double? {
    if (candidate.instrumental) return null
    if (candidate.rawLyrics.isBlank()) return null

    var score = 0.0

    if (track.durationMs > 0L) {
        val candidateDuration = candidate.durationSeconds
        if (candidateDuration != null) {
            val diff = abs(candidateDuration - track.durationMs / 1000.0)
            score += when {
                diff <= 2.0 -> 4.0
                diff <= 5.0 -> 2.0
                diff <= 10.0 -> 0.5
                else -> -4.0
            }
        }
    }

    if (!candidate.syncedLyrics.isNullOrBlank()) score += 2.0

    val trackArtist = normalizeForLyricsMatch(track.artist)
    val candidateArtist = normalizeForLyricsMatch(candidate.artist)
    if (trackArtist.isNotEmpty() && candidateArtist.isNotEmpty() &&
        (trackArtist == candidateArtist ||
            trackArtist.contains(candidateArtist) ||
            candidateArtist.contains(trackArtist))
    ) {
        score += 3.0
    }

    val trackTitle = normalizeForLyricsMatch(track.title)
    if (trackTitle.isNotEmpty() && trackTitle == normalizeForLyricsMatch(candidate.title)) {
        score += 2.0
    }

    return score.takeIf { it >= MIN_AUTO_LYRICS_SCORE }
}

/** Returns the highest-scoring acceptable candidate, or null when none is good enough. */
internal fun pickBestLyricsCandidate(candidates: List<LyricsSearchResult>, track: AudioTrack): LyricsSearchResult? =
    candidates
        .mapNotNull { candidate -> scoreLyricsCandidate(candidate, track)?.let { candidate to it } }
        .maxByOrNull { it.second }
        ?.first
