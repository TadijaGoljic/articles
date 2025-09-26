package com.example.articles.ui.theme

import com.example.articles.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font

val fontFamily = FontFamily(
    Font(resId = R.font.alan_sans_bold, weight = FontWeight.Bold),
    Font(resId = R.font.alan_sans_black, weight = FontWeight.Black),
    Font(resId = R.font.alan_sans_light, weight = FontWeight.Light),
    Font(resId = R.font.alan_sans_medium, weight = FontWeight.Medium),
    Font(resId = R.font.alan_sans_regular, weight = FontWeight.Normal),
    Font(resId = R.font.alan_sans_semi_bold, weight = FontWeight.SemiBold),
    Font(resId = R.font.alan_sans_extra_bold, weight = FontWeight.ExtraBold)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)