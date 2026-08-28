package dev.shephard.player.data

import java.util.Calendar

data class ListenEvent(
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val dayStartMs: Long,
    val timestampMs: Long,
    val listenedMs: Long,
    val countsAsPlay: Boolean,
    val albumArtUri: String? = null
)

enum class StatsPeriod {
    Today, ThisWeek, ThisMonth, ThisYear, AllTime
}

data class StatsSummary(
    val totalListenedMs: Long,
    val playCount: Int,
    val uniqueAlbumCount: Int
)

data class StatsArtistEntry(
    val artistName: String,
    val listenedMs: Long
)

data class StatsAlbumEntry(
    val albumName: String,
    val artistName: String,
    val listenedMs: Long
)

data class StatsTrackEntry(
    val trackId: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val listenedMs: Long,
    val albumArtUri: String? = null
)

data class StatsPeriodSnapshot(
    val summary: StatsSummary,
    val artistEntries: List<StatsArtistEntry>,
    val albumEntries: List<StatsAlbumEntry>,
    val trackEntries: List<StatsTrackEntry>
)

object ListenStatsCalculator {

    val emptySnapshot = StatsPeriodSnapshot(
        summary = StatsSummary(0L, 0, 0),
        artistEntries = emptyList(),
        albumEntries = emptyList(),
        trackEntries = emptyList()
    )

    fun dayStartMs(timeMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMs
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun periodStartMs(period: StatsPeriod, nowMs: Long = System.currentTimeMillis()): Long {
        if (period == StatsPeriod.AllTime) return 0L

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowMs
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        when (period) {
            StatsPeriod.Today -> {}
            StatsPeriod.ThisWeek -> calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            StatsPeriod.ThisMonth -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            StatsPeriod.ThisYear -> calendar.set(Calendar.DAY_OF_YEAR, 1)
            StatsPeriod.AllTime -> {}
        }
        return calendar.timeInMillis
    }

    fun filterEventsForPeriod(allEvents: List<ListenEvent>, period: StatsPeriod): List<ListenEvent> {
        if (period == StatsPeriod.AllTime) return allEvents
        val startMs = periodStartMs(period)
        return allEvents.filter { it.dayStartMs >= startMs }
    }

    fun buildSnapshot(periodEvents: List<ListenEvent>): StatsPeriodSnapshot {
        if (periodEvents.isEmpty()) return emptySnapshot

        val uniqueAlbumKeys = periodEvents.map { it.album }.toSet()
        val summary = StatsSummary(
            totalListenedMs = periodEvents.sumOf { it.listenedMs },
            playCount = periodEvents.count { it.countsAsPlay },
            uniqueAlbumCount = uniqueAlbumKeys.size
        )

        val listenedByArtist = LinkedHashMap<String, Long>()
        for (event in periodEvents) {
            listenedByArtist[event.artist] = (listenedByArtist[event.artist] ?: 0L) + event.listenedMs
        }
        val artistEntries = listenedByArtist.entries
            .sortedByDescending { it.value }
            .map { StatsArtistEntry(it.key, it.value) }

        val eventsByAlbum = periodEvents.groupBy { it.album }
        val albumEntries = eventsByAlbum.entries
            .map { (album, events) ->
                StatsAlbumEntry(
                    albumName = album,
                    artistName = events.first().artist,
                    listenedMs = events.sumOf { it.listenedMs }
                )
            }
            .sortedByDescending { it.listenedMs }

        val eventsByTrack = periodEvents.groupBy { it.trackId }
        val trackEntries = eventsByTrack.entries
            .map { (trackId, events) ->
                val first = events.first()
                StatsTrackEntry(
                    trackId = trackId,
                    title = first.title,
                    artistName = first.artist,
                    albumName = first.album,
                    listenedMs = events.sumOf { it.listenedMs },
                    albumArtUri = first.albumArtUri
                )
            }
            .sortedByDescending { it.listenedMs }

        return StatsPeriodSnapshot(summary, artistEntries, albumEntries, trackEntries)
    }

    fun encodeEvents(events: List<ListenEvent>): String {
        val sb = StringBuilder("[")
        events.forEachIndexed { index, e ->
            if (index > 0) sb.append(',')
            sb.append('{')
            sb.append("\"trackId\":").append(e.trackId).append(',')
            sb.append("\"title\":\"").append(jsonEscape(e.title)).append("\",")
            sb.append("\"artist\":\"").append(jsonEscape(e.artist)).append("\",")
            sb.append("\"album\":\"").append(jsonEscape(e.album)).append("\",")
            sb.append("\"dayStartMs\":").append(e.dayStartMs).append(',')
            sb.append("\"timestampMs\":").append(e.timestampMs).append(',')
            sb.append("\"listenedMs\":").append(e.listenedMs).append(',')
            sb.append("\"countsAsPlay\":").append(e.countsAsPlay).append(',')
            sb.append("\"albumArtUri\":\"").append(jsonEscape(e.albumArtUri ?: "")).append("\"")
            sb.append('}')
        }
        sb.append(']')
        return sb.toString()
    }

    fun decodeEvents(json: String): List<ListenEvent> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val events = mutableListOf<ListenEvent>()

            var i = 0
            while (i < json.length) {
                val objStart = json.indexOf('{', i)
                if (objStart == -1) break
                val objEnd = json.indexOf('}', objStart)
                if (objEnd == -1) break
                val objBody = json.substring(objStart + 1, objEnd)
                events.add(parseEventObject(objBody))
                i = objEnd + 1
            }
            events
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseEventObject(body: String): ListenEvent {
        val fields = mutableMapOf<String, String>()
        var idx = 0
        while (idx < body.length) {
            val keyStart = body.indexOf('"', idx)
            if (keyStart == -1) break
            val keyEnd = body.indexOf('"', keyStart + 1)
            val key = body.substring(keyStart + 1, keyEnd)
            val colonIdx = body.indexOf(':', keyEnd)
            var valueStart = colonIdx + 1
            val value: String
            if (body[valueStart] == '"') {
                val valueEnd = findStringEnd(body, valueStart + 1)
                value = jsonUnescape(body.substring(valueStart + 1, valueEnd))
                idx = valueEnd + 1
            } else {
                var valueEnd = valueStart
                while (valueEnd < body.length && body[valueEnd] != ',' && body[valueEnd] != '}') valueEnd++
                value = body.substring(valueStart, valueEnd).trim()
                idx = valueEnd
            }
            fields[key] = value
            idx = body.indexOf(',', idx).let { if (it == -1) body.length else it + 1 }
        }
        return ListenEvent(
            trackId = fields["trackId"]?.toLongOrNull() ?: 0L,
            title = fields["title"] ?: "",
            artist = fields["artist"] ?: "",
            album = fields["album"] ?: "",
            dayStartMs = fields["dayStartMs"]?.toLongOrNull() ?: 0L,
            timestampMs = fields["timestampMs"]?.toLongOrNull() ?: 0L,
            listenedMs = fields["listenedMs"]?.toLongOrNull() ?: 0L,
            countsAsPlay = fields["countsAsPlay"] == "true",
            albumArtUri = fields["albumArtUri"]?.takeIf { it.isNotEmpty() }
        )
    }

    private fun findStringEnd(s: String, start: Int): Int {
        var i = start
        while (i < s.length) {
            if (s[i] == '\\') {
                i += 2
                continue
            }
            if (s[i] == '"') return i
            i++
        }
        return s.length
    }

    private fun jsonEscape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun jsonUnescape(s: String): String =
        s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
}
