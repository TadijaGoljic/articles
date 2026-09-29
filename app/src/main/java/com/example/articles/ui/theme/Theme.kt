package com.example.articles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Color.green,
    onPrimary = Color.white,
    primaryContainer = Color.green_container,
    onPrimaryContainer = Color.on_green_container,
    secondary = Color.sage,
    onSecondary = Color.white,
    secondaryContainer = Color.sage_container,
    onSecondaryContainer = Color.on_sage_container,
    tertiary = Color.amber,
    onTertiary = Color.white,
    tertiaryContainer = Color.amber_container,
    onTertiaryContainer = Color.on_amber_container,
    background = Color.cream,
    onBackground = Color.ink,
    surface = Color.white,
    onSurface = Color.ink,
    surfaceVariant = Color.container,
    onSurfaceVariant = Color.ink_variant,
    surfaceTint = Color.green,
    surfaceBright = Color.white,
    surfaceDim = Color.container_high,
    surfaceContainerLowest = Color.white,
    surfaceContainerLow = Color.container_low,
    surfaceContainer = Color.container,
    surfaceContainerHigh = Color.container_high,
    surfaceContainerHighest = Color.container_high,
    outline = Color.outline,
    outlineVariant = Color.outline_variant,
    error = Color.red,
    onError = Color.white,
    errorContainer = Color.red_container,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.green_dark,
    onPrimary = Color.on_green_dark,
    primaryContainer = Color.green_container_dark,
    onPrimaryContainer = Color.on_green_container_dark,
    secondary = Color.sage_dark,
    onSecondary = Color.on_sage_container,
    secondaryContainer = Color.sage_container_dark,
    onSecondaryContainer = Color.sage_container,
    tertiary = Color.amber_dark,
    onTertiary = Color.on_amber_container,
    tertiaryContainer = Color.amber_container_dark,
    onTertiaryContainer = Color.amber_container,
    background = Color.night,
    onBackground = Color.paper,
    surface = Color.night_surface,
    onSurface = Color.paper,
    surfaceVariant = Color.night_container,
    onSurfaceVariant = Color.paper_variant,
    surfaceTint = Color.green_dark,
    surfaceBright = Color.night_container_high,
    surfaceDim = Color.night,
    surfaceContainerLowest = Color.night,
    surfaceContainerLow = Color.night_container_low,
    surfaceContainer = Color.night_container,
    surfaceContainerHigh = Color.night_container_high,
    surfaceContainerHighest = Color.night_container_high,
    outline = Color.outline_dark,
    outlineVariant = Color.outline_variant_dark,
    error = Color.red_dark,
    onError = Color.red_container_dark,
    errorContainer = Color.red_container_dark,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
        shapes = AppShapes,
        content = content
    )
}
