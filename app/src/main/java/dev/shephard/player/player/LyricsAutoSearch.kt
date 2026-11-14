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
 * Removes bracketed extras such as "(Live)", "[Remastered]", "（伴奏）" or "【官方版】" and anything after
 * " - " so "晴天 (Live)" and "晴天" are treated as the same title.
 */
internal fun stripTitleDecorations(title: String): String =
    title
        .replace(Regex("[(\\[（【「『][^)\\]）】」』]*[)\\]）】」』]"), " ")
        .substringBefore(" - ")
        .trim()
        .ifEmpty { title }

/**
 * Scores an online lyrics candidate against the local track.
 *  - Duration closeness is the strongest signal (within 2 s: +4, 5 s: +2, 10 s: +0.5, otherwise -4).
 *  - Synced (timestamped) lyrics: +2.
 *  - Artist name match: +3.
 *  - Title match: +2 (one title containing the other: +1), ignoring "(Live)"-style extras.
 * Returns null when the candidate has no usable lyrics or does not reach [MIN_AUTO_LYRICS_SCORE].
 */
internal fun scoreLyricsCandidate(candidate: LyricsSearchResult, track: AudioTrack): Double? {
    if (candidate.instrumental) return null
    if (candidate.rawLyrics.isBlank()) return null

    // A resolver search may return a plausible but unrelated result. Do not auto-apply
    // an aggregator match without BOTH a comparable title and artist.
    if (candidate.source.startsWith("LiriQo")) {
        fun matches(a: String, b: String): Boolean {
            val x = normalizeForLyricsMatch(a); val y = normalizeForLyricsMatch(b)
            return x.length >= 2 && y.length >= 2 && (x == y || x.contains(y) || y.contains(x))
        }
        if (!matches(stripTitleDecorations(track.title), stripTitleDecorations(candidate.title)) ||
            !matches(track.artist, candidate.artist)) return null
        if (candidate.durationSeconds != null && track.durationMs > 0 &&
            abs(candidate.durationSeconds - track.durationMs / 1000.0) > 15.0) return null
    }
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

    if (!candidate.syncedLyrics.isNullOrBlank()) {
        score += 2.0
        // Prefer actual word-tagged timing when otherwise similarly matched.
        if (Regex("<\\d{1,3}:\\d{2}[.:]\\d{1,3}>").containsMatchIn(candidate.syncedLyrics)) score += 1.5
    }

    val trackArtist = normalizeForLyricsMatch(track.artist)
    val candidateArtist = normalizeForLyricsMatch(candidate.artist)
    if (trackArtist.isNotEmpty() && candidateArtist.isNotEmpty() &&
        (trackArtist == candidateArtist ||
            trackArtist.contains(candidateArtist) ||
            candidateArtist.contains(trackArtist))
    ) {
        score += 3.0
    }

    val trackTitle = normalizeForLyricsMatch(stripTitleDecorations(track.title))
    val candidateTitle = normalizeForLyricsMatch(stripTitleDecorations(candidate.title))
    if (trackTitle.isNotEmpty() && candidateTitle.isNotEmpty()) {
        if (trackTitle == candidateTitle) {
            score += 2.0
        } else if (trackTitle.contains(candidateTitle) || candidateTitle.contains(trackTitle)) {
            score += 1.0
        }
    }

    return score.takeIf { it >= MIN_AUTO_LYRICS_SCORE }
}

/** Returns the highest-scoring acceptable candidate, or null when none is good enough. */
internal fun pickBestLyricsCandidate(candidates: List<LyricsSearchResult>, track: AudioTrack): LyricsSearchResult? =
    candidates
        .mapNotNull { candidate -> scoreLyricsCandidate(candidate, track)?.let { candidate to it } }
        .maxByOrNull { it.second }
        ?.first
