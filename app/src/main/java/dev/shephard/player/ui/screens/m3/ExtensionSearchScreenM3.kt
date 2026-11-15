package dev.shephard.player.ui.screens.m3

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionSearchScreenM3(
    extensionManager: ExtensionManager,
    downloadManager: ExtensionDownloadManager,
    onLibraryRefresh: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true
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
                title = { Text("Discover") },
                navigationIcon = if (showBack) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                } else {
                    {}
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (activeExtensions.isEmpty()) {
                Text(
                    text = "Aktif extension yok. Ayarlar → Online Mod üzerinden kurun.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeExtensions, key = { it.manifest.name }) { ext ->
                        FilterChip(
                            selected = ext.manifest.name == selectedExtId,
                            onClick = { selectedExtId = ext.manifest.name },
                            label = { Text(ext.manifest.displayName) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Şarkı veya sanatçı ara...") },
                    trailingIcon = {
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
                )
            }

            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "İndirmeler (${tasks.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(tasks, key = { it.id }) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${task.track.artist} - ${task.track.title}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = when (task.status) {
                                        DownloadStatus.QUEUED -> "Sırada"
                                        DownloadStatus.DOWNLOADING -> "İndiriliyor ${(task.progress * 100).toInt()}%"
                                        DownloadStatus.TAGGING -> "Etiketleniyor..."
                                        DownloadStatus.COMPLETED -> "Tamamlandı"
                                        DownloadStatus.FAILED -> "Hata: ${task.errorMessage}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (task.status == DownloadStatus.DOWNLOADING) {
                                    LinearProgressIndicator(
                                        progress = { task.progress },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                items(results, key = { "${it.extensionId}_${it.id}" }) { track ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = buildString {
                                        append(track.artist)
                                        if (track.album.isNotBlank()) append(" • ${track.album}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
