# 🎵 Lambda Player

<div align="center">

🌐 [English](../README.md) · [Türkçe](README.tr.md) · **简体中文** · [Español](README.es.md) · [Русский](README.ru.md) · [ქართული](README.ka.md) · [العربية](README.ar.md)

</div>

**Lambda Player** 是一款面向 Android 的现代、轻量、离线优先的本地音乐播放器。它专注于快速的本地播放、高级队列控制、播放列表、丰富的歌词体验，以及拥有两套完整界面引擎（**Miuix** 与 **Material 3**）的高度可定制界面。

---

## ✨ 功能特性

* **📂 免费且本地优先：** 无需订阅、无广告，播放时无需网络。Lambda Player 会扫描并整理设备上的音频文件。
* **🎨 两套界面引擎：** 随时在 *设置 → 主题 → UI 风格* 中切换 Miuix 与 Material 3。
* **🌈 灵活的主题：** 九种调色板风格、Material 3 (2021) 或 Expressive (2025) 配色规范、自定义种子颜色、基于种子色生成的 Miuix 自定义配色，以及可选的绿色强调色。
* **🪟 液态玻璃与模糊：** 底栏、卡片和弹出面板均支持真实的背景模糊。在 Android 13+ 上，底栏会变为液态玻璃效果。也可以选择悬浮胶囊式底栏来替代标准导航栏。
* **🔀 高级队列管理：** 添加歌曲到队列，拖拽排序，插播，移除项目，并可随机播放或重新编排队列。
* **🎧 高品质播放：** 基于 Media3 ExoPlayer，支持淡入淡出、无缝播放（提前加载下一首）、循环、随机，以及**音量标准化**（低、标准或高三档目标响度）。
* **📝 丰富的歌词：**
  * 自动歌词搜索：关闭、优先内嵌、最佳匹配或优先在线。
  * 逐字卡拉 OK 歌词。
  * 支持从文件导入歌词，并可导出为 LRC，保留逐字时间轴。
  * 每首歌可单独设置时间偏移，支持自动滚动、背景模糊强度调节，以及全屏歌词视图。
  * 在线歌词来源包括 LRCLIB、QQ 音乐和 lyrics.ovh。
* **🖼️ 带封面的播放列表：** 为每个播放列表设置独立的封面图片。
* **👆 预见性返回：** 可选择无、AOSP、MIUIX、经典或缩放等返回动画。
* **🌍 多语言：** English、Türkçe、简体中文、Español、Русский、ქართული 与 العربية。

---

## 📥 安装

1. 从 [Releases](https://github.com/CplShephard/Lambda-Player/releases) 页面下载最新的 APK。
2. 安装到你的 Android 设备上（需要 Android 8.0 或更高版本）。
3. 出现提示时，授予访问音频文件的权限。

Lambda Player 会通过 GitHub 检查新版本，有更新时会提醒你。

---

## 🛠️ 技术栈

* **语言：** 100% [Kotlin](https://kotlinlang.org/)
* **UI 框架：** [Miuix](https://github.com/compose-miuix-ui/miuix)（`top.yukonga.miuix.kmp`）与 Material 3 Compose
* **音频引擎：** Media3 ExoPlayer
* **存储：** Android MediaStore + DataStore Preferences
* **网络：** OkHttp

---

## 🤝 参与贡献

欢迎提交 Issue 和 Pull Request。请保持改动聚焦，并附上清晰的说明。

---

## 🎁 彩蛋

在“关于”页面中，快速连续点击应用图标 5 次，会有一个小惊喜等着你。

---

## 📄 许可证

本项目基于 GNU 通用公共许可证第 3 版（GPL-3.0）授权。详见 `LICENSE` 文件。

