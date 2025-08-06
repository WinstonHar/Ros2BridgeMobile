package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
            .size(48.dp)
    ) {
        label()
    }
}