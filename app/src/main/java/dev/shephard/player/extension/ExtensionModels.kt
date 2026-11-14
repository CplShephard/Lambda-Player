package dev.shephard.player.extension

import java.io.File

data class ExtensionManifest(
    val name: String,
    val displayName: String,
    val version: String,
    val description: String = "",
    val author: String = "",
    val types: List<String> = emptyList(),
    val allowHttp: Boolean = false,
    val searchEnabled: Boolean = true,
    val searchPlaceholder: String = "Search...",
    val qualityOptions: List<QualityOption> = emptyList(),
    val settings: List<ExtensionSetting> = emptyList()
)

data class QualityOption(
    val id: String,
    val label: String,
    val description: String = ""
)

data class ExtensionSetting(
    val key: String,
    val label: String,
    val type: String = "string",
    val defaultValue: String = "",
    val description: String = ""
)

data class InstalledExtension(
    val manifest: ExtensionManifest,
    val directory: File,
    val isEnabled: Boolean = true
) {
    val indexJsFile: File get() = File(directory, "index.js")
    val iconFile: File get() = File(directory, "icon.png")
}

data class RegistryExtension(
    val id: String,
    val name: String,
    val displayName: String,
    val version: String,
    val description: String,
    val author: String,
    val downloadUrl: String,
    val iconUrl: String? = null
)

data class ExtensionTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val coverUrl: String? = null,
    val extensionId: String,
    val extraJson: String = "{}"
)

data class ExtensionDownloadResult(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val format: String = "mp3",
    val quality: String = ""
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    TAGGING,
    COMPLETED,
    FAILED
}

data class DownloadTask(
    val id: String,
    val track: ExtensionTrack,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val filePath: String? = null,
    val errorMessage: String? = null
)
