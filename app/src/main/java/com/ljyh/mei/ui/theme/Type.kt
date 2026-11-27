package com.ljyh.mei.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font as PlatformFont
import android.graphics.fonts.FontFamily as PlatformFontFamily
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ljyh.mei.R

/**
 * iOS renders SF Pro with two optical sizes: SF Pro Text up to 17 pt and SF Pro Display from
 * 20 pt. The bundled variable font defaults to the display cut (opsz 28), so a family that does
 * not pin the axis draws body copy with display glyph widths: at 17 pt the letters are about
 * 12 % narrower and the x-height is smaller than on iOS, which only reads correctly once the
 * missing tracking is papered over with extra letter spacing.
 *
 * SF Pro carries no CJK glyphs, so Chinese is drawn with the bundled Noto Sans SC (SIL OFL 1.1)
 * through a custom fallback instead of whatever sans the OEM happens to ship.
 */
private const val TextOpticalSize = 17
private const val DisplayOpticalSize = 28
private const val OpticalSizeBreakpoint = 20

/** SF Pro's real 'wght' stops; Compose's Medium and SemiBold resolve onto 510 and 590. */
private val sfProWeightAxes = listOf(
    FontWeight(300) to 300,
    FontWeight(400) to 400,
    FontWeight(510) to 510,
    FontWeight(590) to 590,
    FontWeight(700) to 700,
)

private class SfProFont(
    override val weight: FontWeight,
    val opticalSize: Int,
    val axisWeight: Int,
) : AndroidFont(
    loadingStrategy = FontLoadingStrategy.Blocking,
    typefaceLoader = SfProFontLoader,
    variationSettings = FontVariation.Settings(),
) {
    override val style = FontStyle.Normal

    override fun toString(): String = "SfProFont(weight=$weight, opsz=$opticalSize, wght=$axisWeight)"
}

private object SfProFontLoader : AndroidFont.TypefaceLoader {
    private var regularFont: PlatformFont? = null
    private var cjkRegularFont: PlatformFont? = null
    private var symbolTypeface: Typeface? = null
    private val cjkFamilies = HashMap<Int, PlatformFontFamily>()
    private val opticalFamilies = HashMap<Pair<Int, Int>, Typeface>()

    private fun baseFont(context: Context): PlatformFont =
        regularFont ?: PlatformFont.Builder(context.resources, R.font.sf_pro).build()
            .also { regularFont = it }

    private fun cjkBaseFont(context: Context): PlatformFont =
        cjkRegularFont ?: PlatformFont.Builder(context.resources, R.font.noto_sans_sc).build()
            .also { cjkRegularFont = it }

    private fun face(base: PlatformFont, axisWeight: Int, opticalSize: Int): PlatformFont =
        PlatformFont.Builder(base)
            .setWeight(axisWeight)
            .setFontVariationSettings("'wght' $axisWeight, 'opsz' $opticalSize")
            .build()

    private fun cjkFamily(context: Context, axisWeight: Int): PlatformFontFamily =
        cjkFamilies.getOrPut(axisWeight) {
            PlatformFontFamily.Builder(
                PlatformFont.Builder(cjkBaseFont(context))
                    .setWeight(axisWeight)
                    .setFontVariationSettings("'wght' $axisWeight")
                    .build(),
            ).build()
        }

    /**
     * SF Pro has no CJK glyphs, so without an explicit fallback Chinese resolves to the OEM
     * font (MiSans, OPPO Sans, ...). Noto Sans SC is the open Source Han design and keeps the
     * humanist shapes iOS ships as PingFang SC.
     */
    @Synchronized
    private fun opticalFamilyTypeface(context: Context, opticalSize: Int, axisWeight: Int): Typeface =
        opticalFamilies.getOrPut(opticalSize to axisWeight) {
            val primary = PlatformFontFamily.Builder(face(baseFont(context), axisWeight, opticalSize)).build()
            Typeface.CustomFallbackBuilder(primary)
                .addCustomFallback(cjkFamily(context, axisWeight))
                .build()
        }

    @Synchronized
    fun getSymbolTypeface(context: Context): Typeface {
        symbolTypeface?.let { return it }
        val regular = baseFont(context)
        // Symbols keep their single-face weight resolution and share the text font's buffer.
        return Typeface.CustomFallbackBuilder(PlatformFontFamily.Builder(regular).build())
            .setStyle(regular.style)
            .build()
            .also { symbolTypeface = it }
    }

    @Synchronized
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface {
        val sfPro = font as SfProFont
        return opticalFamilyTypeface(context, sfPro.opticalSize, sfPro.axisWeight)
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface =
        loadBlocking(context, font)
}

internal fun sfProSymbolTypeface(context: Context): Typeface =
    SfProFontLoader.getSymbolTypeface(context)

private fun sfProFamily(opticalSize: Int): FontFamily =
    FontFamily(sfProWeightAxes.map { (weight, axisWeight) -> SfProFont(weight, opticalSize, axisWeight) })

/** SF Pro Text: body sizes, 17 pt and below on iOS. */
val SfProText = sfProFamily(TextOpticalSize)

/** SF Pro Display: title sizes, 20 pt and above on iOS. */
val SfProDisplay = sfProFamily(DisplayOpticalSize)

// Apple's Large-category tracking, in points at the size each role uses. Applying these per
// style is what separates iOS text from a single flat letter-spacing override: 17 pt body copy
// is tightened, while 22 pt and larger titles open up.
private const val LargeTitleTracking = 0.40f
private const val Title1Tracking = 0.38f
private const val Title2Tracking = -0.26f
private const val Title3Tracking = -0.45f
private const val HeadlineTracking = -0.43f
private const val SubheadlineTracking = -0.23f
private const val FootnoteTracking = -0.08f
private const val Caption1Tracking = 0f
private const val Caption2Tracking = 0.06f

private fun iosTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Float,
): TextStyle = TextStyle(
    fontFamily = if (size >= OpticalSizeBreakpoint) SfProDisplay else SfProText,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

/** Shared typography for Material controls, iOS components, inputs, and lyrics. */
fun appTypography(): Typography = Typography(
    displayLarge = iosTextStyle(34, 41, FontWeight.Bold, LargeTitleTracking),
    displayMedium = iosTextStyle(28, 34, FontWeight.Bold, Title1Tracking),
    displaySmall = iosTextStyle(22, 28, FontWeight.Bold, Title2Tracking),
    headlineLarge = iosTextStyle(34, 41, FontWeight.Bold, LargeTitleTracking),
    headlineMedium = iosTextStyle(28, 34, FontWeight.Bold, Title1Tracking),
    headlineSmall = iosTextStyle(22, 28, FontWeight.Bold, Title2Tracking),
    titleLarge = iosTextStyle(20, 25, FontWeight(590), Title3Tracking),
    titleMedium = iosTextStyle(17, 22, FontWeight(590), HeadlineTracking),
    titleSmall = iosTextStyle(15, 20, FontWeight(590), SubheadlineTracking),
    bodyLarge = iosTextStyle(17, 22, FontWeight.Normal, HeadlineTracking),
    bodyMedium = iosTextStyle(15, 20, FontWeight.Normal, SubheadlineTracking),
    bodySmall = iosTextStyle(13, 18, FontWeight.Normal, FootnoteTracking),
    labelLarge = iosTextStyle(17, 22, FontWeight(590), HeadlineTracking),
    labelMedium = iosTextStyle(12, 16, FontWeight.Normal, Caption1Tracking),
    labelSmall = iosTextStyle(11, 13, FontWeight(510), Caption2Tracking),
)
