# 🎵 Lambda Player

<div align="center">

🌐 **English** · [Türkçe](docs/README.tr.md) · [简体中文](docs/README.zh.md) · [Español](docs/README.es.md) · [Русский](docs/README.ru.md) · [ქართული](docs/README.ka.md) · [العربية](docs/README.ar.md)

</div>

Lambda Player is a modern, lightweight, offline-first local music player vibecoded with Kotlin By Different Agent AI Models (Claude Sonex, Kimi etc.). It focuses on fast local playback, advanced queue controls, playlists, rich lyrics, and a highly customizable interface with two complete UI engines: **Miuix** and **Material 3**.

---

## ✨ Features

* **📂 Free & Local-First:** No subscriptions, no ads, and no internet required for playback. Lambda Player scans and organizes the audio files on your device.
* **🎨 Two UI Engines:** Switch between Miuix and Material 3 at any time from *Settings → Theme → UI Style*.
* **🌈 Flexible Theming:** Nine palette styles, Material 3 (2021) or Expressive (2025) color specs, a custom seed color, Miuix Custom Colors generated from your seed, and an optional green accent.
* **🪟 Liquid Glass & Blur:** Real background blur for the dock, cards and sheets. On Android 13+, the dock becomes a Liquid Glass surface. A floating pill-shaped dock is available as an alternative to the standard navigation bar.
* **🔀 Advanced Queue Management:** Add tracks to the queue, reorder them with drag-and-drop, play next, remove items, and shuffle or remix the queue.
* **🎧 High-Quality Playback:** Powered by Media3 ExoPlayer with crossfade, gapless playback (the next track is prepared ahead of time), repeat, shuffle, and **volume normalization** (Low, Normal or High targets).
* **📝 Rich Lyrics:**
  * Automatic lyrics search: Off, Embedded first, Best match, or Online first.
  * Word-by-word karaoke lyrics.
  * Import lyrics from files and export them as LRC, keeping word-level timing.
  * Per-track sync offset, auto-scroll, background blur strength, and a fullscreen lyrics view.
  * Online sources include LRCLIB, QQ Music and lyrics.ovh.
* **🖼️ Playlists with Covers:** Give each playlist its own cover image.
* **👆 Predictive Back:** Choose None, AOSP, MIUIX, Classic or Scale back animations.
* **🌍 Localized:** English, Türkçe, 简体中文, Español, Русский, ქართული and العربية.

---

## 📥 Installation

1. Download the latest APK from the [Releases](https://github.com/CplShephard/Lambda-Player/releases) page.
2. Install it on your Android device (Android 8.0 or newer).
3. Grant access to your audio files when prompted.

Lambda Player checks GitHub for new releases and will let you know when an update is available.

---

## 🛠️ Built With

* **Language:** 100% [Kotlin](https://kotlinlang.org/)
* **UI Frameworks:** [Miuix](https://github.com/compose-miuix-ui/miuix) (`top.yukonga.miuix.kmp`) and Material 3 Compose
* **Audio Engine:** Media3 ExoPlayer
* **Storage:** Android MediaStore + DataStore Preferences
* **Networking:** OkHttp

---

## 🤝 Contributing

Issues and pull requests are welcome. Please keep changes focused and well-described.

---

## 📄 License

This project is licensed under the GNU General Public License v3.0. See the `LICENSE` file for details.

