package dev.shephard.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.shephard.player.player.GithubReleaseInfo
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.UpdateChecker
import dev.shephard.player.theme.ThemeState
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.rememberDrawerDismiss
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.miuix.ExperimentalMaterial3Api
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import dev.shephard.player.ui.miuix.TextButton
import dev.shephard.player.ui.navigation.MainContainer
import dev.shephard.player.ui.theme.PlayerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val externalAudioUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        externalAudioUriState.value = audioUriFromIntent(intent)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { PreferencesManager(context) }

            // FIX: use the persisted value from the very first frame (no true->false flip)
            val initialUseMiuix = remember { runBlocking { prefs.useMiuix.first() } }
            val useMiuix by prefs.useMiuix.collectAsState(initial = initialUseMiuix)
            val themeMode by prefs.themeModeEnum.collectAsState(initial = dev.shephard.player.theme.ThemeMode.SYSTEM)
            val paletteStyle by prefs.paletteStyle.collectAsState(initial = dev.shephard.player.theme.PaletteStyle.TonalSpot)
            val colorSpec by prefs.colorSpec.collectAsState(initial = dev.shephard.player.theme.ThemeColorSpec.SPEC_2025)
            val dynamicColor by prefs.dynamicColor.collectAsState(initial = false)
            val useMiuixMonet by prefs.useMiuixMonet.collectAsState(initial = false)
            val seedColor by prefs.seedColor.collectAsState(initial = 0xFF22C55E.toInt())
            // Read the persisted blur toggle synchronously so glass surfaces (dock,
            // top bars) don't render in their solid fallback state for a second on
            // every launch while DataStore warms up.
            val initialBlurEnabled = remember { runBlocking { prefs.liquidGlassEnabled.first() } }
            val blurEnabled by prefs.liquidGlassEnabled.collectAsState(initial = initialBlurEnabled)
            val appleFloatingBar by prefs.useAppleFloatingBar.collectAsState(initial = false)
            val languageCode by prefs.language.collectAsState(initial = "en")
            val strings = remember(languageCode) { stringsFor(languageCode) }

            // Material 3 always uses the Monet (wallpaper-dynamic) palette — the
            // M3 UI has no "Miuix custom colors" switch. The setting is only
            // honoured by the Miuix engine.
            val themeState = ThemeState(
                useMiuix = useMiuix,
                themeMode = themeMode,
                paletteStyle = paletteStyle,
                colorSpec = colorSpec,
                useDynamicColor = dynamicColor,
                useMiuixMonet = if (useMiuix) useMiuixMonet else true,
                seedColor = seedColor,
                useBlur = blurEnabled,
                useAppleFloatingBar = appleFloatingBar,
            )

            PlayerTheme(themeState) {
                CompositionLocalProvider(LocalBlurEnabled provides blurEnabled) {
                    var availableRelease by remember { mutableStateOf<GithubReleaseInfo?>(null) }
                    LaunchedEffect(Unit) {
                        availableRelease = UpdateChecker.checkLatestRelease()
                    }

                    MainContainer(initialAudioUri = externalAudioUriState.value)

                    val release = availableRelease
                    if (release != null) {
                        if (useMiuix) {
                            MiuixDrawer(
                                onDismissRequest = { availableRelease = null },
                            ) {
                                val dismissDrawer = rememberDrawerDismiss()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        text = strings.updateAvailable,
                                        style = MiuixAppTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "${strings.updateAvailableMessage} (${release.tagName})",
                                        color = MiuixAppTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(22.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { dismissDrawer() }) { Text(strings.later) }
                                        TextButton(onClick = {
                                            UpdateChecker.openRelease(this@MainActivity, release.htmlUrl)
                                            dismissDrawer()
                                        }) { Text(strings.update) }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        } else {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { availableRelease = null },
                                title = { androidx.compose.material3.Text(strings.updateAvailable) },
                                text = {
                                    androidx.compose.material3.Text(
                                        "${strings.updateAvailableMessage} (${release.tagName})"
                                    )
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            UpdateChecker.openRelease(this@MainActivity, release.htmlUrl)
                                            availableRelease = null
                                        }
                                    ) { androidx.compose.material3.Text(strings.update) }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { availableRelease = null }
                                    ) { androidx.compose.material3.Text(strings.later) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalAudioUriState.value = audioUriFromIntent(intent)
    }

    private fun audioUriFromIntent(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        return intent.data
    }
}
