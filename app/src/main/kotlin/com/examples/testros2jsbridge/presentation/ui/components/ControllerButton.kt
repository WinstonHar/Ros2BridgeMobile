package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.domain.model.AppAction

@Composable
fun ControllerButton(
    labelText: String,
    label: @Composable () -> Unit = { Text(labelText) },
    assignedAction: AppAction?,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    textAlignCenter: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val backgroundColor = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .padding(4.dp)
            .background(backgroundColor, shape = MaterialTheme.shapes.small)
            .clickable(
                onClick = {
                    isPressed = true
                    onPress()
                },
                onClickLabel = labelText
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }
                )
            }
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (textAlignCenter) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                    color = contentColor,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else {
                label()
            }
        }
    }
}