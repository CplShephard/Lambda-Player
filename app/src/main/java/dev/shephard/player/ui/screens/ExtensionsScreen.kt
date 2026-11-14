package dev.shephard.player.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import dev.shephard.player.extension.ExtensionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    extensionManager: ExtensionManager,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val installed by extensionManager.installedExtensions.collectAsState()
    val registry by extensionManager.registryExtensions.collectAsState()
    val scope = rememberCoroutineScope()

    var isLoadingRegistry by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = extensionManager.installFromUri(uri)
                statusMessage = res.fold(
                    onSuccess = { "Installed ${it.manifest.displayName} (${it.manifest.version})" },
                    onFailure = { "Install failed: ${it.message}" }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoadingRegistry = true
                                val res = extensionManager.fetchRegistry()
                                isLoadingRegistry = false
                                res.onFailure { statusMessage = "Registry error: ${it.message}" }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Registry")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Install .sflx")
                    }
                    OutlinedButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Search & Download")
                    }
                }
            }

            if (statusMessage != null) {
                item {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Text(
                    text = "Installed (${installed.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(installed, key = { it.manifest.name }) { ext ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ext.manifest.displayName,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "v${ext.manifest.version} • ${ext.manifest.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (ext.manifest.description.isNotBlank()) {
                                Text(
                                    text = ext.manifest.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                        Switch(
                            checked = ext.isEnabled,
                            onCheckedChange = {
                                extensionManager.setExtensionEnabled(ext.manifest.name, it)
                            }
                        )
                        IconButton(
                            onClick = { extensionManager.uninstallExtension(ext.manifest.name) }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Uninstall")
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mornye Registry (${registry.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (isLoadingRegistry) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    }
                }
            }

            items(registry, key = { it.id }) { item ->
                val isInstalled = installed.any { it.manifest.name == item.name }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "v${item.version} • ${item.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val res = extensionManager.installFromUrl(item.downloadUrl)
                                    statusMessage = res.fold(
                                        onSuccess = { "Installed ${it.manifest.displayName}" },
                                        onFailure = { "Failed: ${it.message}" }
                                    )
                                }
                            }
                        ) {
                            Text(if (isInstalled) "Update" else "Install")
                        }
                    }
                }
            }
        }
    }
}
