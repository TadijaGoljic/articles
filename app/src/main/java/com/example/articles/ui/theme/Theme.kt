package com.example.articles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Color.blue,
    onPrimary = Color.white,
    surface = Color.haze,
    onSurface = Color.black,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.blue_dark,
    onPrimary = Color.white_dark,
    surface = Color.haze_dark,
    onSurface = Color.black_dark,
)

@Composable
fun ArticlesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}