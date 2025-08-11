package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.domain.model.Publisher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePublisherScreen(
    viewModel: PublisherViewModel  = hiltViewModel(),
    onPublisherCreated: (Publisher) -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showAddPublisherDialog(false) },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage ?: "Unknown error") },
            confirmButton = {
                Button(onClick = { viewModel.showAddPublisherDialog(false) }) {
                    Text("OK")
                }
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Create Standard App Action", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.topicInput,
            onValueChange = { viewModel.updateTopicInput(it) },
            label = { Text("Topic Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        val messageTypes = listOf(
            "Bool", "Byte", "Char", "ColorRGBA", "Empty", "Float32", "Float64", "Header",
            "Int16", "Int32", "Int64", "Int8", "String", "UInt16", "UInt32", "UInt64", "UInt8"
        )
        val expanded = remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded.value,
            onExpandedChange = { expanded.value = !expanded.value }
        ) {
            OutlinedTextField(
                value = uiState.typeInput,
                onValueChange = { }, // No manual editing
                readOnly = true,
                label = { Text("Message Type") },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) }
            )
            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                messageTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            viewModel.updateTypeInput(type)
                            expanded.value = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.messageContentInput,
            onValueChange = { viewModel.updateMessageContentInput(it) },
            label = { Text("Message Content") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(
                onClick = {
                    viewModel.createStandardPublisher()
                    uiState.selectedPublisher?.let { onPublisherCreated(it) }
                },
                enabled = !uiState.isSaving
            ) {
                Text("Save")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}