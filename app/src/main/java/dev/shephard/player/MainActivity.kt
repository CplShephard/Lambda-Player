package dev.shephard.player

import android.content.pm.ActivityInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.UpdateChecker
import dev.shephard.player.player.GithubReleaseInfo
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.navigation.MainContainer
import dev.shephard.player.ui.theme.LambdaPlayerTheme

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
            val accent by prefs.accentColor.collectAsState(initial = 0xFF22C55E.toInt())
            val themeMode by prefs.themeMode.collectAsState(initial = dev.shephard.player.player.ThemeModePreference.LIGHT)
            val dynamicColor by prefs.dynamicColor.collectAsState(initial = false)
            val languageCode by prefs.language.collectAsState(initial = "en")
            val strings = remember(languageCode) { stringsFor(languageCode) }
            val initialAudioUri = externalAudioUriState.value
            LambdaPlayerTheme(
                accentArgb = accent,
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                var availableRelease by remember { mutableStateOf<GithubReleaseInfo?>(null) }
                LaunchedEffect(Unit) {
                    availableRelease = UpdateChecker.checkLatestRelease()
                }

                MainContainer(initialAudioUri = initialAudioUri)

                val release = availableRelease
                if (release != null) {
                    AlertDialog(
                        onDismissRequest = { availableRelease = null },
                        title = { Text(strings.updateAvailable) },
                        text = { Text("${strings.updateAvailableMessage} (${release.tagName})") },
                        confirmButton = {
                            TextButton(onClick = {
                                UpdateChecker.openRelease(this@MainActivity, release.htmlUrl)
                                availableRelease = null
                            }) { Text(strings.update) }
                        },
                        dismissButton = {
                            TextButton(onClick = { availableRelease = null }) { Text(strings.later) }
                        }
                    )
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
