package dev.shephard.player.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.shephard.player.theme.PaletteStyle
import dev.shephard.player.theme.ThemeColorSpec
import dev.shephard.player.theme.ThemeMode
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
    val CARD_ALPHA = floatPreferencesKey("card_alpha")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val THEME_MODE = intPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val PLAYLISTS_LAYOUT = intPreferencesKey("playlists_layout")
    val MUSICS_LAYOUT = intPreferencesKey("musics_layout")
    val LIKED_SONG_IDS = stringPreferencesKey("liked_song_ids")
    val TRACK_OVERRIDES_JSON = stringPreferencesKey("track-overrides_json")
    val LYRICS_JSON = stringPreferencesKey("lyrics_json")
    val LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
    val LISTEN_STATS_EVENTS_JSON = stringPreferencesKey("listen_stats_events_json")

    val USE_MIUIX = booleanPreferencesKey("use_miuix")
    val PALETTE_STYLE = stringPreferencesKey("palette_style")
    val COLOR_SPEC = stringPreferencesKey("color_spec")
    val USE_MIUIX_MONET = booleanPreferencesKey("use_miuix_monet")
    val USE_APPLE_FLOATING_BAR = booleanPreferencesKey("use_apple_floating_bar")
    val LAST_MAIN_PAGE = intPreferencesKey("last_main_page")
    val NOW_PLAYING_TRANSLATION = booleanPreferencesKey("now_playing_translation")
    val LYRIC_BLUR_EFFECT = booleanPreferencesKey("lyric_blur_effect")
    val NOW_PLAYING_SHOW_VOLUME_BAR = booleanPreferencesKey("now_playing_show_volume_bar")
    val NOWPLAYING_BACKGROUND_EFFECT = booleanPreferencesKey("nowplaying_background_effect")
}

object ThemeModePreference {
    const val LIGHT = 0
    const val AUTO = 1
    const val DARK = 2
}

object LayoutMode {
    const val LIST = 0
    const val GRID = 1
}

fun ThemeMode.toPreferenceInt(): Int = when (this) {
    ThemeMode.LIGHT -> ThemeModePreference.LIGHT
    ThemeMode.SYSTEM -> ThemeModePreference.AUTO
    ThemeMode.DARK -> ThemeModePreference.DARK
}

fun Int.toThemeMode(): ThemeMode = when (this) {
    ThemeModePreference.LIGHT -> ThemeMode.LIGHT
    ThemeModePreference.AUTO -> ThemeMode.SYSTEM
    else -> ThemeMode.DARK
}

class PreferencesManager(private val context: Context) {

    companion object {
        var cachedWallpaperBrightness: Float = 0.55f
    }

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

    val useMiuix: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.USE_MIUIX] ?: true
    }

    val paletteStyle: Flow<PaletteStyle> = context.dataStore.data.map {
        PaletteStyle.fromValueOrDefault(it[PrefsKeys.PALETTE_STYLE] ?: "")
    }

    val colorSpec: Flow<ThemeColorSpec> = context.dataStore.data.map {
        ThemeColorSpec.fromValueOrDefault(it[PrefsKeys.COLOR_SPEC] ?: "")
    }

    val useMiuixMonet: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.USE_MIUIX_MONET] ?: false
    }

    val useAppleFloatingBar: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.USE_APPLE_FLOATING_BAR] ?: false
    }

    val lastMainPage: Flow<Int> = context.dataStore.data.map {
        it[PrefsKeys.LAST_MAIN_PAGE] ?: 0
    }

    val nowPlayingTranslation: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.NOW_PLAYING_TRANSLATION] ?: false
    }

    val lyricBlurEffect: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.LYRIC_BLUR_EFFECT] ?: true
    }

    val nowPlayingShowVolumeBar: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.NOW_PLAYING_SHOW_VOLUME_BAR] ?: false
    }

    val nowplayingBackgroundEffect: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.NOWPLAYING_BACKGROUND_EFFECT] ?: true
    }

    suspend fun setLastMainPage(index: Int) {
        context.dataStore.edit { it[PrefsKeys.LAST_MAIN_PAGE] = index }
    }

    suspend fun setNowPlayingTranslation(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.NOW_PLAYING_TRANSLATION] = enabled }
    }

    suspend fun setLyricBlurEffect(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.LYRIC_BLUR_EFFECT] = enabled }
    }

    suspend fun setNowPlayingShowVolumeBar(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.NOW_PLAYING_SHOW_VOLUME_BAR] = enabled }
    }

    suspend fun setNowplayingBackgroundEffect(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.NOWPLAYING_BACKGROUND_EFFECT] = enabled }
    }

    val seedColor: Flow<Int> = accentColor

    val themeModeEnum: Flow<ThemeMode> = themeMode.map { it.toThemeMode() }

    val liquidGlassEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.LIQUID_GLASS_ENABLED] ?: false
    }

    val playlistsLayout: Flow<Int> = context.dataStore.data.map {
        it[PrefsKeys.PLAYLISTS_LAYOUT] ?: LayoutMode.LIST
    }

    val musicsLayout: Flow<Int> = context.dataStore.data.map {
        it[PrefsKeys.MUSICS_LAYOUT] ?: LayoutMode.LIST
    }

    val likedSongIds: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.LIKED_SONG_IDS] ?: "[]"
    }

    val trackOverridesJson: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.TRACK_OVERRIDES_JSON] ?: "{}"
    }

    val lyricsJson: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.LYRICS_JSON] ?: "{}"
    }

    val wallpaperBrightness: Flow<Float> = context.dataStore.data.map {
        val v = it[PrefsKeys.WALLPAPER_BRIGHTNESS] ?: 0.55f
        cachedWallpaperBrightness = v
        v
    }

    val cardAlpha: Flow<Float> = context.dataStore.data.map {
        it[PrefsKeys.CARD_ALPHA] ?: 0.85f
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

    suspend fun setUseMiuix(useMiuix: Boolean) {
        context.dataStore.edit { it[PrefsKeys.USE_MIUIX] = useMiuix }
    }

    suspend fun setPaletteStyle(style: PaletteStyle) {
        context.dataStore.edit { it[PrefsKeys.PALETTE_STYLE] = style.name }
    }

    suspend fun setColorSpec(spec: ThemeColorSpec) {
        context.dataStore.edit { it[PrefsKeys.COLOR_SPEC] = spec.name }
    }

    suspend fun setUseMiuixMonet(use: Boolean) {
        context.dataStore.edit { it[PrefsKeys.USE_MIUIX_MONET] = use }
    }

    suspend fun setUseAppleFloatingBar(use: Boolean) {
        context.dataStore.edit { it[PrefsKeys.USE_APPLE_FLOATING_BAR] = use }
    }

    suspend fun setSeedColor(argb: Int) {
        context.dataStore.edit { it[PrefsKeys.ACCENT_COLOR] = argb }
    }

    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.LIQUID_GLASS_ENABLED] = enabled }
    }

    suspend fun setPlaylistsLayout(mode: Int) {
        context.dataStore.edit { it[PrefsKeys.PLAYLISTS_LAYOUT] = mode }
    }

    suspend fun setMusicsLayout(mode: Int) {
        context.dataStore.edit { it[PrefsKeys.MUSICS_LAYOUT] = mode }
    }

    suspend fun setLikedSongIds(json: String) {
        context.dataStore.edit { it[PrefsKeys.LIKED_SONG_IDS] = json }
    }

    suspend fun setTrackOverridesJson(json: String) {
        context.dataStore.edit { it[PrefsKeys.TRACK_OVERRIDES_JSON] = json }
    }

    suspend fun setLyricsJson(json: String) {
        context.dataStore.edit { it[PrefsKeys.LYRICS_JSON] = json }
    }

    suspend fun setWallpaperBrightness(value: Float) {
        cachedWallpaperBrightness = value.coerceIn(0f, 1f)
        context.dataStore.edit { it[PrefsKeys.WALLPAPER_BRIGHTNESS] = value.coerceIn(0f, 1f) }
    }

    suspend fun setCardAlpha(value: Float) {
        context.dataStore.edit { it[PrefsKeys.CARD_ALPHA] = value.coerceIn(0f, 1f) }
    }

    val listenStatsEventsJson: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.LISTEN_STATS_EVENTS_JSON] ?: "[]"
    }

    suspend fun setListenStatsEventsJson(json: String) {
        context.dataStore.edit { it[PrefsKeys.LISTEN_STATS_EVENTS_JSON] = json }
    }
}
