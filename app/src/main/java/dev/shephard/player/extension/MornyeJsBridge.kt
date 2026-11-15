package dev.shephard.player.extension

import android.content.Context
import android.util.Base64
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Provides Mornye-compatible native host APIs to QuickJS extensions:
 * - HTTP requests (with per-extension CookieJar)
 * - Persistent key-value storage
 * - Base64 & Crypto (MD5 / SHA-256)
 * - Console logging
 */
class MornyeJsBridge(
    context: Context,
    private val extensionId: String
) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("ext_storage_$extensionId", Context.MODE_PRIVATE)
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val list = cookieStore.getOrPut(url.host) { mutableListOf() }
                cookies.forEach { newCookie ->
                    list.removeAll { it.name == newCookie.name }
                    list.add(newCookie)
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host]?.filter { it.expiresAt >= System.currentTimeMillis() }
                    ?: emptyList()
            }
        })
        .build()

    fun httpFetch(url: String, optionsJson: String?): String {
        return try {
            val opts = if (!optionsJson.isNullOrBlank()) JSONObject(optionsJson) else JSONObject()
            val method = opts.optString("method", "GET").uppercase()
            val bodyStr = if (opts.has("body") && !opts.isNull("body")) opts.getString("body") else null

            val reqBuilder = Request.Builder().url(url)
            val headersObj = opts.optJSONObject("headers")
            var contentType = "application/json"
            if (headersObj != null) {
                val keys = headersObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = headersObj.optString(k)
                    reqBuilder.addHeader(k, v)
                    if (k.equals("Content-Type", ignoreCase = true)) {
                        contentType = v
                    }
                }
            }

            if (method == "POST" || method == "PUT" || method == "PATCH") {
                val mediaType = contentType.toMediaTypeOrNull()
                reqBuilder.method(method, (bodyStr ?: "").toRequestBody(mediaType))
            } else if (method == "DELETE") {
                reqBuilder.delete(bodyStr?.toRequestBody(contentType.toMediaTypeOrNull()))
            } else {
                reqBuilder.get()
            }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                val respHeaders = JSONObject()
                response.headers.names().forEach { name ->
                    respHeaders.put(name.lowercase(), response.header(name) ?: "")
                }
                val respBody = response.body?.string() ?: ""
                JSONObject().apply {
                    put("ok", response.isSuccessful)
                    put("status", response.code)
                    put("headers", respHeaders)
                    put("body", respBody)
                }.toString()
            }
        } catch (e: Exception) {
            Log.e("MornyeBridge[$extensionId]", "fetch error: $url", e)
            JSONObject().apply {
                put("ok", false)
                put("status", 0)
                put("error", e.message ?: "Network error")
                put("body", "")
            }.toString()
        }
    }

    fun clearCookies() {
        cookieStore.clear()
    }

    fun storageGet(key: String): String? = prefs.getString(key, null)

    fun storageSet(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun storageRemove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun log(level: String, message: String) {
        when (level.lowercase()) {
            "error" -> Log.e("Ext[$extensionId]", message)
            "warn" -> Log.w("Ext[$extensionId]", message)
            else -> Log.d("Ext[$extensionId]", message)
        }
    }

    fun btoa(input: String): String =
        Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    fun atob(input: String): String =
        String(Base64.decode(input, Base64.DEFAULT), Charsets.UTF_8)

    fun hash(algorithm: String, input: String): String {
        val md = MessageDigest.getInstance(algorithm)
        return md.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
