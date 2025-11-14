package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.examples.testros2jsbridge.domain.model.AppAction

@Composable
fun ControllerButton(
    labelText: String,
    assignedAction: AppAction?,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    textAlignCenter: Boolean = false
) {
    Button(
        onClick = onPress,
        modifier = modifier
    ) {
        Text(text = labelText)
    }
}
