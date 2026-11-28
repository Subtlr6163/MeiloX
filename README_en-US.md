<div align="center">

# [MeiloX](https://github.com/NEORUAA/MeiloX)

<img src="./screenshot/logo.png" width="120px" style="border-radius:12px"/>

### A multi-source Android music client based on Mei, inspired by the Apple Music design

![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![License](https://img.shields.io/badge/License-GPLv3-orange)
![Status](https://img.shields.io/badge/Development-In%20Progress-yellow)

</div>

---

Language: [中文](./README.md) | **English** | [日本語](./README_ja-JP.md)

## About

MeiloX is a multi-source music client built on [Mei](https://github.com/ljyh223/Mei). It comes with a removable NetEase Cloud Music source and uses **Jetpack Compose** to bring the layered design, Liquid Glass, and immersive player experience of iOS 27 to Android.

The project focuses on three goals:

- Organizing music content in an iOS-style interface;
- Creating an immersive listening experience with Liquid Glass, dynamic backgrounds, and album artwork;
- Preserving familiar music source features such as search, playlists, lyrics, and profiles.

> MeiloX is an unofficial personal open-source project and is not affiliated with NetEase Cloud Music or Apple. The project does not provide music content; music and its associated copyrights belong to their respective owners.

## Features

- Cookie and mobile protocol login and account information for supported sources;
- Search for songs, albums, artists, and playlists;
- Playback queue, shuffle, repeat, and sleep timer;
- Playback history, liked music, playlist management, and local favorites;
- Album details, artist pages, daily recommendations, and FM playback;
- Word-by-word and multilingual lyrics;
- Apple Music-style full-screen player, mini player, and floating lyrics;
- Light and dark themes, dynamic album backgrounds, and Liquid Glass effects;
- A preinstalled, removable NetEase Cloud Music source, a separate QQ Music plugin, and automatic playback source fallback in user-defined order;
- A [plugin marketplace](https://github.com/NEORUAA/MeiloX-Source-Registry), version checks on launch, and automatic updates, with plugin submissions accepted via pull requests.

Features are still evolving, and availability may change with app versions and music source APIs.

See [Music Sources](docs/music-sources.md) for importing, configuring, and switching sources. Developers can also refer to the [Plugin Specification and SDK](docs/source-plugin-spec.md) and [Architecture and Compatibility Migration](docs/multi-source-architecture.md).

## Lyrics Sharing

MeiloX integrates [Lyricon Provider](https://tomakino.github.io/lyricon/zh-cn/developer/provider/), [SuperLyricApi](https://github.com/HChenX/SuperLyricApi), and [ColorOS Live Lyrics Bridge](https://github.com/Andrea-lyz/ColorOS-Live-Lyrics-Bridge) to share lyrics, word timings, translations, and playback status.

“Enable lyrics sharing” is on by default under Settings → Lyrics Settings → Lyrics Sharing.

## Acknowledgements

MeiloX's interface, lyrics, and underlying functionality are inspired by or directly use open-source components from the following projects. Thanks to all contributors:

- [Mei](https://github.com/ljyh223/Mei): upstream Android project and core functionality;
- [MeloX](https://github.com/youshen2/MeloX): iOS reference implementation and product direction;
- [Glasmio](https://github.com/NEORUAA/Glasmio): iOS-style animation and UI library;
- [amll-ttml-db](https://github.com/Steve-xmh/amll-ttml-db): word-by-word lyrics data;
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui): lyrics models and rendering components;
- [Lyricon](https://github.com/tomakino/lyricon): lyrics provider SDK;
- [SuperLyricApi](https://github.com/HChenX/SuperLyricApi): lyrics sharing SDK;
- [ColorOS Live Lyrics Bridge](https://github.com/Andrea-lyz/ColorOS-Live-Lyrics-Bridge): ColorOS lock-screen lyrics interface.

## License and Third-Party Notices

The main MeiloX code is released under the [GNU General Public License v3](LICENSE). MeiloX is derived from Mei, and upstream code and reference implementations remain subject to their original copyrights and licenses. Third-party libraries, models, scripts, fonts, and visual assets do not automatically become GPLv3-licensed because MeiloX uses GPLv3.

Key notices:

- Upstream [Mei](https://github.com/ljyh223/Mei) code is licensed under Apache License 2.0;
- Relevant reference code from [MeloX](https://github.com/youshen2/MeloX) is subject to that project's GPLv3 terms;
- [Glasmio](https://github.com/NEORUAA/Glasmio) is licensed under Apache License 2.0; see [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES) for the origins and attribution of retained third-party code;
- [amll-ttml-db](https://github.com/Steve-xmh/amll-ttml-db) and [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui) follow the licenses and attribution requirements published in their respective repositories;
- The BeatNet model, audio fingerprint runtime, ONNX Runtime, PV Tool-derived content, and other distributed components are subject to their respective licenses, copyright notices, and redistribution restrictions.

See [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES) for complete source, copyright, license, and redistribution details. The `res/font/sf_pro.ttf` (SF Pro) and `res/font/pingfang_sc_*.ttf` (PingFang SC) fonts are Apple proprietary and used solely as personal project resources; they are not covered by this project's GPLv3 license. Do not extract or redistribute them separately from build artifacts, and replace them with licensed-able fonts before publicly releasing a build.

## Screenshots

<table width="100%">
  <tr>
    <td width="25%"><img src="./screenshot/2026-11-16/player/Screenshot_2026-11-16-12-17-40-602_com.neoruaa.meilox.jpg" width="100%" alt="Player screenshot 1"></td>
    <td width="25%"><img src="./screenshot/2026-11-16/player/Screenshot_2026-11-16-12-20-48-527_com.neoruaa.meilox.jpg" width="100%" alt="Player screenshot 2"></td>
    <td width="25%"><img src="./screenshot/2026-11-16/player/Screenshot_2026-11-16-12-20-04-075_com.neoruaa.meilox.jpg" width="100%" alt="Player screenshot 3"></td>
    <td width="25%"><img src="./screenshot/2026-11-16/player/Screenshot_2026-11-16-12-22-45-066_com.neoruaa.meilox.jpg" width="100%" alt="Player screenshot 4"></td>
  </tr>
  <tr>
    <td colspan="3" width="75%"><img src="./screenshot/2026-11-16/ui/pad/Screenshot_2026-11-16-13-36-58-88_b4ebea482d4eb8966c92221d11addacb.jpg" width="100%" alt="Tablet player screenshot"></td>
    <td width="25%"><img src="./screenshot/2026-11-16/player/Screenshot_2026-11-16-12-25-00-174_com.neoruaa.meilox.jpg" width="100%" alt="Player screenshot 5"></td>
  </tr>
</table>

<table>
  <tr>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-11-52-19-696_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 1"></td>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-11-53-11-793_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 2"></td>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-11-55-37-127_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 3"></td>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-12-00-51-927_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 4"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-12-02-20-868_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 5"></td>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-12-16-08-010_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 6"></td>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-12-07-37-859_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 7"></td>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-12-09-58-456_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 8"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-12-04-02-421_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 9"></td>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-12-11-54-815_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 10"></td>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-12-13-27-264_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 11"></td>
    <td><img src="./screenshot/2026-11-16/ui/phone/Screenshot_2026-11-16-12-13-58-153_com.neoruaa.meilox.jpg" width="200" alt="Phone screenshot 12"></td>
  </tr>
</table>

<table>
  <tr>
    <td><img src="./screenshot/2026-11-16/ui/pad/Screenshot_2026-11-16-13-45-46-10_b4ebea482d4eb8966c92221d11addacb.jpg" width="320" alt="Tablet screenshot 1"></td>
    <td><img src="./screenshot/2026-11-16/ui/pad/Screenshot_2026-11-16-13-42-08-96_b4ebea482d4eb8966c92221d11addacb.jpg" width="320" alt="Tablet screenshot 2"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2026-11-16/ui/pad/Screenshot_2026-11-16-13-37-33-42_b4ebea482d4eb8966c92221d11addacb.jpg" width="320" alt="Tablet screenshot 3"></td>
    <td><img src="./screenshot/2026-11-16/ui/pad/Screenshot_2026-11-16-13-35-15-01_b4ebea482d4eb8966c92221d11addacb.jpg" width="320" alt="Tablet screenshot 4"></td>
  </tr>
</table>

## Build from Source

Use JDK 21 and Android SDK 37. By default, the latest Glasmio source is built from the sibling `../Glasmio` directory:

```sh
git clone https://github.com/NEORUAA/Glasmio.git ../Glasmio
./gradlew :app:assembleDebug
```

To use the latest published Maven package, explicitly disable source substitution:

```sh
./gradlew -Pglasmio.source=false -Pglasmio.version=0.1.3 :app:assembleDebug
```

See the [Glasmio integration guide](docs/glasmio-integration.md) for its renderer, navigation viewport binding, and MiniPlayer native attachment animations.

## Disclaimer

This project is intended for learning, research, and personal use only. When using it to access NetEase Cloud Music, comply with local laws, the music service's terms, and applicable copyright requirements. The maintainers are not responsible for third-party service availability, account status, or music content.

**About the fonts:** to match iOS typography, this project bundles Apple's **SF Pro** (from Apple's official SF Pro download) and **PingFang SC** (split out of `PingFang.ttc`) under `app/src/main/res/font/`. Both are proprietary Apple fonts; all rights belong to Apple and they are **not** covered by this project's GPLv3 license. They are included for local, personal builds only. Do not publish, redistribute, or commercially use source archives or APKs that contain them; before any public release, delete these font files and substitute fonts you are licensed to redistribute (for example Noto Sans SC / Source Han Sans under the SIL OFL).

This project is not affiliated with, authorized by, or sponsored by Apple. Apple, Apple Music, iOS, SF Pro, and PingFang are trademarks of Apple Inc. No music content is distributed with this project; all rights remain with their respective owners.

Any consequence of using software built from this project (including but not limited to account issues, data loss, device malfunction, or legal risk) is borne solely by the user; the maintainers accept no liability.

If you find infringing content, missing license information, or inaccurate third-party notices, please contact the maintainers through [Issues](https://github.com/NEORUAA/MeiloX/issues).

<div align="center">

**Made with Jetpack Compose ♥ MeiloX**

</div>
