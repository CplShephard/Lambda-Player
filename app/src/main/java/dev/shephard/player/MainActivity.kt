package dev.shephard.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.navigation.MainContainer
import dev.shephard.player.ui.theme.LambdaPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { PreferencesManager(context) }
            val accent by prefs.accentColor.collectAsState(initial = 0xFF22C55E.toInt())
            val themeMode by prefs.themeMode.collectAsState(initial = dev.shephard.player.player.ThemeModePreference.LIGHT)
            val dynamicColor by prefs.dynamicColor.collectAsState(initial = false)
            LambdaPlayerTheme(
                accentArgb = accent,
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                MainContainer()
            }
        }
    }
}
