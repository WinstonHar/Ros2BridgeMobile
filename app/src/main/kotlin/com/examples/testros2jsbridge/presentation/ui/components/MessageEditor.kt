package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageEditor(
    initialMessage: String = "",
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onChange: ((String) -> Unit)? = null
) {
    var message by remember { mutableStateOf(initialMessage) }
    var isEditing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = message,
            onValueChange = {
                message = it
                isEditing = true
                onChange?.invoke(it)
            },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(
                onClick = {
                    onSave(message)
                    isEditing = false
                },
                enabled = isEditing
            ) {
                Text("Save")
            }
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onDelete()
                        message = ""
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}