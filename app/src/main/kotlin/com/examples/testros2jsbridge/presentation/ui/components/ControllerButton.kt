package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.examples.testros2jsbridge.domain.model.AppAction

@Composable
fun ControllerButton(
    labelText: String,
    assignedAction: com.examples.testros2jsbridge.domain.model.AppAction?,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    textAlignCenter: Boolean = false
) {
    Button(
        onClick = onPress,
        modifier = modifier
    ) {
        Column {
            Text(text = labelText)
            if (assignedAction != null) {
                Text(
                    text = assignedAction.displayName,
                    textAlign = if (textAlignCenter) TextAlign.Center else TextAlign.Start
                )
            }
        }
    }
}
