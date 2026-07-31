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
import dev.shephard.player.ui.miuix.ExperimentalMaterial3Api
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.rememberDrawerDismiss
import dev.shephard.player.ui.miuix.Text
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import dev.shephard.player.ui.miuix.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.UpdateChecker
import dev.shephard.player.player.GithubReleaseInfo
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.navigation.MainContainer
import dev.shephard.player.ui.theme.LambdaPlayerTheme

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
            val accent by prefs.accentColor.collectAsState(initial = 0xFF22C55E.toInt())
            val themeMode by prefs.themeMode.collectAsState(initial = dev.shephard.player.player.ThemeModePreference.DARK)
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
                // Status bar ikon rengini (saat, wifi, batarya simgeleri) uygulamanın GERÇEK
                // arkaplan koyuluğuna göre ayarla. Önceden hiç ayarlanmıyordu: enableEdgeToEdge()
                // sadece sistem UI moduna bakıyordu, bu da uygulamanın kendi tema tercihiyle
                // (themeMode = DARK/LIGHT/SYSTEM) senkron değildi — koyu temada ikonlar koyu
                // (neredeyse görünmez), açık temada bazen ters kalabiliyordu.
                val isAppDark = MiuixAppTheme.colorScheme.background.luminance() < 0.5f
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isAppDark
                    }
                }
                CompositionLocalProvider(LocalBlurEnabled provides blurEnabled) {
                var availableRelease by remember { mutableStateOf<GithubReleaseInfo?>(null) }
                LaunchedEffect(Unit) {
                    availableRelease = UpdateChecker.checkLatestRelease()
                }

                MainContainer(initialAudioUri = initialAudioUri)

                val release = availableRelease
                if (release != null) {
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
