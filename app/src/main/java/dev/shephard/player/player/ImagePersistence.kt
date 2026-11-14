package dev.shephard.player.player

import android.content.Context
import android.net.Uri
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

object ImagePersistence {
    private const val COVERS_DIR = "persisted_covers"

    /** Only MP4 is accepted as a video cover. MIME can be missing for some document providers. */
    fun isMp4(context: Context, uri: Uri): Boolean {
        val type = runCatching { context.contentResolver.getType(uri) }.getOrNull()?.lowercase()
        if (type == "video/mp4") return true
        if (type != null && type != "application/octet-stream") return false
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
        return name?.substringBefore('?')?.endsWith(".mp4", ignoreCase = true) == true
    }

    /** Video covers bypass the still-image cropper; GIFs remain animated too. */
    fun isAnimatedCover(context: Context, uri: Uri): Boolean =
        runCatching { context.contentResolver.getType(uri) == "image/gif" }.getOrDefault(false) || isMp4(context, uri)

    suspend fun persistImage(context: Context, sourceUri: Uri, subDir: String, fileNamePrefix: String): Uri? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, subDir).apply { mkdirs() }
        val video = isMp4(context, sourceUri)
        val mime = runCatching { context.contentResolver.getType(sourceUri) }.getOrNull()?.lowercase()
        // Never treat another video format as an image and accidentally keep its audio.
        if (!video && mime?.startsWith("video/") == true) return@withContext null
        val extension = if (video) "mp4" else when (mime) {
            "image/gif" -> "gif"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val destination = File(dir, "${fileNamePrefix}${UUID.randomUUID()}.$extension")
        try {
            if (video) {
                // Remux the video track ONLY. Even if playback volume is later changed, this
                // persisted cover contains no audio, subtitle or data tracks whatsoever.
                writeSilentMp4(context, sourceUri, destination)
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Unable to open image")
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destination)
        } catch (e: Exception) {
            destination.delete()
            android.util.Log.w("ImagePersistence", "Cover could not be saved", e)
            null
        }
    }

    private fun writeSilentMp4(context: Context, source: Uri, destination: File) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var started = false
        try {
            extractor.setDataSource(context, source, null)
            val sourceTrack = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: error("MP4 has no video track")
            val format = extractor.getTrackFormat(sourceTrack)
            val rotation = runCatching {
                MediaMetadataRetriever().let { retriever ->
                    try { retriever.setDataSource(context, source); retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() }
                    finally { retriever.release() }
                }
            }.getOrNull()
            val output = MediaMuxer(destination.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = output
            if (rotation in listOf(0, 90, 180, 270)) output.setOrientationHint(rotation!!)
            val targetTrack = output.addTrack(format)
            extractor.selectTrack(sourceTrack)
            output.start(); started = true
            val declaredSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) runCatching { format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }.getOrDefault(0) else 0
            var buffer = ByteBuffer.allocateDirect(declaredSize.coerceIn(2 * 1024 * 1024, 32 * 1024 * 1024))
            val info = MediaCodec.BufferInfo()
            var samples = 0
            while (true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val sampleSize = extractor.sampleSize
                    if (sampleSize < 0) break
                    require(sampleSize <= 32L * 1024 * 1024) { "Video frame too large" }
                    if (sampleSize > buffer.capacity()) buffer = ByteBuffer.allocateDirect(sampleSize.toInt())
                }
                buffer.clear()
                val count = extractor.readSampleData(buffer, 0)
                if (count < 0) break
                require(count <= buffer.capacity()) { "Video frame too large" }
                require(extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED == 0) { "Encrypted video is not supported" }
                info.set(0, count, extractor.sampleTime.coerceAtLeast(0),
                    if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                output.writeSampleData(targetTrack, buffer, info)
                samples++
                extractor.advance()
            }
            require(samples > 0) { "MP4 contains no video frames" }
            output.stop(); started = false
        } finally {
            if (started) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            extractor.release()
        }
        // Fail closed if a platform muxer ever outputs anything except the one video track.
        val verify = MediaExtractor()
        try {
            verify.setDataSource(destination.absolutePath)
            require(verify.trackCount == 1 && verify.getTrackFormat(0).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true)
        } finally { verify.release() }
    }

    suspend fun persistCover(context: Context, sourceUri: Uri): Uri? {
        val result = persistImage(context, sourceUri, COVERS_DIR, "cover_")
        if (result == null) withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(context, dev.shephard.player.R.string.cover_import_error, android.widget.Toast.LENGTH_LONG).show()
        }
        return result
    }
}
