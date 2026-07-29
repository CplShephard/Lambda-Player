package dev.shephard.player

import android.content.pm.ActivityInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import dev.shephard.player.ui.miuix.ExperimentalMiuixApi
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.ModalBottomSheet
import dev.shephard.player.ui.miuix.Text
import androidx.activity.enableEdgeToEdge
import dev.shephard.player.ui.miuix.TextButton
import dev.shephard.player.ui.miuix.rememberModalBottomSheetState
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
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.UpdateChecker
import dev.shephard.player.player.GithubReleaseInfo
import dev.shephard.player.ui.components.MiuixSheetDefaults
import dev.shephard.player.ui.components.MiuixSheetHandle
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.navigation.MainContainer
import dev.shephard.player.ui.theme.LambdaPlayerTheme

@OptIn(ExperimentalMiuixApi::class)
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
            val cardAlpha by prefs.cardAlpha.collectAsState(initial = 0.85f)
            val blurEnabled by prefs.liquidGlassEnabled.collectAsState(initial = false)
            val languageCode by prefs.language.collectAsState(initial = "en")
            val strings = remember(languageCode) { stringsFor(languageCode) }
            val initialAudioUri = externalAudioUriState.value
            LambdaPlayerTheme(
                accentArgb = accent,
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                cardAlpha = cardAlpha
            ) {
                CompositionLocalProvider(LocalBlurEnabled provides blurEnabled) {
                var availableRelease by remember { mutableStateOf<GithubReleaseInfo?>(null) }
                LaunchedEffect(Unit) {
                    availableRelease = UpdateChecker.checkLatestRelease()
                }

                MainContainer(initialAudioUri = initialAudioUri)

                val release = availableRelease
                if (release != null) {
                    val updateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                    ModalBottomSheet(
                        onDismissRequest = { availableRelease = null },
                        sheetState = updateSheetState,
                        shape = MiuixSheetDefaults.Shape,
                        containerColor = MiuixSheetDefaults.containerColor(blurEnabled),
                        contentColor = MiuixAppTheme.colorScheme.onSurface,
                        dragHandle = { MiuixSheetHandle(blurEnabled) }
                    ) {
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
                                TextButton(onClick = { availableRelease = null }) { Text(strings.later) }
                                TextButton(onClick = {
                                    UpdateChecker.openRelease(this@MainActivity, release.htmlUrl)
                                    availableRelease = null
                                }) { Text(strings.update) }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
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
