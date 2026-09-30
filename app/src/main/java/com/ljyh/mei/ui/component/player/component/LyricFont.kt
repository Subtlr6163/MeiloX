package com.ljyh.mei.ui.component.player.component

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ljyh.mei.R

/**
 * 歌词统一字体：Samsung Sharp Sans（三星 One UI 西文品牌字体）。
 * - Regular(400) → SamsungSharpSans Regular
 * - Bold(700)    → SamsungSharpSans Bold
 * 中文由系统按 fallback 链自动选择（与三星手机上 One UI 实际渲染一致）。
 *
 * 所有歌词展示位置（全屏歌词、悬浮 Pip、Liquid 歌词效果）统一使用此字体。
 */
val LyricFontFamily: FontFamily = FontFamily(
    Font(R.font.samsung_sharp_sans_regular, FontWeight.Normal),
    Font(R.font.samsung_sharp_sans_bold, FontWeight.Bold),
)
