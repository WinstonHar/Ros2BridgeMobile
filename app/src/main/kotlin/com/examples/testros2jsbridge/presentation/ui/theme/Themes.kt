package com.examples.testros2jsbridge.presentation.ui.theme

// Import color schemes and typography from Color.kt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Consistent Compose theme for the app, supporting dark and light mode.
 * Pulls all colors and typography from Color.kt.
 */
@Composable
fun Ros2BridgeTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AppTheme(useDarkTheme = useDarkTheme, content = content)
}