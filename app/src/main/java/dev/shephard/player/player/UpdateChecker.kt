package dev.shephard.player.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.shephard.player.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_RELEASES_API = "https://api.github.com/repos/CplShephard/Lambda-Player/releases/latest"

data class GithubReleaseInfo(
    val tagName: String,
    val name: String,
    val htmlUrl: String
)

object UpdateChecker {
    suspend fun checkLatestRelease(): GithubReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(GITHUB_RELEASES_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Lambda-Player/${BuildConfig.VERSION_NAME}")
            }
            connection.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                val tag = json.optString("tag_name")
                val name = json.optString("name", tag)
                val url = json.optString("html_url")
                if (tag.isBlank() || url.isBlank()) {
                    null
                } else {
                    GithubReleaseInfo(tag, name, url)
                        .takeIf { isNewerVersion(tag, BuildConfig.VERSION_NAME) }
                }
            }
        }.getOrNull()
    }

    fun openRelease(context: Context, releaseUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val current = currentVersion.trim().removePrefix("v").removePrefix("V")
        val remoteParts = remote.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
        val currentParts = current.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
        val max = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until max) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return remote != current && remoteTag != currentVersion
    }
}
