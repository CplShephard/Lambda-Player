// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.screens

/** One third-party source (code, design or library) listed in About -> Licenses. */
data class LicenseEntry(
    val name: String,
    val author: String,
    val license: String,
    val url: String,
)

/**
 * Projects whose code or design Lambda Player builds on, followed by the libraries it links against.
 * Shared by the Miuix and Material 3 About screens; add a line here and both dialogs pick it up.
 */
val AppLicenses: List<LicenseEntry> = listOf(
    // Code and design taken from other projects
    LicenseEntry("InstallerX Revived", "wxxsfxyzm, iamr0s and contributors", "GPL-3.0", "https://github.com/wxxsfxyzm/InstallerX-Revived"),
    LicenseEntry("KernelSU", "tiann (weishu) and contributors", "GPL-3.0", "https://github.com/tiann/KernelSU"),
    LicenseEntry("LineageOS Twelve", "The LineageOS Project", "Apache-2.0", "https://github.com/LineageOS/android_packages_apps_Twelve"),
    LicenseEntry("AndroidLiquidGlass", "Kyant0", "Apache-2.0", "https://github.com/Kyant0/AndroidLiquidGlass"),
    LicenseEntry("PixelPlayer", "theovilardo", "MIT (code before 2026-05-12)", "https://github.com/theovilardo/PixelPlayer"),
    LicenseEntry("Miuix example app (OS3 background effect, liquid glass bottom bar, glass effects)", "compose-miuix-ui contributors", "Apache-2.0", "https://github.com/compose-miuix-ui/miuix"),
    // Online lyrics sources queried by the app
    LicenseEntry("LRCLIB", "lrclib.net", "Online lyrics service", "https://lrclib.net"),
    LicenseEntry("QQ Music lyrics API", "Tencent", "Online lyrics service", "https://y.qq.com"),
    LicenseEntry("lyrics.ovh", "lyrics.ovh", "Online lyrics service", "https://lyrics.ovh"),
    // Libraries
    LicenseEntry("Miuix (ui, preference, icons, blur, shader, squircle, nav)", "compose-miuix-ui contributors", "Apache-2.0", "https://github.com/compose-miuix-ui/miuix"),
    LicenseEntry("AndroidX (Core, Lifecycle, Activity, Navigation Event, Compose, Material 3, Material Icons, DataStore, Palette)", "Google", "Apache-2.0", "https://developer.android.com/jetpack/androidx"),
    LicenseEntry("Media3 (ExoPlayer, Session, Common)", "Google", "Apache-2.0", "https://github.com/androidx/media"),
    LicenseEntry("MaterialKolor", "jordond", "MIT", "https://github.com/jordond/MaterialKolor"),
    LicenseEntry("AppIconLoader", "zhanghai", "Apache-2.0", "https://github.com/zhanghai/AppIconLoader"),
    LicenseEntry("Reorderable", "Calvin-LL", "Apache-2.0", "https://github.com/Calvin-LL/Reorderable"),
    LicenseEntry("Coil (Compose, GIF)", "coil-kt", "Apache-2.0", "https://github.com/coil-kt/coil"),
    LicenseEntry("kotlinx.coroutines", "JetBrains", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    LicenseEntry("Guava", "Google", "Apache-2.0", "https://github.com/google/guava"),
    LicenseEntry("JAudiotagger", "ijabz", "LGPL-2.1", "https://github.com/ijabz/JAudiotagger"),
    LicenseEntry("Kotlin Standard Library", "JetBrains", "Apache-2.0", "https://github.com/JetBrains/kotlin"),
    // Pulled in transitively by the libraries above
    LicenseEntry("Compose Multiplatform (via Miuix)", "JetBrains", "Apache-2.0", "https://github.com/JetBrains/compose-multiplatform"),
    LicenseEntry("Shapes (via Miuix)", "Kyant0", "Apache-2.0", "https://github.com/Kyant0/Shapes"),
    LicenseEntry("Material Color Utilities (via MaterialKolor)", "Google", "Apache-2.0", "https://github.com/material-foundation/material-color-utilities"),
    LicenseEntry("OkHttp & Okio (via Coil)", "Square", "Apache-2.0", "https://github.com/square/okhttp"),
)
