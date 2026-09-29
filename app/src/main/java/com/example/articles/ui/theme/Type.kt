package com.example.articles.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.articles.R

val fontFamily = FontFamily(
    Font(resId = R.font.alan_sans_bold, weight = FontWeight.Bold),
    Font(resId = R.font.alan_sans_black, weight = FontWeight.Black),
    Font(resId = R.font.alan_sans_light, weight = FontWeight.Light),
    Font(resId = R.font.alan_sans_medium, weight = FontWeight.Medium),
    Font(resId = R.font.alan_sans_regular, weight = FontWeight.Normal),
    Font(resId = R.font.alan_sans_semi_bold, weight = FontWeight.SemiBold),
    Font(resId = R.font.alan_sans_extra_bold, weight = FontWeight.ExtraBold)
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Double = 0.0
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp
)

val Typography = Typography(
    displaySmall = style(size = 36, lineHeight = 44, weight = FontWeight.ExtraBold),
    headlineLarge = style(size = 32, lineHeight = 40, weight = FontWeight.ExtraBold, letterSpacing = -0.5),
    headlineMedium = style(size = 28, lineHeight = 36, weight = FontWeight.Bold, letterSpacing = -0.25),
    headlineSmall = style(size = 24, lineHeight = 32, weight = FontWeight.Bold),
    titleLarge = style(size = 22, lineHeight = 28, weight = FontWeight.Bold),
    titleMedium = style(size = 17, lineHeight = 24, weight = FontWeight.SemiBold),
    titleSmall = style(size = 14, lineHeight = 20, weight = FontWeight.SemiBold),
    bodyLarge = style(size = 16, lineHeight = 24, letterSpacing = 0.15),
    bodyMedium = style(size = 14, lineHeight = 20, letterSpacing = 0.15),
    bodySmall = style(size = 12, lineHeight = 16, letterSpacing = 0.2),
    labelLarge = style(size = 14, lineHeight = 20, weight = FontWeight.SemiBold, letterSpacing = 0.1),
    labelMedium = style(size = 12, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.4),
    labelSmall = style(size = 11, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.5),
)
