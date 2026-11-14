// SPDX-License-Identifier: GPL-3.0-only
// Scoring and quotas adapted from PixelPlayerOSS DailyMixManager.
package dev.shephard.player.data

import java.util.Calendar
import java.util.Random
import kotlin.random.asKotlinRandom
import java.util.concurrent.TimeUnit

/**
 * Pixel's ranking and quotas adapted to Lambda's MediaStore and listening events.
 * Pixel's Room engagement records and Lambda's event history are different sources,
 * so matching input data is required for identical results.
 */
fun buildYourMix(
    tracks: List<AudioTrack>,
    likedIds: Set<Long>,
    history: List<ListenEvent>,
    now: Long = System.currentTimeMillis(),
    limit: Int = 60,
): List<AudioTrack> {
    if (tracks.isEmpty() || limit <= 0) return emptyList()
    val calendar = Calendar.getInstance().apply { timeInMillis = now }
    val seed = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR) + 17
    val random = Random(seed.toLong())
    val byId = tracks.associateBy { it.id }
    val events = history.filter { it.trackId in byId }.groupBy { it.trackId }
    val plays = events.mapValues { (_, v) -> v.count { it.countsAsPlay } }
    val durations = events.mapValues { (_, v) -> v.sumOf { it.listenedMs.coerceAtLeast(0L) } }
    val lastPlayed = events.mapValues { (_, v) -> v.maxOf { it.timestampMs } }
    fun artistKey(track: AudioTrack) = track.artistId?.let { "id:$it" } ?: track.artist.trim().lowercase()
    fun genreKey(track: AudioTrack) = track.genre?.trim()?.lowercase()?.takeIf { it.isNotBlank() && !it.contains("unknown") }
    val artistAffinity = mutableMapOf<String, Double>()
    val genreAffinity = mutableMapOf<String, Double>()
    events.forEach { (id, _) ->
        val track = byId.getValue(id)
        val artist = artistKey(track)
        val weight = (plays[id] ?: 0).toDouble() + (durations[id] ?: 0L) / 60000.0
        if (weight > 0) {
            artistAffinity[artist] = (artistAffinity[artist] ?: 0.0) + weight
            genreKey(track)?.let { genre -> genreAffinity[genre] = (genreAffinity[genre] ?: 0.0) + weight }
        }
    }
    val favouriteArtists = tracks.filter { it.id in likedIds }.groupingBy(::artistKey).eachCount()
    val maxPlays = (plays.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val maxDuration = (durations.values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val maxArtist = (artistAffinity.values.maxOrNull() ?: 0.0).takeIf { it > 0 } ?: 1.0
    val maxGenre = (genreAffinity.values.maxOrNull() ?: 0.0).takeIf { it > 0 } ?: 1.0
    val maxFavouriteArtist = (favouriteArtists.values.maxOrNull() ?: 0).coerceAtLeast(1)

    data class Ranked(val track: AudioTrack, val score: Double, val discovery: Double)
    val ranked = tracks.map { track ->
        val artist = artistKey(track)
        val genre = genreKey(track)
        val affinity = (((plays[track.id] ?: 0).toDouble() / maxPlays) * .7 +
            ((durations[track.id] ?: 0L).toDouble() / maxDuration) * .3).coerceIn(0.0, 1.0)
        val artistPreference = (artistAffinity[artist] ?: 0.0) / maxArtist
        val genrePreference = if (genre == null) 0.0 else (genreAffinity[genre] ?: 0.0) / maxGenre
        val favouriteArtistPreference = (favouriteArtists[artist] ?: 0).toDouble() / maxFavouriteArtist
        val preference = if (genre == null) artistPreference * .6 + favouriteArtistPreference * .4
            else artistPreference * .45 + genrePreference * .35 + favouriteArtistPreference * .20
        val last = lastPlayed[track.id]
        val days = if (last == null || last <= 0) null else (now - last).coerceAtLeast(0) / TimeUnit.DAYS.toMillis(1)
        val recency = when {
            days == null -> .6
            days < 1 -> .2
            days < 3 -> .5
            days < 7 -> .7
            days < 14 -> .85
            else -> 1.0
        }
        val favourite = if (track.id in likedIds) 1.0 else 0.0
        val baseline = if (track.id !in events) .1 else 0.0
        val addedMs = if (track.dateAdded in 1 until 10_000_000_000L) TimeUnit.SECONDS.toMillis(track.dateAdded) else track.dateAdded
        val novelty = if (addedMs <= 0) 0.0 else
            (1.0 - ((now - addedMs).coerceAtLeast(0L) / TimeUnit.DAYS.toMillis(1)).toDouble() / 60.0).coerceIn(0.0, 1.0)
        val score = preference * .45 + affinity * .25 + recency * .15 + favourite * .1 + novelty * .05 + baseline + random.nextDouble() * .005
        Ranked(track, score, (1.0 - affinity) * .6 + novelty * .25 + preference * .15)
    }.sortedWith(compareByDescending<Ranked> { it.score }.thenBy { it.track.id })

    val artistCounts = mutableMapOf<String, Int>()
    var unknownGenres = 0
    val genreCounts = mutableMapOf<String, Int>()
    fun pick(candidates: List<Ranked>, quota: Int): List<AudioTrack> {
        if (quota <= 0) return emptyList()
        val selected = mutableListOf<AudioTrack>()
        for (candidate in candidates) {
            if (selected.size >= quota) break
            val track = candidate.track
            val artist = artistKey(track)
            val genre = genreKey(track)
            val favourite = track.id in likedIds
            val artistCap = if (favourite) 3 else 2
            val genreBase = if (genre == null) {
                if (quota <= 12) 1 else if (quota <= 30) 2 else 3
            } else {
                if (quota <= 12) 2 else if (quota <= 30) 3 else 4
            }
            val genreCap = genreBase + if (favourite) 1 else 0
            val genreCount = if (genre == null) unknownGenres else genreCounts[genre] ?: 0
            if ((artistCounts[artist] ?: 0) >= artistCap || genreCount >= genreCap) continue
            selected += track
            artistCounts[artist] = (artistCounts[artist] ?: 0) + 1
            if (genre == null) unknownGenres++ else genreCounts[genre] = genreCount + 1
        }
        if (selected.size < quota) {
            val selectedIds = selected.mapTo(mutableSetOf()) { it.id }
            for (candidate in candidates) {
                if (selected.size >= quota) break
                if (selectedIds.add(candidate.track.id)) selected += candidate.track
            }
        }
        return selected
    }

    val favouriteCount = (limit * .3).toInt().coerceAtLeast(5).coerceAtMost(limit)
    val coreCount = (limit * .45).toInt().coerceAtLeast(10).coerceAtMost(limit)
    val discoveryCount = (limit - favouriteCount - coreCount).coerceAtLeast(0)
    val result = LinkedHashMap<Long, AudioTrack>()
    fun append(items: List<AudioTrack>) { items.forEach { result.putIfAbsent(it.id, it) } }
    append(pick(ranked.filter { it.track.id in likedIds }, favouriteCount))
    append(pick(ranked.filter { it.track.id !in result }, coreCount))
    append(pick(ranked.filter { it.track.id !in result }.sortedWith(compareByDescending<Ranked> { it.discovery }.thenBy { it.track.id }), discoveryCount))
    if (result.size < limit) append(pick(ranked.filter { it.track.id !in result }, limit - result.size))
    if (result.size < limit) append(tracks.filter { it.id !in result }.shuffled(random.asKotlinRandom()))
    return result.values.take(limit)
}
