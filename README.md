# MeiloX 完整源码（iOS 字体修正版）

这个压缩包里是**可以直接编译的完整工程**，不是补丁：

```
MeiloX-full-source/
├─ MeiloX/     ← 主工程（已包含全部字体修正）
├─ Glasmio/    ← Glasmio 源码，必须和 MeiloX 放在同一个父目录
└─ BUILD.md    ← 本文件
```

> `MeiloX/settings.gradle.kts` 在 `-Pglasmio.source=true`（默认值）时，会去找
> `../Glasmio`。两个文件夹保持现在的同级关系就能编译；拆开或改名会导致
> `-Pglasmio.source=true requires a Glasmio checkout beside MeiloX.`

## 为什么必须带 Glasmio 源码

MeiloX 的 HEAD 用到了 Glasmio 里较新的 API（`LocalGlasmioHorizontalSafeAreaEnabled`），
Maven Central 上的 `io.github.neoruaa.glasmio:0.1.3` 还没有这个符号。
用 `-Pglasmio.source=false` 编译会失败：

```
Unresolved reference 'LocalGlasmioHorizontalSafeAreaEnabled'.
Location: app/src/main/java/com/ljyh/mei/MainActivity.kt line 274
```

所以请保持 `glasmio.source=true`（gradle.properties 里已默认），让 Gradle 用源码构建 Glasmio。

## 环境要求

本机已经装好了，路径如下：

| 组件 | 位置 / 版本 |
|---|---|
| JDK | `C:\Program Files\Java\jdk-21.0.12`（17 以上都行） |
| Android SDK | `C:\Android\sdk`（`MeiloX/local.properties` 已指向这里） |
| Platform | `platforms/android-37.0`（compileSdk 37） |
| Build-Tools | `37.0.0` |
| CMake | `3.22.1` |
| NDK | `29.0.14206865` |
| Gradle | 9.6.0（wrapper 自动下载，走腾讯镜像） |

换机器的话，把 `local.properties` 里的 `sdk.dir` 改成自己的 SDK 路径，并确保上面五个
SDK 组件都装了（`sdkmanager` 或 Android Studio SDK Manager 均可）。

## 编译

在 `MeiloX/` 目录下：

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12"
$env:ANDROID_HOME = "C:\Android\sdk"

# 调试包
.\gradlew.bat :app:assembleDebug -Pglasmio.source=true

# 正式包（R8 + 资源压缩）
.\gradlew.bat :app:assembleRelease -Pglasmio.source=true
```

产物：

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`（**未签名**）

正式包要自己签名才能安装：

```powershell
$bt = "C:\Android\sdk\build-tools\37.0.0"
& "$bt\zipalign.exe" -p -f 4 app-release-unsigned.apk aligned.apk
& "$bt\apksigner.bat" sign --ks 你的.jks --out MeiloX-release.apk aligned.apk
```

> 我上一轮交付的两个 APK 是用 `C:\Users\MEMZ4\.android\debug.keystore`
> （别名 `androiddebugkey`，密码 `android`）签的，所以能直接覆盖安装；
> 公开分发请换成自己的 keystore。

## 本轮改动（相对上游 main）

| 文件 | 改动 |
|---|---|
| `app/src/main/java/com/ljyh/mei/ui/theme/Type.kt` | SF Pro 拆成 Text(opsz 17)/Display(opsz 28) 两套光学尺寸；字重轴改用 300/400/510/590/700；每个 Material role 写入 Apple 的逐级字距；新增 Noto Sans SC 作为中文字体回退 |
| `app/src/main/java/com/ljyh/mei/ui/theme/Theme.kt` | 字距为 0 时不再全局覆盖，交回各样式自己的 Apple 字距 |
| `app/src/main/java/com/ljyh/mei/constants/PreferenceKeys.kt` | `DefaultInterfaceLetterSpacing` 0.4 → 0（0 = 跟随 iOS）；字距偏好改用 `interfaceLetterSpacingV2` 键，清掉历史遗留的 0.4 |
| `app/src/main/java/com/ljyh/mei/ui/screen/setting/AppearanceSettings.kt` | 字距滑杆为 0 时显示“iOS 默认” |
| `app/src/main/res/values*/strings.xml` | 新增 `appearance_letter_spacing_ios` 文案（en / zh / zh-rTW / ja） |
| `app/src/main/res/font/sf_pro.ttf` | 换成 Apple 官网 SF-Pro.dmg 里的 22.2d2e1（21,440,708 字节） |
| `app/src/main/res/font/pingfang_sc_*.ttf` | 新增：从 `PingFang.ttc` 拆出的 SC Regular / Medium / Semibold 三个字重，作为中文字体回退（**仅限个人使用，不可公开分发**） |
| `THIRD_PARTY_NOTICES` | 新增 PingFang 与 SF Pro 的个人使用声明 |


`typography-ios-look.patch` 也留在工程根目录，想对照上游看 diff 可以直接用。

## 使用限制与免责声明

本工程在 `app/src/main/res/font/` 内置了 Apple 的 **SF Pro** 和 **PingFang SC**。这两套字体是 Apple 的专有字体，**不随本项目的 GPLv3 授权**，仅限本地个人构建使用，请勿公开分发包含它们的源码包或安装包；公开发布前请删除并替换为可再分发的字体（例如 SIL OFL 的 Noto Sans SC / 思源黑体）。

本项目与 Apple 无任何隶属或授权关系；Apple、Apple Music、iOS、SF Pro、PingFang 为其商标。本工程仅供学习研究，使用产生的一切后果由使用者自行承担。
