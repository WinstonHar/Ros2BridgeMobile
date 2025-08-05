package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ControllerButton(
    label: String,
    assignedAction: AppAction?,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(4.dp)
            .background(if (isPressed) MaterialTheme.colorScheme.primary else Color.LightGray)
            .clickable(
                onClick = {
                    isPressed = true
                    onPress()
                },
                onClickLabel = label
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
            .size(48.dp)
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else Color.Black,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}