package dev.shephard.player.extension

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Manages Mornye-compatible .sflx extension packages:
 * - Loading installed extensions from internal storage
 * - Installing .sflx (ZIP) packages from Uri or URL
 * - Fetching available extensions from Mornye-Extension registry.json
 */
class ExtensionManager(private val context: Context) {

    private val extensionsRoot = File(context.filesDir, "extensions").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("lambda_extensions_meta", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()

    private val _installedExtensions = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val installedExtensions: StateFlow<List<InstalledExtension>> = _installedExtensions.asStateFlow()

    private val _registryExtensions = MutableStateFlow<List<RegistryExtension>>(emptyList())
    val registryExtensions: StateFlow<List<RegistryExtension>> = _registryExtensions.asStateFlow()

    init {
        reloadInstalled()
    }

    fun reloadInstalled() {
        val list = extensionsRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val manifestFile = File(dir, "manifest.json")
                val indexFile = File(dir, "index.js")
                if (!manifestFile.exists() || !indexFile.exists()) return@mapNotNull null
                runCatching {
                    val manifest = parseManifest(manifestFile.readText())
                    val enabled = prefs.getBoolean("enabled_${manifest.name}", true)
                    InstalledExtension(manifest = manifest, directory = dir, isEnabled = enabled)
                }.getOrNull()
            }
            ?.sortedBy { it.manifest.displayName.lowercase() }
            ?: emptyList()
        _installedExtensions.value = list
    }

    fun setExtensionEnabled(extensionId: String, enabled: Boolean) {
        prefs.edit().putBoolean("enabled_$extensionId", enabled).apply()
        reloadInstalled()
    }

    fun uninstallExtension(extensionId: String) {
        val dir = File(extensionsRoot, extensionId)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        prefs.edit().remove("enabled_$extensionId").apply()
        reloadInstalled()
    }

    suspend fun installFromUri(uri: Uri): Result<InstalledExtension> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open selected file")
            stream.use { installFromZipStream(it) }
        }
    }

    suspend fun installFromUrl(url: String): Result<InstalledExtension> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Download failed: HTTP ${resp.code}")
                }
                val body = resp.body ?: throw IllegalStateException("Empty response body")
                body.byteStream().use { installFromZipStream(it) }
            }
        }
    }

    private fun installFromZipStream(inputStream: InputStream): InstalledExtension {
        val tempDir = File(context.cacheDir, "ext_unpack_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(tempDir, entry.name)
                        // Prevent Zip Path Traversal
                        if (outFile.canonicalPath.startsWith(tempDir.canonicalPath)) {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { out -> zis.copyTo(out) }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Support both root-level manifest.json and single nested folder
            val manifestFile = findFile(tempDir, "manifest.json")
                ?: throw IllegalStateException("Invalid .sflx package: manifest.json not found")
            val packageRoot = manifestFile.parentFile ?: tempDir
            val indexFile = File(packageRoot, "index.js")
            if (!indexFile.exists()) {
                throw IllegalStateException("Invalid .sflx package: index.js not found")
            }

            val manifest = parseManifest(manifestFile.readText())
            val targetDir = File(extensionsRoot, manifest.name)
            if (targetDir.exists()) targetDir.deleteRecursively()
            packageRoot.copyRecursively(targetDir, overwrite = true)

            prefs.edit().putBoolean("enabled_${manifest.name}", true).apply()
            reloadInstalled()

            return InstalledExtension(manifest = manifest, directory = targetDir, isEnabled = true)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun fetchRegistry(
        registryUrl: String = DEFAULT_REGISTRY_URL
    ): Result<List<RegistryExtension>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(registryUrl).build()
            val jsonStr = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string() ?: "{}"
            }
            val list = parseRegistry(jsonStr)
            _registryExtensions.value = list
            list
        }
    }

    fun getRuntime(extensionId: String): ExtensionRuntime? {
        val ext = _installedExtensions.value.firstOrNull {
            it.manifest.name == extensionId && it.isEnabled
        } ?: return null
        return ExtensionRuntime(context, ext)
    }

    private fun findFile(dir: File, name: String): File? {
        val direct = File(dir, name)
        if (direct.exists()) return direct
        return dir.listFiles()?.firstNotNullOfOrNull {
            if (it.isDirectory) findFile(it, name) else null
        }
    }

    private fun parseManifest(jsonStr: String): ExtensionManifest {
        val obj = JSONObject(jsonStr)
        val typesArr = obj.optJSONArray("type") ?: JSONArray()
        val types = (0 until typesArr.length()).mapNotNull { typesArr.optString(it).ifBlank { null } }

        val perms = obj.optJSONObject("permissions")
        val allowHttp = perms?.optBoolean("allowHttp", false) ?: false

        val searchObj = obj.optJSONObject("searchBehavior")
        val searchEnabled = searchObj?.optBoolean("enabled", true) ?: true
        val searchPlaceholder = searchObj?.optString("placeholder", "Search...") ?: "Search..."

        val qualityArr = obj.optJSONArray("qualityOptions") ?: JSONArray()
        val qualities = (0 until qualityArr.length()).mapNotNull { i ->
            val q = qualityArr.optJSONObject(i) ?: return@mapNotNull null
            QualityOption(
                id = q.optString("id"),
                label = q.optString("label"),
                description = q.optString("description", "")
            )
        }

        val settingsArr = obj.optJSONArray("settings") ?: JSONArray()
        val settings = (0 until settingsArr.length()).mapNotNull { i ->
            val s = settingsArr.optJSONObject(i) ?: return@mapNotNull null
            ExtensionSetting(
                key = s.optString("key"),
                label = s.optString("label"),
                type = s.optString("type", "string"),
                defaultValue = s.optString("default", ""),
                description = s.optString("description", "")
            )
        }

        return ExtensionManifest(
            name = obj.getString("name"),
            displayName = obj.optString("displayName", obj.getString("name")),
            version = obj.optString("version", "1.0.0"),
            description = obj.optString("description", ""),
            author = obj.optString("author", ""),
            types = types,
            allowHttp = allowHttp,
            searchEnabled = searchEnabled,
            searchPlaceholder = searchPlaceholder,
            qualityOptions = qualities,
            settings = settings
        )
    }

    private fun parseRegistry(jsonStr: String): List<RegistryExtension> {
        val trimmed = jsonStr.trim()
        val array = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val obj = JSONObject(trimmed)
            obj.optJSONArray("extensions") ?: JSONArray()
        }
        val list = mutableListOf<RegistryExtension>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val name = item.optString("name", item.optString("id", ""))
            val downloadUrl = item.optString("downloadUrl", item.optString("url", ""))
            if (name.isBlank() || downloadUrl.isBlank()) continue
            list.add(
                RegistryExtension(
                    id = item.optString("id", name),
                    name = name,
                    displayName = item.optString("displayName", name),
                    version = item.optString("version", "1.0.0"),
                    description = item.optString("description", ""),
                    author = item.optString("author", ""),
                    downloadUrl = downloadUrl,
                    iconUrl = item.optString("iconUrl", "").ifBlank { null }
                )
            )
        }
        return list
    }

    companion object {
        const val DEFAULT_REGISTRY_URL =
            "https://raw.githubusercontent.com/mornyeapp/Mornye-Extension/main/registry.json"
    }
}
