package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.presentation.state.PublisherUiState
import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.presentation.mapper.PublisherUiMapper
import androidx.hilt.navigation.compose.hiltViewModel

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
        Text(text = "Create Publisher", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.topicInput,
            onValueChange = { viewModel.updateTopicInput(it) },
            label = { Text("Topic Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.typeInput,
            onValueChange = { viewModel.updateTypeInput(it) },
            label = { Text("Message Type") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.messageContentInput,
            onValueChange = { viewModel.updateMessageContentInput(it) },
            label = { Text("Message Content (JSON)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(
                onClick = {
                    viewModel.createPublisher()
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