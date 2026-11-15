// SPDX-License-Identifier: GPL-3.0-only
// Per-song embedded-art lookup follows the approach used by PixelPlayer's AlbumArtUtils.
package dev.shephard.player.data

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

/** Lazy, versioned, song-specific artwork. No album-wide or folder-image fallback is used. */
class SongArtworkProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = if (parse(uri) != null) "image/*" else null

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Artwork is read-only")
        val (id, version) = parse(uri) ?: throw FileNotFoundException("Invalid artwork URI")
        val app = context ?: throw FileNotFoundException("Provider not ready")
        val dir = File(app.cacheDir, "song_art_v1").apply { mkdirs() }
        val name = "${id}_$version"
        val art = File(dir, "$name.art")
        val missing = File(dir, "$name.none")
        // Lock by song, not globally: simultaneous thumbnails can decode in parallel.
        val lock = locks[(id % locks.size).toInt()]
        synchronized(lock) {
            if (art.isFile && art.length() > 0L) {
                art.setLastModified(System.currentTimeMillis())
                return ParcelFileDescriptor.open(art, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            if (missing.exists()) throw FileNotFoundException("No embedded artwork")
            val media = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val audioUri = ContentUris.withAppendedId(media, id)
            val retriever = MediaMetadataRetriever()
            val picture = try {
                retriever.setDataSource(app, audioUri)
                retriever.embeddedPicture
            } catch (e: Exception) {
                // Do not persist transient permission/provider failures as permanent misses.
                throw FileNotFoundException("Could not read embedded artwork").apply { initCause(e) }
            } finally {
                retriever.release()
            }
            if (picture == null || picture.isEmpty()) {
                runCatching { missing.createNewFile(); trimCache(dir, missing) }
                throw FileNotFoundException("No embedded artwork")
            }
            if (picture.size > MAX_ART_BYTES) throw FileNotFoundException("Artwork is too large")
            val temporary = File.createTempFile("art_", ".tmp", dir)
            try {
                temporary.writeBytes(picture)
                if (!temporary.renameTo(art)) throw FileNotFoundException("Could not cache artwork")
                val descriptor = ParcelFileDescriptor.open(art, ParcelFileDescriptor.MODE_READ_ONLY)
                trimCache(dir, art)
                return descriptor
            } finally {
                temporary.delete()
            }
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val MAX_ART_BYTES = 20 * 1024 * 1024
        private const val MAX_CACHE_BYTES = 80L * 1024 * 1024
        private val locks = Array(32) { Any() }
        private val trimLock = Any()

        fun uri(context: Context, songId: Long, dateAdded: Long, dateModified: Long,
                size: Long, generation: Long, sourcePath: String?, artworkRevision: Long): Uri {
            val signature = listOf(songId, dateAdded, dateModified, size, generation, sourcePath.orEmpty(), artworkRevision).joinToString("|")
            val hash = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray(Charsets.UTF_8))
                .take(12).joinToString("") { "%02x".format(it.toInt() and 0xff) }
            return Uri.Builder().scheme("content").authority("${context.packageName}.songart")
                .appendPath("song").appendPath(songId.toString()).appendQueryParameter("v", hash).build()
        }

        internal fun parse(uri: Uri): Pair<Long, String>? {
            val parts = uri.pathSegments
            if (parts.size != 2 || parts[0] != "song") return null
            val id = parts[1].toLongOrNull()?.takeIf { it >= 0L } ?: return null
            val version = uri.getQueryParameter("v")?.takeIf { it.matches(Regex("[0-9a-f]{24}")) } ?: return null
            return id to version
        }

        private fun trimCache(dir: File, keep: File) = synchronized(trimLock) {
            val files = dir.listFiles()?.filter { it.isFile && (it.name.endsWith(".art") || it.name.endsWith(".none")) } ?: return@synchronized
            var bytes = files.sumOf { it.length() }
            // Bound both bytes and negative entries; delete least recently used first.
            var count = files.size
            for (file in files.sortedBy { it.lastModified() }) {
                if (bytes <= MAX_CACHE_BYTES && count <= 5000) break
                if (file == keep) continue
                val length = file.length()
                if (file.delete()) { bytes -= length; count-- }
            }
        }
    }
}
