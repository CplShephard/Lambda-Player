package dev.shephard.player.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
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
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OnlineModeSettingsScreen(
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
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = extensionManager.installFromUri(uri)
                statusMessage = res.fold(
                    onSuccess = { "${it.manifest.displayName} (${it.manifest.version}) kuruldu" },
                    onFailure = { "Kurulum hatası: ${it.message}" }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Online Mod",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Online Mod",
                                style = MiuixTheme.textStyles.title3
                            )
                            Text(
                                text = "Dock üzerinde Discover sekmesini ve extensionları gösterir",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Extension Registry Linki", style = MiuixTheme.textStyles.subtitle)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = registryUrlInput,
                                        onValueChange = { registryUrlInput = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        label = "Registry URL"
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val url = registryUrlInput.trim()
                                                if (url.isNotEmpty()) {
                                                    preferencesManager.setExtensionRegistryUrl(url)
                                                    val res = extensionManager.fetchRegistry(url)
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
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { filePicker.launch(arrayOf("*/*")) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cihazdan .sflx Yükle")
                                }
                            }
                        }

                        if (statusMessage != null) {
                            Text(
                                text = statusMessage!!,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.primary
                            )
                        }

                        SmallTitle(text = "Kurulu Extensionlar (${installed.size})")
                    }
                }
            }

            if (onlineModeEnabled) {
                items(installed, key = { "inst_${it.manifest.name}" }) { ext ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ext.manifest.displayName, style = MiuixTheme.textStyles.subtitle)
                                Text(
                                    text = "v${ext.manifest.version} • ${ext.manifest.author}",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
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
                                Icon(Icons.Default.Delete, contentDescription = "Kaldır")
                            }
                        }
                    }
                }

                item {
                    SmallTitle(text = "Extension Listesi (${registry.size})")
                }

                items(registry, key = { "reg_${it.id}" }) { item ->
                    val isInstalled = installed.any { it.manifest.name == item.name }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.displayName, style = MiuixTheme.textStyles.subtitle)
                                Text(
                                    text = "v${item.version} • ${item.author}",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                            Button(
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
