package dev.shephard.player.player

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DataStoreRecovery {
    private const val SIZE_WARN_BYTES = 5_000_000L // 5MB file is suspicious for preferences
    private const val SIZE_CRITICAL_BYTES = 10_000_000L

    suspend fun maybeRecover(context: Context) = withContext(Dispatchers.IO) {
        try {
            val prefsFile = File(context.filesDir, "datastore/lambda_player_prefs.preferences_pb")
            if (!prefsFile.exists()) return@withContext
            val size = prefsFile.length()
            if (size < SIZE_WARN_BYTES) return@withContext

            // File is suspiciously large — likely LISTEN_STATS_EVENTS_JSON bloat.
            // Try to migrate heavy key to file storage before DataStore chokes on next write.
            val appContext = context.applicationContext
            // Attempt lightweight migration via direct DataStore read with OOM guard
            try {
                val prefs = PreferencesManager(appContext)
                // This read itself may OOM; catch it
                val json = kotlinx.coroutines.flow.first(prefs.listenStatsEventsJson)
                if (json.length > 500_000) {
                    // Move to file storage if not already
                    try {
                        val events = dev.shephard.player.data.ListenStatsCalculator.decodeEvents(json.takeLast(2_000_000))
                        dev.shephard.player.data.ListenStatsStorage.save(appContext, events)
                    } catch (_: OutOfMemoryError) {}
                    try { prefs.clearListenStatsEventsJson() } catch (_: Exception) {}
                }
            } catch (e: OutOfMemoryError) {
                // Even reading failed — file is critically bloated.
                // Last resort: delete preferences file to break crash loop.
                // We keep a backup for debugging if critical.
                if (size > SIZE_CRITICAL_BYTES) {
                    try {
                        val backup = File(context.filesDir, "datastore/lambda_player_prefs.preferences_pb.bak_oom")
                        prefsFile.copyTo(backup, overwrite = true)
                    } catch (_: Exception) {}
                    try { prefsFile.delete() } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }
}
