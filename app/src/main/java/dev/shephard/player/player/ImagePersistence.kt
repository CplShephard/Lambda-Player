package dev.shephard.player.player

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object ImagePersistence {

    private const val COVERS_DIR = "persisted_covers"
    private const val WALLPAPER_DIR = "persisted_wallpaper"

    suspend fun persistImage(
        context: Context,
        sourceUri: Uri,
        subDir: String,
        fileNamePrefix: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, subDir).apply { mkdirs() }

            val mimeType = context.contentResolver.getType(sourceUri)
            val extension = when (mimeType) {
                "image/gif" -> "gif"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val destFile = File(dir, "${fileNamePrefix}${System.currentTimeMillis()}.$extension")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    suspend fun persistCover(context: Context, sourceUri: Uri): Uri? =
        persistImage(context, sourceUri, COVERS_DIR, "cover_")

    suspend fun persistWallpaper(context: Context, sourceUri: Uri): Uri? =
        persistImage(context, sourceUri, WALLPAPER_DIR, "wallpaper_")
}
