package dev.shephard.player.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ReleaseInfo(val tagName: String, val htmlUrl: String, val body: String)

private const val REPO_OWNER = "CplShephard"
private const val REPO_NAME = "Lambda-Player"

suspend fun checkForUpdate(currentVersion: String): ReleaseInfo? = withContext(Dispatchers.IO) {
    try {
        val url = java.net.URL("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "LambdaPlayer/$currentVersion")
        if (conn.responseCode != 200) return@withContext null
        val body = conn.inputStream.bufferedReader().readText()
        val json = org.json.JSONObject(body)
        val tag = json.optString("tag_name").trimStart('v')
        val htmlUrl = json.optString("html_url")
        val releaseBody = json.optString("body")
        if (tag.isNotBlank() && isNewer(tag, currentVersion)) {
            ReleaseInfo(tag, htmlUrl, releaseBody)
        } else null
    } catch (_: Exception) { null }
}

private fun isNewer(remote: String, current: String): Boolean {
    fun parts(v: String) = v.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
    val r = parts(remote)
    val c = parts(current)
    for (i in 0 until maxOf(r.size, c.size)) {
        val rv = r.getOrElse(i) { 0 }
        val cv = c.getOrElse(i) { 0 }
        if (rv > cv) return true
        if (rv < cv) return false
    }
    return false
}
