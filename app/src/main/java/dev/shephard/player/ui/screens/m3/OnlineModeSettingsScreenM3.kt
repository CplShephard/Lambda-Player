package dev.shephard.player.ui.screens.m3

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import dev.shephard.player.player.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineModeSettingsScreenM3(
    preferencesManager: PreferencesManager,
    extensionManager: ExtensionManager,
    onBack: () -> Unit
) {
    val onlineModeEnabled by preferencesManager.onlineModeEnabled.collectAsState(initial = false)
    val savedRegistryUrl by preferencesManager.extensionRegistryUrl.collectAsState(
        initial = ExtensionManager.DEFAULT_REGISTRY_URL
    )
    val installed by extensionManager.installedExtensions.collectAsState()
    val registry by extensionManager.registryExtensions.collectAsState()
    val scope = rememberCoroutineScope()

    var registryUrlInput by remember(savedRegistryUrl) { mutableStateOf(savedRegistryUrl) }
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
                title = { Text("Online Mod") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Online Mod", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Dock üzerinde Discover sekmesini ve extensionları gösterir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = onlineModeEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { preferencesManager.setOnlineModeEnabled(enabled) }
                            }
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(visible = onlineModeEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Extension Registry Linki", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = registryUrlInput,
                                    onValueChange = { registryUrlInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("https://.../registry.json") },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    val url = registryUrlInput.trim()
                                                    if (url.isNotEmpty()) {
                                                        preferencesManager.setExtensionRegistryUrl(url)
                                                        isLoadingRegistry = true
                                                        val res = extensionManager.fetchRegistry(url)
                                                        isLoadingRegistry = false
                                                        res.onFailure {
                                                            statusMessage = "Registry hatası: ${it.message}"
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Yükle")
                                        }
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { filePicker.launch(arrayOf("*/*")) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.padding(4.dp))
                                    Text("Cihazdan .sflx Yükle")
                                }
                            }
                        }

                        if (statusMessage != null) {
                            Text(
                                text = statusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text("Kurulu Extensionlar (${installed.size})", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (onlineModeEnabled) {
                items(installed, key = { "inst_${it.manifest.name}" }) { ext ->
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
                                Text(ext.manifest.displayName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "v${ext.manifest.version} • ${ext.manifest.author}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = ext.isEnabled,
                                onCheckedChange = { extensionManager.setExtensionEnabled(ext.manifest.name, it) }
                            )
                            IconButton(onClick = { extensionManager.uninstallExtension(ext.manifest.name) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Kaldır")
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Extension Listesi (${registry.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLoadingRegistry) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    }
                }

                items(registry, key = { "reg_${it.id}" }) { item ->
                    val isInstalled = installed.any { it.manifest.name == item.name }
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
                                            onSuccess = { "${it.manifest.displayName} kuruldu" },
                                            onFailure = { "Hata: ${it.message}" }
                                        )
                                    }
                                }
                            ) {
                                Text(if (isInstalled) "Güncelle" else "Kur")
                            }
                        }
                    }
                }
            }
        }
    }
}
