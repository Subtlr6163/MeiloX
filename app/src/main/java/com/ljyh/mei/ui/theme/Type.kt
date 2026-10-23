package com.ljyh.mei.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ljyh.mei.R

/**
 * Latin glyphs come from SF Pro, CJK glyphs from Noto Sans SC, merged into one file per weight.
 *
 * Every weight is a real static cut instead of a synthesised fake bold, and Chinese text no
 * longer falls back to the device sans. The old `res/font/sf_pro.ttf` variable font served a
 * single default instance for all weights and carried no CJK glyphs at all.
 */
val AppFontFamily = FontFamily(
    Font(R.font.app_font_regular, FontWeight.Normal),
    Font(R.font.app_font_medium, FontWeight.Medium),
    Font(R.font.app_font_semibold, FontWeight.SemiBold),
    Font(R.font.app_font_bold, FontWeight.Bold),
)

/** Display optical size: tighter sidebearings, meant for 20pt and up (large titles). */
val AppDisplayFontFamily = FontFamily(
    Font(R.font.app_font_display_regular, FontWeight.Normal),
    Font(R.font.app_font_display_bold, FontWeight.Bold),
)

/**
 * iOS/Apple Music type scale. Large text uses the Display optical size and tighter tracking,
 * body text uses the Text optical size, matching how Apple renders its own UI.
 */
fun appTypography(letterSpacing: TextUnit): Typography {
    val base = TextStyle(fontFamily = AppFontFamily, letterSpacing = letterSpacing)
    val display = TextStyle(fontFamily = AppDisplayFontFamily, letterSpacing = letterSpacing * 0.5f)
    return Typography(
        displayLarge = display.copy(fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp),
        displayMedium = display.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp),
        displaySmall = display.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
        headlineLarge = display.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp),
        headlineMedium = display.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
        headlineSmall = display.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp),
        titleMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
        titleSmall = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = base.copy(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp),
        bodyMedium = base.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
        bodySmall = base.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
        labelMedium = base.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = base.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp),
    )
}
