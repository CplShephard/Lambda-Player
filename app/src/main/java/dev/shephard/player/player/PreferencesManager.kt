package dev.shephard.player.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lambda_player_prefs")

object PrefsKeys {
    val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
    val GAPLESS_ENABLED = booleanPreferencesKey("gapless_enabled")
    val PLAY_WITH_OTHERS = booleanPreferencesKey("play_with_others")
    val TOTAL_LISTENING_MS = longPreferencesKey("total_listening_ms")
    val LANGUAGE = stringPreferencesKey("language")
    val ACCENT_COLOR = intPreferencesKey("accent_color")
    val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
    val PLAYLISTS_JSON = stringPreferencesKey("playlists_json")
    val WALLPAPER_BRIGHTNESS = floatPreferencesKey("wallpaper_brightness")
    val DARK_MODE = booleanPreferencesKey("dark_mode") // Legacy bool, kept for migration.
    val THEME_MODE = intPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
}

object ThemeModePreference {
    const val LIGHT = 0
    const val AUTO = 1
    const val DARK = 2
}

class PreferencesManager(private val context: Context) {

    val crossfadeEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.CROSSFADE_ENABLED] ?: false
    }

    val gaplessEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.GAPLESS_ENABLED] ?: true
    }

    val playWithOthers: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.PLAY_WITH_OTHERS] ?: false
    }

    val totalListeningMs: Flow<Long> = context.dataStore.data.map {
        it[PrefsKeys.TOTAL_LISTENING_MS] ?: 0L
    }

    val language: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.LANGUAGE] ?: "en"
    }

    val accentColor: Flow<Int> = context.dataStore.data.map {
        it[PrefsKeys.ACCENT_COLOR] ?: 0xFF22C55E.toInt()
    }

    val wallpaperUri: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.WALLPAPER_URI] ?: ""
    }

    val playlistsJson: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.PLAYLISTS_JSON] ?: "[]"
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.DARK_MODE] ?: false
    }

    val themeMode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PrefsKeys.THEME_MODE] ?: if (prefs[PrefsKeys.DARK_MODE] == true) {
            ThemeModePreference.DARK
        } else {
            ThemeModePreference.LIGHT
        }
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.DYNAMIC_COLOR] ?: false
    }

    /** 0f..1f — applied as a black scrim opacity on top of the wallpaper. */
    val wallpaperBrightness: Flow<Float> = context.dataStore.data.map {
        it[PrefsKeys.WALLPAPER_BRIGHTNESS] ?: 0.55f
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.CROSSFADE_ENABLED] = enabled }
    }

    suspend fun setGaplessEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.GAPLESS_ENABLED] = enabled }
    }

    suspend fun setPlayWithOthers(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.PLAY_WITH_OTHERS] = enabled }
    }

    suspend fun addListeningTime(deltaMs: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[PrefsKeys.TOTAL_LISTENING_MS] ?: 0L
            prefs[PrefsKeys.TOTAL_LISTENING_MS] = current + deltaMs
        }
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { it[PrefsKeys.LANGUAGE] = code }
    }

    suspend fun setAccentColor(argb: Int) {
        context.dataStore.edit { it[PrefsKeys.ACCENT_COLOR] = argb }
    }

    suspend fun setWallpaperUri(uri: String) {
        context.dataStore.edit { it[PrefsKeys.WALLPAPER_URI] = uri }
    }

    suspend fun setPlaylistsJson(json: String) {
        context.dataStore.edit { it[PrefsKeys.PLAYLISTS_JSON] = json }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit {
            it[PrefsKeys.DARK_MODE] = enabled
            it[PrefsKeys.THEME_MODE] = if (enabled) ThemeModePreference.DARK else ThemeModePreference.LIGHT
        }
    }

    suspend fun setThemeMode(mode: Int) {
        val safeMode = when (mode) {
            ThemeModePreference.AUTO -> ThemeModePreference.AUTO
            ThemeModePreference.DARK -> ThemeModePreference.DARK
            else -> ThemeModePreference.LIGHT
        }
        context.dataStore.edit {
            it[PrefsKeys.THEME_MODE] = safeMode
            it[PrefsKeys.DARK_MODE] = safeMode == ThemeModePreference.DARK
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setWallpaperBrightness(value: Float) {
        context.dataStore.edit { it[PrefsKeys.WALLPAPER_BRIGHTNESS] = value.coerceIn(0f, 1f) }
    }
}
