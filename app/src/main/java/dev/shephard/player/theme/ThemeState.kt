package dev.shephard.player.theme

data class ThemeState(
    val isLoaded: Boolean = true,
    val useMiuix: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = false,
    val useMiuixMonet: Boolean = false,
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Int = 0xFF22C55E.toInt(),
    val useBlur: Boolean = false,
)
