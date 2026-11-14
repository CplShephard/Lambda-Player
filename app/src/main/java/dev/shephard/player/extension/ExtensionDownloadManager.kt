package dev.shephard.player.extension

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.util.UUID

/**
 * Downloads tracks resolved by extensions, embeds metadata/artwork via jaudiotagger,
 * and triggers MediaScannerConnection so Lambda Player's local library picks them up.
 */
class ExtensionDownloadManager(
    private val context: Context,
    private val extensionManager: ExtensionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    fun enqueueDownload(
        track: ExtensionTrack,
        quality: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        val taskId = UUID.randomUUID().toString()
        val initialTask = DownloadTask(id = taskId, track = track, status = DownloadStatus.QUEUED)
        _tasks.update { listOf(initialTask) + it }

        scope.launch {
            try {
                updateTask(taskId) { it.copy(status = DownloadStatus.DOWNLOADING, progress = 0f) }

                val runtime = extensionManager.getRuntime(track.extensionId)
                    ?: throw IllegalStateException("Extension '${track.extensionId}' is not active")

                val resolved = runtime.getDownloadUrl(track, quality)
                    ?: throw IllegalStateException("Could not resolve download URL")

                val musicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "LambdaPlayer"
                ).apply { mkdirs() }

                val safeName = "${track.artist} - ${track.title}"
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    .take(120)
                val ext = resolved.format.lowercase().ifBlank { "mp3" }
                val targetFile = File(musicDir, "$safeName.$ext")

                downloadFile(resolved.url, resolved.headers, targetFile) { progress ->
                    updateTask(taskId) { it.copy(progress = progress) }
                }

                updateTask(taskId) { it.copy(status = DownloadStatus.TAGGING, progress = 1f) }
                embedMetadata(targetFile, track)

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    null
                ) { _, _ ->
                    onComplete?.invoke()
                }

                updateTask(taskId) {
                    it.copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 1f,
                        filePath = targetFile.absolutePath
                    )
                }
            } catch (e: Exception) {
                updateTask(taskId) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = e.message ?: "Download failed"
                    )
                }
            }
        }
    }

    private fun downloadFile(
        url: String,
        headers: Map<String, String>,
        destination: File,
        onProgress: (Float) -> Unit
    ) {
        val reqBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = resp.body ?: throw IllegalStateException("Empty body")
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloaded = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalBytes > 0) {
                            onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }
    }

    private fun embedMetadata(file: File, track: ExtensionTrack) {
        runCatching {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault
            tag.setField(FieldKey.TITLE, track.title)
            tag.setField(FieldKey.ARTIST, track.artist)
            if (track.album.isNotBlank()) {
                tag.setField(FieldKey.ALBUM, track.album)
            }

            if (!track.coverUrl.isNullOrBlank()) {
                val coverBytes = httpClient.newCall(Request.Builder().url(track.coverUrl).build())
                    .execute()
                    .use { if (it.isSuccessful) it.body?.bytes() else null }
                if (coverBytes != null && coverBytes.isNotEmpty()) {
                    val tempCover = File(context.cacheDir, "cover_${UUID.randomUUID()}.jpg")
                    try {
                        tempCover.writeBytes(coverBytes)
                        val artwork = ArtworkFactory.createArtworkFromFile(tempCover)
                        tag.deleteArtworkField()
                        tag.setField(artwork)
                    } finally {
                        tempCover.delete()
                    }
                }
            }
            audioFile.commit()
        }
    }

    private fun updateTask(taskId: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { list ->
            list.map { if (it.id == taskId) transform(it) else it }
        }
    }
}
