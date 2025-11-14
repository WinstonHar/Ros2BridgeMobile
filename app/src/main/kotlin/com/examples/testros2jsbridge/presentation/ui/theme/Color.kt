package com.examples.testros2jsbridge.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors
val LightPrimary = Color(0xFF1976D2)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF222222)
val LightSecondary = Color(0xFF03DAC6)
val LightError = Color(0xFFB00020)

// Dark theme colors
val DarkPrimary = Color(0xFF90CAF9)
val DarkOnPrimary = Color(0xFF222222)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF222222)
val DarkOnSurface = Color(0xFFF5F5F5)
val DarkSecondary = Color(0xFF03DAC6)
val DarkError = Color(0xFFCF6679)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    secondary = LightSecondary,
    error = LightError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    secondary = DarkSecondary,
    error = DarkError
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

// Example Typography (customize as needed)
val Typography = Typography()