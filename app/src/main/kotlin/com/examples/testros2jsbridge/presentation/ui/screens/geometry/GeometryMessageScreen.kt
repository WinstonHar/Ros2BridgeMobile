package com.examples.testros2jsbridge.presentation.ui.screens.geometry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.presentation.state.GeometryUiState
import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.presentation.mapper.MessageUiMapper
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GeometryMessageScreen(
    viewModel: GeometryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        Text(text = "Geometry Messages", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Topic input
        OutlinedTextField(
            value = uiState.topicInput,
            onValueChange = viewModel::updateTopicInput,
            label = { Text("Topic") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Type input
        OutlinedTextField(
            value = uiState.typeInput,
            onValueChange = viewModel::updateTypeInput,
            label = { Text("Type") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Message content input
        OutlinedTextField(
            value = uiState.messageContentInput,
            onValueChange = viewModel::updateMessageContentInput,
            label = { Text("Message Content (JSON)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 6
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { viewModel.saveMessage() }, modifier = Modifier.weight(1f)) {
                Text("Save Message")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.publishMessage() }, modifier = Modifier.weight(1f)) {
                Text("Publish Message")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.showSavedButtons) {
            Text(text = "Saved Geometry Messages", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            uiState.messages.forEach { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { viewModel.selectMessage(msg) }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = msg.label ?: "Unnamed", style = MaterialTheme.typography.bodyLarge)
                        Text(text = "Topic: ${msg.topic}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Type: ${msg.type}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Saved: ${MessageUiMapper.formatTimestamp(msg)}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Content:", style = MaterialTheme.typography.bodySmall)
                        Text(text = MessageUiMapper.formatMessageContent(msg), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (uiState.showErrorDialog && uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissErrorDialog,
                title = { Text("Error") },
                text = { Text(uiState.errorMessage ?: "") },
                confirmButton = {
                    Button(onClick = viewModel::dismissErrorDialog) { Text("OK") }
                }
            )
        }
    }
}
}