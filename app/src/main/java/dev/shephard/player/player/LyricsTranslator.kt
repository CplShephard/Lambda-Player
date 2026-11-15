package dev.shephard.player.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Simple online translator for lyrics lines.
 * Uses Google translate free endpoint when target != off/original.
 * Falls back gracefully to original translation provided by QQ/Liriqo.
 * Cached per session to avoid repeated network calls.
 */
object LyricsTranslator {
    private val cache = mutableMapOf<String, String>()

    val supportedLanguages = listOf(
        "off" to "Off",
        "original" to "Original",
        "en" to "English",
        "tr" to "Turkish",
        "de" to "German",
        "fr" to "French",
        "es" to "Spanish",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese",
        "ru" to "Russian",
        "ar" to "Arabic",
    )

    fun labelFor(code: String, strings: dev.shephard.player.ui.i18n.Strings): String = when (code) {
        "off" -> strings.lyricsTranslationOff
        "original" -> strings.lyricsTranslationOriginal
        else -> supportedLanguages.find { it.first == code }?.second ?: code.uppercase()
    }

    suspend fun translateIfNeeded(
        text: String,
        targetLang: String,
        existingTranslation: String?
    ): String? {
        if (targetLang == "off") return null
        if (targetLang == "original") return existingTranslation
        // If QQ already provided a translation and target matches original language heuristic, reuse it
        // For now, if existingTranslation exists and target is not off/original, translate original text to target
        if (text.isBlank()) return null
        // Return cached
        val key = "$targetLang:$text"
        cache[key]?.let { return it }
        // Try to translate via Google free endpoint (device network, not sandbox)
        return try {
            withContext(Dispatchers.IO) {
                val translated = translateViaGoogle(text, targetLang)
                if (!translated.isNullOrBlank() && translated != text) {
                    cache[key] = translated
                    translated
                } else existingTranslation
            }
        } catch (_: Exception) {
            existingTranslation
        }
    }

    private fun translateViaGoogle(text: String, target: String): String? {
        val encoded = URLEncoder.encode(text, "UTF-8")
        // Google translate gtx endpoint
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$target&dt=t&q=$encoded"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LambdaPlayer/2.5")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            // Response is JSON array: [[[translated, original,...]]]
            // Very lightweight parse: extract first quoted translation segment
            // Example: [[["Merhaba","Hello",...]]]
            // We pull the first string after [[[
            val array = org.json.JSONArray(body)
            val inner = array.optJSONArray(0) ?: return null
            val first = inner.optJSONArray(0) ?: return null
            first.optString(0).takeIf { it.isNotBlank() }
        } finally {
            conn.disconnect()
        }
    }

    fun clearCache() { cache.clear() }
}
