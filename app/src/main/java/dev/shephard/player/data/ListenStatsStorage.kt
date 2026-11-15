package dev.shephard.player.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * File-based storage for listen stats.
 * DataStore is unsuitable for large JSON (> ~1MB) — it serializes the entire
 * preferences map on every edit and causes OOM with heap limit 256MB.
 */
object ListenStatsStorage {

    private const val FILE_NAME = "listen_stats_events.json"
    private const val MAX_EVENTS = 15_000
    private const val MAX_JSON_BYTES = 1_900_000 // keep well below DataStore safe limit (~2MB)
    private const val MIGRATION_MAX_JSON_BYTES = 4_000_000 // if DataStore value bigger, truncate aggressively

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    suspend fun load(context: Context, dataStoreJsonFallback: suspend () -> String): List<ListenEvent> =
        withContext(Dispatchers.IO) {
            val f = file(context)
            if (f.exists() && f.length() > 0) {
                try {
                    // Guard against truncated/corrupted writes: read text, if too large truncate
                    val text = f.readText()
                    if (text.length > MIGRATION_MAX_JSON_BYTES * 2) {
                        // file itself got too large (should not happen with new cap, but handle legacy)
                        return@withContext decodeAndCap(text)
                    }
                    val events = try {
                        ListenStatsCalculator.decodeEvents(text)
                    } catch (e: OutOfMemoryError) {
                        emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    if (events.isNotEmpty()) return@withContext events
                } catch (e: OutOfMemoryError) {
                    // file too large to read -> drop and start fresh
                } catch (e: Exception) {
                }
            }
            // Fallback: migrate from DataStore (old location)
            try {
                val json = dataStoreJsonFallback()
                if (json.isBlank() || json == "[]") return@withContext emptyList()
                if (json.length > MIGRATION_MAX_JSON_BYTES) {
                    // Don't try to decode 50MB string fully — take tail slice containing recent events
                    // Each event ~120-300 chars, so 1.9MB ~ 8k-12k events. Keep tail.
                    val tail = json.takeLast(MIGRATION_MAX_JSON_BYTES)
                    // tail may start mid-object, find first '{' to align
                    val aligned = tail.substringAfter('{', "")
                    val repaired = if (aligned.isEmpty()) "[]" else "[{" + aligned
                    return@withContext decodeAndCap(repaired)
                }
                decodeAndCap(json)
            } catch (e: OutOfMemoryError) {
                emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun save(context: Context, events: List<ListenEvent>): Boolean = withContext(Dispatchers.IO) {
        try {
            val capped = capEvents(events)
            val json = try {
                ListenStatsCalculator.encodeEvents(capped)
            } catch (e: OutOfMemoryError) {
                // try with smaller cap
                val smaller = capEvents(capped.takeLast(MAX_EVENTS / 2))
                ListenStatsCalculator.encodeEvents(smaller)
            }
            // extra safety: if still too large, trim further
            var finalJson = json
            var finalEvents = capped
            var attempts = 0
            while (finalJson.length > MAX_JSON_BYTES && finalEvents.size > 1000 && attempts < 5) {
                finalEvents = finalEvents.takeLast((finalEvents.size * 0.7).toInt().coerceAtLeast(1000))
                finalJson = ListenStatsCalculator.encodeEvents(finalEvents)
                attempts++
            }
            val f = file(context)
            val tmp = File(f.parent, "${f.name}.tmp")
            tmp.writeText(finalJson)
            // atomic replace
            if (f.exists()) f.delete()
            tmp.renameTo(f)
            true
        } catch (e: OutOfMemoryError) {
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        try { file(context).delete() } catch (_: Exception) {}
    }

    private fun decodeAndCap(json: String): List<ListenEvent> {
        val decoded = try {
            ListenStatsCalculator.decodeEvents(json)
        } catch (e: OutOfMemoryError) {
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
        return capEvents(decoded)
    }

    private fun capEvents(events: List<ListenEvent>): List<ListenEvent> {
        if (events.isEmpty()) return events
        var capped = events
        // cap by count
        if (capped.size > MAX_EVENTS) {
            capped = capped.takeLast(MAX_EVENTS)
        }
        // cap by age: keep last 365 days max, but never drop below 2000 recent events
        val now = System.currentTimeMillis()
        val oneYearAgo = now - 365L * 24 * 60 * 60 * 1000
        val recentYear = capped.filter { it.timestampMs >= oneYearAgo || it.dayStartMs >= oneYearAgo }
        if (recentYear.size >= 2000) {
            capped = recentYear
        }
        return capped
    }

    /**
     * Migration helper: after successful file load/save, clear the heavy DataStore key
     * so future DataStore writes don't need to serialize 50MB again.
     * This must be called from a coroutine that can tolerate OOM — it catches all.
     */
    suspend fun clearDataStoreKeyIfNeeded(
        getJson: suspend () -> String,
        clearAction: suspend () -> Unit
    ) {
        try {
            val json = getJson()
            if (json.length > 500_000) { // >500KB is already risky in DataStore
                try {
                    clearAction()
                } catch (e: OutOfMemoryError) {
                    // even clearing can OOM if preferences map is huge; try direct file delete fallback
                } catch (e: Exception) {}
            }
        } catch (e: OutOfMemoryError) {
        } catch (e: Exception) {}
    }
}
