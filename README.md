<div align="center">

# [MeiloX](https://github.com/NEORUAA/MeiloX)

<img src="./screenshot/logo.png" width="120px" style="border-radius:12px"/>

### 一个基于 Mei 改造的高仿 Apple Music 风格网易云音乐 Android 客户端

![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![License](https://img.shields.io/badge/License-GPLv3-orange)
![Status](https://img.shields.io/badge/Development-In%20Progress-yellow)

</div>

---

## 项目简介

MeiloX 是一个基于 [Mei](https://github.com/ljyh223/Mei) 改造的第三方网易云音乐客户端，使用 **Jetpack Compose** 构建，尝试把 iOS 27 的层次感、液态玻璃和沉浸式播放器体验带到 Android 上。

项目重点放在三个方向：

- 用 iOS 风格的页面结构重新组织网易云音乐内容；
- 用 Liquid Glass、动态背景和专辑封面让播放过程更有沉浸感；
- 保留网易云音乐的搜索、歌单、歌词和个人资料等使用习惯。

> MeiloX 是非官方的个人开源项目，不隶属于网易云音乐或 Apple；项目本身不提供音乐资源，音乐内容及相关版权归其权利人所有。

## 功能概览

- 网易云音乐 Cookie 登录与账号信息展示；
- 搜索歌曲、专辑、歌手和歌单；
- 播放队列、随机播放、循环播放和定时停止；
- 播放历史、喜欢的音乐、歌单管理和本地收藏；
- 专辑详情、歌手主页、每日推荐及 FM 播放；
- 逐字歌词与多语言歌词展示；
- Apple Music 风格全屏播放器、迷你播放器和歌词浮窗；
- 浅色/深色主题、动态专辑背景和 Liquid Glass 交互效果。

功能仍在持续调整中，实际可用范围会随版本和网易云音乐接口状态变化。

---

## 本分支修改记录

本分支在原版基础上做了以下定制修改，均为个人使用向调整，未改动原项目的功能逻辑与数据结构。

### 1. 全局字体更换为三星 One UI 字体（Samsung Sharp Sans）

- **新增字体资源**：`app/src/main/res/font/samsung_sharp_sans_regular.ttf`、`samsung_sharp_sans_bold.ttf`，来源为三星官方仓库 [SamsungInternet/OneUI-Web](https://github.com/SamsungInternet/OneUI-Web)；
- **新增共享字体定义**：`LyricFont.kt`（顶层 `LyricFontFamily`，含 Regular + Bold 两个字重），所有歌词/歌名位置统一引用；
- **全局主题字体**：`Theme.kt` 中 `SfProFamily` 的定义由 `sf_pro.ttf` 改为三星字体，所有 `MaterialTheme.typography.*` 样式全局生效；
- **iOS 风格组件字体**：`GlassTokens.kt` 中 `IosTypography.fontFamily` 同步改为三星字体；
- **歌词界面**：全屏歌词（`LyricScreen.kt`）、悬浮画中画歌词（`FloatingLyricsPip.kt`）、Liquid 歌词特效（`LiquidLyricsViews.kt`，含列表 / EVA / TEXT PV / Skyline 四种视图）全部使用三星字体；
- **歌名文字**：播放页歌名与歌手副标题（`Title.kt`）、歌单/歌曲列表行歌名（`TrackItem.kt`）使用三星字体；
- **图标例外**：`SfSymbol.kt` 的 SF Symbols **图标字形**保留 `sf_pro` 不变——图标字体替换会导致图标显示为方块；
- **中文渲染**：三星字体仅含拉丁字形，中文等非拉丁字符由系统按 fallback 链自动选择字体，这与三星 One UI 在中文设备上的真实渲染行为一致。

### 2. 性能优化

- **构建改为 Release 模式**：开启 R8 代码压缩、资源收缩与 Baseline Profile 编译，APK 体积从 Debug 版约 64MB 降至约 20MB，运行流畅度与上游发布版一致；
- **Release 签名**：`app/build.gradle.kts` 中为 release 构建启用 debug 签名（`signingConfig = signingConfigs.getByName("debug")`），产物可直接安装；
- **歌词样式缓存**：`LyricScreen.kt` / `LiquidLyricsViews.kt` 中原本每次重组都会新建的 `LocalTextStyle.current.copy(...)` / `MaterialTheme.typography.*.copy(...)` 改为 `remember` 缓存，仅在字号/字重等参数变化时重建，减少逐帧对象分配。

### 修改文件清单

| 文件 | 修改内容 |
|---|---|
| `app/src/main/java/com/ljyh/mei/ui/theme/Theme.kt` | 全局默认字体改为 Samsung Sharp Sans |
| `app/src/main/java/com/ljyh/mei/ui/glass/GlassTokens.kt` | `IosTypography.fontFamily` 改为三星字体 |
| `app/src/main/java/com/ljyh/mei/ui/component/player/component/LyricFont.kt` | 新增：共享字体定义 `LyricFontFamily` |
| `app/src/main/java/com/ljyh/mei/ui/component/player/component/LyricScreen.kt` | 全屏歌词三行样式使用三星字体 + remember 缓存 |
| `app/src/main/java/com/ljyh/mei/ui/component/player/FloatingLyricsPip.kt` | 悬浮画中画歌词使用三星字体 |
| `app/src/main/java/com/ljyh/mei/ui/component/player/component/LiquidLyricsViews.kt` | 四种 Liquid 歌词视图使用三星字体 + 样式缓存 |
| `app/src/main/java/com/ljyh/mei/ui/component/player/component/applemusic/Title.kt` | 播放页歌名/副标题使用三星字体 |
| `app/src/main/java/com/ljyh/mei/ui/component/item/TrackItem.kt` | 歌单/歌曲列表行歌名使用三星字体 |
| `app/src/main/res/font/samsung_sharp_sans_regular.ttf` | 新增：三星 Regular 字体 |
| `app/src/main/res/font/samsung_sharp_sans_bold.ttf` | 新增：三星 Bold 字体 |
| `app/build.gradle.kts` | Release 构建启用 debug 签名以便直接安装 |

## 歌词共享

已接入 [Lyricon Provider](https://tomakino.github.io/lyricon/zh-cn/developer/provider/) 和 [SuperLyricApi](https://github.com/HChenX/SuperLyricApi) SDK，用于共享歌词、逐字时间、翻译及播放状态。

「设置 → 歌词设置 → 歌词共享」中的「启用歌词共享」默认开启。

## 开源致谢

MeiloX 的界面、歌词和底层能力受以下项目启发或直接使用其开源组件，感谢所有贡献者：

- [Mei](https://github.com/ljyh223/Mei)：上游 Android 项目与基础能力；
- [MeloX](https://github.com/youshen2/MeloX)：iOS 版参考实现与产品方向；
- [amll-ttml-db](https://github.com/Steve-xmh/amll-ttml-db)：高质量逐字歌词数据；
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui)：歌词模型与渲染组件；
- [Lyricon](https://github.com/tomakino/lyricon)：词幕 Provider SDK；
- [SuperLyricApi](https://github.com/HChenX/SuperLyricApi)：歌词共享 SDK；
- [AndroidLiquidGlass / Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)：Compose Multiplatform 流体玻璃效果。

## 许可证与第三方声明

MeiloX 主体代码以 [GNU General Public License v3](LICENSE) 发布。MeiloX 是 Mei 的衍生项目，相关上游代码和参考实现仍受其原始版权与许可证约束；第三方库、模型、脚本、字体和视觉素材不因 MeiloX 使用 GPLv3 就自动转为 GPLv3。

重点声明如下：

- [Mei](https://github.com/ljyh223/Mei) 的上游代码遵循 Apache License 2.0；
- [MeloX](https://github.com/youshen2/MeloX) 的相关参考代码遵循其项目的 GPLv3 条款；
- [AndroidLiquidGlass / Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) 相关依赖遵循 Apache License 2.0；
- [amll-ttml-db](https://github.com/Steve-xmh/amll-ttml-db) 与 [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui) 遵循各自仓库公布的许可证和署名要求；
- BeatNet 模型、音频指纹运行时、ONNX Runtime、PV Tool 衍生内容及其它随项目分发的组件，遵循其各自的许可证、版权声明和再分发限制。

完整的来源、版权归属、许可证链接和特殊再分发要求请阅读 [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES)。其中 `res/font/sf_pro.ttf` 仅作为个人项目资源使用；请勿从构建产物中单独提取或再分发，公开发布构建版本前请重新核对 Apple 的字体许可条款。

## 软件界面预览

以下预览来自 [`screenshot/2026-08-22`](./screenshot/2026-08-22/)，同时展示浅色、深色和播放器界面。

### 浅色界面

<table>
  <tr>
    <td><img src="./screenshot/2026-08-22/light_Screenshot_2026-08-22-09-47-44-801_com.neoruaa.meilox.jpg" width="200" alt="Light mode 01"></td>
    <td><img src="./screenshot/2026-08-22/light_Screenshot_2026-08-22-10-31-11-383_com.neoruaa.meilox.jpg" width="200" alt="Light mode 07"></td>
    <td><img src="./screenshot/2026-08-22/light_Screenshot_2026-08-22-10-06-44-461_com.neoruaa.meilox.jpg" width="200" alt="Light mode 04"></td>
    <td><img src="./screenshot/2026-08-22/light_Screenshot_2026-08-22-10-05-43-651_com.neoruaa.meilox.jpg" width="200" alt="Light mode 02"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2026-08-22/light_Screenshot_2026-08-22-10-13-55-779_com.neoruaa.meilox.jpg" width="200" alt="Light mode 05"></td>
    <td><img src="./screenshot/2026-08-22/light_Screenshot_2026-08-22-10-14-22-986_com.neoruaa.meilox.jpg" width="200" alt="Light mode 06"></td>
    <td><img src="./screenshot/2026-08-22/light_Screenshot_2026-08-22-10-06-04-617_com.neoruaa.meilox.jpg" width="200" alt="Light mode 03"></td>
    <td><img src="./screenshot/2026-08-22/light_Screenshot_2026-08-22-10-33-30-925_com.neoruaa.meilox.jpg" width="200" alt="Light mode 08"></td>
  </tr>
</table>

### 深色界面

<table>
  <tr>
    <td><img src="./screenshot/2026-08-22/dark_Screenshot_2026-08-22-10-17-19-250_com.neoruaa.meilox.jpg" width="200" alt="Dark mode 02"></td>
    <td><img src="./screenshot/2026-08-22/dark_Screenshot_2026-08-22-10-28-56-966_com.neoruaa.meilox.jpg" width="200" alt="Dark mode 07"></td>
    <td><img src="./screenshot/2026-08-22/dark_Screenshot_2026-08-22-10-19-07-140_com.neoruaa.meilox.jpg" width="200" alt="Dark mode 06"></td>
    <td><img src="./screenshot/2026-08-22/dark_Screenshot_2026-08-22-10-17-04-531_com.neoruaa.meilox.jpg" width="200" alt="Dark mode 01"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2026-08-22/dark_Screenshot_2026-08-22-10-17-59-681_com.neoruaa.meilox.jpg" width="200" alt="Dark mode 04"></td>
    <td><img src="./screenshot/2026-08-22/dark_Screenshot_2026-08-22-10-34-42-884_com.neoruaa.meilox.jpg" width="200" alt="Dark mode 08"></td>
    <td><img src="./screenshot/2026-08-22/dark_Screenshot_2026-08-22-10-17-30-088_com.neoruaa.meilox.jpg" width="200" alt="Dark mode 03"></td>
    <td><img src="./screenshot/2026-08-22/dark_Screenshot_2026-08-22-10-18-09-075_com.neoruaa.meilox.jpg" width="200" alt="Dark mode 05"></td>
  </tr>
</table>

### 播放器与歌词

<table>
  <tr>
    <td><img src="./screenshot/2026-08-22/player_Screenshot_2026-08-22-10-12-05-012_com.neoruaa.meilox.jpg" width="200" alt="Player 02"></td>
    <td><img src="./screenshot/2026-08-22/player_Screenshot_2026-08-22-10-11-20-597_com.neoruaa.meilox.jpg" width="200" alt="Player 01"></td>
    <td><img src="./screenshot/2026-08-22/player_Screenshot_2026-08-22-10-16-34-746_com.neoruaa.meilox.jpg" width="200" alt="Player 03"></td>
    <td><img src="./screenshot/2026-08-22/player_Screenshot_2026-08-22-10-16-52-289_com.neoruaa.meilox.jpg" width="200" alt="Player 04"></td>
    <td><img src="./screenshot/2026-08-22/player_Screenshot_2026-08-22-10-21-10-403_com.neoruaa.meilox.jpg" width="200" alt="Player 05"></td>
  </tr>
</table>

## 免责声明

本项目仅用于学习、研究和个人使用。使用本项目访问网易云音乐服务时，请遵守当地法律法规、网易云音乐服务条款及相关版权要求。项目维护者不对第三方服务的可用性、账号状态或音乐内容承担责任。

如果你发现侵权内容、许可证遗漏或不准确的第三方声明，欢迎通过 [Issues](https://github.com/NEORUAA/MeiloX/issues) 联系维护者。

<div align="center">

Made with Jetpack Compose · MeiloX

</div>
