package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.util.AppTextFormat

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val InterFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val NunitoFamily = FontFamily(
    Font(R.font.nunito, FontWeight.Normal),
    Font(R.font.nunito, FontWeight.Medium),
    Font(R.font.nunito, FontWeight.SemiBold),
    Font(R.font.nunito, FontWeight.Bold)
)

val LoraFamily = FontFamily(
    Font(R.font.lora, FontWeight.Normal),
    Font(R.font.lora, FontWeight.Medium),
    Font(R.font.lora, FontWeight.SemiBold),
    Font(R.font.lora, FontWeight.Bold)
)

val RobotoMonoFamily = FontFamily(
    Font(R.font.roboto_mono, FontWeight.Normal),
    Font(R.font.roboto_mono, FontWeight.Medium),
    Font(R.font.roboto_mono, FontWeight.SemiBold),
    Font(R.font.roboto_mono, FontWeight.Bold)
)

fun displayFontFamilyFor(format: AppTextFormat): FontFamily = when (format) {
    AppTextFormat.MODERN -> PlusJakartaSansFamily
    AppTextFormat.ROUNDED -> NunitoFamily
    AppTextFormat.SERIF -> LoraFamily
    AppTextFormat.MONO -> RobotoMonoFamily
    AppTextFormat.SYSTEM -> FontFamily.Default
}

fun bodyFontFamilyFor(format: AppTextFormat): FontFamily = when (format) {
    AppTextFormat.MODERN -> InterFamily
    AppTextFormat.ROUNDED -> NunitoFamily
    AppTextFormat.SERIF -> LoraFamily
    AppTextFormat.MONO -> RobotoMonoFamily
    AppTextFormat.SYSTEM -> FontFamily.Default
}

fun buildTabularAmountStyle(format: AppTextFormat): TextStyle = TextStyle(
    fontFamily = bodyFontFamilyFor(format),
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    fontFeatureSettings = "tnum"
)

fun buildLedgerTypography(format: AppTextFormat): Typography {
    val display = displayFontFamilyFor(format)
    val body = bodyFontFamilyFor(format)
    return Typography(
        displaySmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        titleSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 15.sp
        ),
        labelLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    )
}

val LocalTabularAmountStyle = staticCompositionLocalOf {
    buildTabularAmountStyle(AppTextFormat.MODERN)
}

val TabularAmountStyle: TextStyle
    @Composable @ReadOnlyComposable get() = LocalTabularAmountStyle.current

val Typography = buildLedgerTypography(AppTextFormat.MODERN)
