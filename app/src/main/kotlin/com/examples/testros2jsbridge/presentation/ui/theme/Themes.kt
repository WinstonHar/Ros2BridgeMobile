package com.examples.testros2jsbridge.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

// Import color schemes and typography from Color.kt
import com.examples.testros2jsbridge.presentation.ui.theme.*

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