# MeiloX 字体修复 · 改动说明

## 一、为什么原来「一点都不像」

仓库里只有一个 `res/font/sf_pro.ttf`，它是 SF Pro **可变字体**。代码里这样注册的：

```kotlin
val AppFontFamily = FontFamily(Font(R.font.sf_pro, FontWeight.Normal))
```

三个致命问题叠在一起：

1. **只注册了一个字重**。整份排版里所有 `FontWeight.SemiBold / Bold` 请求，最终都由 Android 拿 Regular 字形做**合成加粗**（描边描出来的假粗体），笔画发虚、字腔糊掉 —— 这是「不像」最大的元凶。可变字体自带的 `wght`(1–1000) 和 `opsz`(Text/Display) 轴完全没被用上。
2. **这份 SF Pro 里一个中文字形都没有**。实测 cmap 里 CJK 区（U+4E00–9FFF）命中数为 **0**，所以界面上全部中文 100% 回落到安卓系统黑体，中英混排风格割裂。
3. **默认字距是 1sp**。SF Pro 本身已带 Apple 的 tracking，再叠 1sp 让每个标签都被拉散。

## 二、修法

用 fontTools 在构建前把字体处理成**静态字重实例**，再合并中文字形：

| 步骤 | 做法 |
| --- | --- |
| 取真实字重 | 从可变字体轴实例化 `wght=400/500/600/700`，`opsz=17(Text)` 与 `opsz=28(Display)` 两套光学尺寸 |
| 补中文 | Noto Sans SC（思源黑体，SIL OFL 1.1）同样取 400/500/600/700 真实字重 |
| 合并 | 拉丁/希腊/西里尔取 SF Pro，**中文取 Noto**，合并成一个文件一个字重；统一 upem 到 2048，竖直度量取两者较大值避免行高裁切 |
| 瘦身 | 拉丁子集 ~133KB；中文子集保留 GB2312 全部汉字 + 标点/假名/注音/全角等约 9100 字 |

最终每个字重文件约 2.5MB，共 6 个（Regular / Medium / Semibold / Bold / Display Regular / Display Bold）。

SF Symbols 图标字单独拆成 `sf_symbols.ttf`（仅 PUA U+100000–103FFF，1.9MB），供 `SfSymbol.kt` 使用。

### 体积账

删掉了原本 25MB 的 `sf_pro.ttf`，换上 6×2.5MB + 1.9MB ≈ 16.9MB。**APK 体积不增反降**。

## 三、改了哪些文件

```
app/src/main/res/font/
  ├─ app_font_regular.ttf          ← 新增
  ├─ app_font_medium.ttf           ← 新增
  ├─ app_font_semibold.ttf         ← 新增
  ├─ app_font_bold.ttf             ← 新增
  ├─ app_font_display_regular.ttf  ← 新增
  ├─ app_font_display_bold.ttf     ← 新增
  ├─ sf_symbols.ttf                ← 新增（图标）
  └─ sf_pro.ttf                    ← 删除（25MB 可变字体）

app/src/main/java/com/ljyh/mei/ui/theme/Type.kt            ← 重写：四档真实字重 + Display 光学尺寸
app/src/main/java/com/ljyh/mei/ui/glass/SfSymbol.kt        ← 改：图标字体指向 sf_symbols.ttf
app/src/main/java/com/ljyh/mei/constants/PreferenceKeys.kt ← 改：默认字距 1f → 0f
THIRD_PARTY_NOTICES                                        ← 补：字体来源与许可声明
.github/workflows/build-apk.yml                            ← 新增：免密钥构建签名工作流
```

`IosTypography` 的语义映射（largeTitle / title2 / headline / body / subheadline / caption）保持原样，不需要动 —— 它们读的是 `MaterialTheme.typography`，换掉底层 `AppFontFamily` 后自动生效，**歌词、设置页、播放器全量覆盖**。

## 四、怎么拿到 release APK

项目自带的工作流 `main.yml` 依赖 `secrets.SIGNING_KEY`，fork 后没有这个密钥会直接失败。所以我另加了一个 `build-apk.yml`，**不需要任何密钥**，自己生成一次性签名：

1. Fork `NEORUAA/MeiloX`
2. 把本压缩包里的文件按上面的目录结构覆盖进去（记得删掉 `app/src/main/res/font/sf_pro.ttf`）
3. 推送到任意分支
4. Actions → `Build APK (self-signed)` → 运行结束后在 **Artifacts** 里下载 `MeiloX-release`

本地构建同理：

```bash
./gradlew assembleRelease
```

要求 JDK 21 + Android SDK platform 37 + build-tools 37.0.0 + NDK 27.1.12297006（项目含 beatnet native 代码）。

## 五、可回退项

- **默认字距改回**：`PreferenceKeys.kt` 的 `DefaultInterfaceLetterSpacing` 改回 `1f`。设置里「界面字距」仍可在 0–3sp 间手动调。
- **中文想换回系统字体**：删掉各 `app_font_*.ttf` 里的中文字形即可，或把 `AppFontFamily` 的第一个 `Font` 换成只含拉丁的 `sf_pro` 子集。

## 六、许可提醒

`app_font_*.ttf` 中的拉丁字形来自 SF Pro，Apple 保留全部权利；中文字形来自 Noto Sans SC（SIL OFL 1.1）。**公开发布构建版本前请重新核对 Apple 的字体许可条款**，`THIRD_PARTY_NOTICES` 已补充对应声明。
