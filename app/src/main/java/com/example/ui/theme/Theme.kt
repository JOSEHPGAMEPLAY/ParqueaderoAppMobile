package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GeoPrimary,
    secondary = GeoPrimary,
    tertiary = GeoPrimary,
    background = GeoBgDark,
    surface = GeoSurfaceDark,
    onPrimary = GeoOnPrimary,
    onSecondary = GeoOnPrimary,
    onTertiary = GeoOnPrimary,
    onBackground = GeoOnBg,
    onSurface = GeoOnBg,
    error = GeoError,
    surfaceVariant = GeoSurfaceVariant,
    onSurfaceVariant = GeoOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = GeoOnPrimary,
    secondary = GeoPrimary,
    tertiary = GeoPrimary,
    background = Color(0xFFF4F6F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    error = GeoError,
    surfaceVariant = Color(0xFFE5ECE9),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force visual identity dark vibe by default for premium automotive feel
    dynamicColor: Boolean = false, // Disable to preserve custom branding palette
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
