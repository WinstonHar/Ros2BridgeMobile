package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.domain.model.Publisher

@Composable
fun EditPublisherDialog(
    publisher: Publisher,
    onConfirm: (Publisher, originalId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var topic by remember { mutableStateOf(publisher.topic.value) }
    var type by remember { mutableStateOf(publisher.messageType) }
    var message by remember { mutableStateOf(publisher.message) }
    var label by remember { mutableStateOf(publisher.label ?: "") }
    val originalId = publisher.id?.value

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Publisher") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Message Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message Content") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    publisher.copy(
                        topic = com.examples.testros2jsbridge.domain.model.RosId(topic),
                        messageType = type,
                        message = message,
                        label = label.ifBlank { null }
                    ),
                    originalId
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
