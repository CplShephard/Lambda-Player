package dev.shephard.player.extension

import android.content.Context
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Executes a single Mornye-compatible JS extension inside QuickJS.
 */
class ExtensionRuntime(
    private val context: Context,
    val extension: InstalledExtension
) {
    private val bridge = MornyeJsBridge(context, extension.manifest.name)

    private suspend fun <T> withRuntime(block: suspend (QuickJs) -> T): T = withContext(Dispatchers.IO) {
        val qjs = QuickJs.create(Dispatchers.IO)
        try {
            registerBridge(qjs)
            val script = extension.indexJsFile.readText()
            qjs.evaluate<Any?>(GLOBAL_SHIM)
            qjs.evaluate<Any?>(script)
            block(qjs)
        } finally {
            qjs.close()
        }
    }

    private fun registerBridge(qjs: QuickJs) {
        qjs.define("nativeBridge") {
            function("fetch") { args ->
                val url = args.getOrNull(0)?.toString() ?: ""
                val opts = args.getOrNull(1)?.toString()
                bridge.httpFetch(url, opts)
            }
            function("clearCookies") {
                bridge.clearCookies()
            }
            function("storageGet") { args ->
                val key = args.getOrNull(0)?.toString() ?: ""
                bridge.storageGet(key)
            }
            function("storageSet") { args ->
                val key = args.getOrNull(0)?.toString() ?: ""
                val value = args.getOrNull(1)?.toString() ?: ""
                bridge.storageSet(key, value)
            }
            function("storageRemove") { args ->
                val key = args.getOrNull(0)?.toString() ?: ""
                bridge.storageRemove(key)
            }
            function("log") { args ->
                val level = args.getOrNull(0)?.toString() ?: "info"
                val msg = args.getOrNull(1)?.toString() ?: ""
                bridge.log(level, msg)
            }
            function("btoa") { args ->
                bridge.btoa(args.getOrNull(0)?.toString() ?: "")
            }
            function("atob") { args ->
                bridge.atob(args.getOrNull(0)?.toString() ?: "")
            }
            function("md5") { args ->
                bridge.hash("MD5", args.getOrNull(0)?.toString() ?: "")
            }
            function("sha256") { args ->
                bridge.hash("SHA-256", args.getOrNull(0)?.toString() ?: "")
            }
        }
    }

    suspend fun search(query: String, limit: Int = 25): List<ExtensionTrack> = withRuntime { qjs ->
        val escapedQuery = JSONObject.quote(query)
        val jsCall = """
            (async () => {
                const ext = (typeof module !== 'undefined' && module.exports) ? module.exports : globalThis;
                const fn = ext.search || ext.searchTracks || globalThis.search;
                if (typeof fn !== 'function') return JSON.stringify([]);
                const res = await fn($escapedQuery, $limit);
                return JSON.stringify(res || []);
            })()
        """.trimIndent()
        val raw = qjs.evaluate<String?>(jsCall) ?: "[]"
        parseTrackList(raw)
    }

    suspend fun getDownloadUrl(track: ExtensionTrack, quality: String = ""): ExtensionDownloadResult? =
        withRuntime { qjs ->
            val trackJson = JSONObject.quote(track.extraJson)
            val qualityJson = JSONObject.quote(quality)
            val jsCall = """
                (async () => {
                    const ext = (typeof module !== 'undefined' && module.exports) ? module.exports : globalThis;
                    const fn = ext.getDownloadUrl || ext.getStreamUrl || ext.download || globalThis.getDownloadUrl;
                    if (typeof fn !== 'function') return null;
                    const trackObj = JSON.parse($trackJson);
                    const res = await fn(trackObj, $qualityJson);
                    return res ? JSON.stringify(res) : null;
                })()
            """.trimIndent()
            val raw = qjs.evaluate<String?>(jsCall) ?: return@withRuntime null
            val obj = JSONObject(raw)
            val url = obj.optString("url", "")
            if (url.isBlank()) return@withRuntime null

            val headersMap = mutableMapOf<String, String>()
            val headersObj = obj.optJSONObject("headers")
            if (headersObj != null) {
                val keys = headersObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    headersMap[k] = headersObj.optString(k)
                }
            }
            ExtensionDownloadResult(
                url = url,
                headers = headersMap,
                format = obj.optString("format", "mp3"),
                quality = obj.optString("quality", quality)
            )
        }

    private fun parseTrackList(rawJson: String): List<ExtensionTrack> {
        val result = mutableListOf<ExtensionTrack>()
        val trimmed = rawJson.trim()
        val array = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val obj = JSONObject(trimmed)
            obj.optJSONArray("tracks") ?: obj.optJSONArray("items") ?: JSONArray()
        }

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", i.toString())
            val title = item.optString("title", item.optString("name", "Unknown"))
            val artist = item.optString("artist", item.optString("artists", "Unknown Artist"))
            val album = item.optString("album", "")
            val durationMs = item.optLong("durationMs", item.optLong("duration", 0L) * 1000L)
            val coverUrl = item.optString("coverUrl", item.optString("artwork", "")).ifBlank { null }

            result.add(
                ExtensionTrack(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = durationMs,
                    coverUrl = coverUrl,
                    extensionId = extension.manifest.name,
                    extraJson = item.toString()
                )
            )
        }
        return result
    }

    companion object {
        private val GLOBAL_SHIM = """
            var module = { exports: {} };
            var exports = module.exports;
            var console = {
                log: (...a) => nativeBridge.log('info', a.map(String).join(' ')),
                warn: (...a) => nativeBridge.log('warn', a.map(String).join(' ')),
                error: (...a) => nativeBridge.log('error', a.map(String).join(' '))
            };
            var localStorage = {
                getItem: (k) => nativeBridge.storageGet(String(k)),
                setItem: (k, v) => nativeBridge.storageSet(String(k), String(v)),
                removeItem: (k) => nativeBridge.storageRemove(String(k))
            };
            function btoa(s) { return nativeBridge.btoa(String(s)); }
            function atob(s) { return nativeBridge.atob(String(s)); }
            async function fetch(url, options) {
                const raw = nativeBridge.fetch(String(url), options ? JSON.stringify(options) : null);
                const res = JSON.parse(raw);
                return {
                    ok: !!res.ok,
                    status: res.status || 0,
                    headers: res.headers || {},
                    text: async () => res.body || '',
                    json: async () => JSON.parse(res.body || '{}')
                };
            }
        """.trimIndent()
    }
}
