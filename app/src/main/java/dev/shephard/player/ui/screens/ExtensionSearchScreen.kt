package dev.shephard.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.shephard.player.extension.DownloadStatus
import dev.shephard.player.extension.ExtensionDownloadManager
import dev.shephard.player.extension.ExtensionManager
import dev.shephard.player.extension.ExtensionTrack
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ExtensionSearchScreen(
    extensionManager: ExtensionManager,
    downloadManager: ExtensionDownloadManager,
    onLibraryRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val installed by extensionManager.installedExtensions.collectAsState()
    val activeExtensions = remember(installed) { installed.filter { it.isEnabled } }
    val tasks by downloadManager.tasks.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedExtId by remember(activeExtensions) {
        mutableStateOf(activeExtensions.firstOrNull()?.manifest?.name)
    }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ExtensionTrack>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Discover",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            if (activeExtensions.isEmpty()) {
                Text(
                    text = "Aktif extension yok. Ayarlar → Online Mod üzerinden kurun.",
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeExtensions, key = { it.manifest.name }) { ext ->
                        val selected = ext.manifest.name == selectedExtId
                        Button(
                            onClick = { selectedExtId = ext.manifest.name }
                        ) {
                            Text(if (selected) "✓ ${ext.manifest.displayName}" else ext.manifest.displayName)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = "Şarkı veya sanatçı ara..."
                    )
                    IconButton(
                        onClick = {
                            val extId = selectedExtId ?: return@IconButton
                            if (query.isBlank()) return@IconButton
                            scope.launch {
                                isSearching = true
                                errorText = null
                                runCatching {
                                    val runtime = extensionManager.getRuntime(extId)
                                        ?: throw IllegalStateException("Extension aktif değil")
                                    runtime.search(query.trim())
                                }.onSuccess { results = it }.onFailure { errorText = it.message }
                                isSearching = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Ara")
                    }
                }
            }

            if (isSearching) {
                Text(
                    text = "Aranıyor...",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tasks.isNotEmpty()) {
                    item { SmallTitle(text = "İndirmeler (${tasks.size})") }
                    items(tasks, key = { it.id }) { task ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${task.track.artist} - ${task.track.title}",
                                    style = MiuixTheme.textStyles.subtitle
                                )
                                Text(
                                    text = when (task.status) {
                                        DownloadStatus.QUEUED -> "Sırada"
                                        DownloadStatus.DOWNLOADING -> "İndiriliyor ${(task.progress * 100).toInt()}%"
                                        DownloadStatus.TAGGING -> "Etiketleniyor..."
                                        DownloadStatus.COMPLETED -> "Tamamlandı"
                                        DownloadStatus.FAILED -> "Hata: ${task.errorMessage}"
                                    },
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                }

                items(results, key = { "${it.extensionId}_${it.id}" }) { track ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MiuixTheme.textStyles.subtitle)
                                Text(
                                    text = buildString {
                                        append(track.artist)
                                        if (track.album.isNotBlank()) append(" • ${track.album}")
                                    },
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                            IconButton(
                                onClick = {
                                    downloadManager.enqueueDownload(track = track, onComplete = onLibraryRefresh)
                                }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "İndir")
                            }
                        }
                    }
                }
            }
        }
    }
}
