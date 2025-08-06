package com.examples.testros2jsbridge.presentation.ui.screens.subscriber

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.presentation.state.SubscriberUiState
import com.examples.testros2jsbridge.domain.model.Subscriber
import com.examples.testros2jsbridge.presentation.ui.components.CollapsibleMessageHistoryList

@Composable
fun SubscriberScreen(
    viewModel: SubscriberViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val subscribers = uiState.subscribers
    val selectedSubscriber = uiState.selectedSubscriber
    val messageHistory = uiState.messageHistory

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Subscribers", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onBack) { Text("Back") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.showAddSubscriberDialog(true) }) {
            Text("Add Subscriber")
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(subscribers) { subscriber ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            subscriber.label?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
                            Text(text = "${subscriber.topic.value} (${subscriber.type})", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Last: ${subscriber.lastMessage ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            Button(onClick = { viewModel.unsubscribeFromTopic(subscriber) }) {
                                Text("Unsubscribe")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { viewModel.selectSubscriber(subscriber) }) {
                                Text("Select")
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        CollapsibleMessageHistoryList(messageHistory = messageHistory)
    }

    if (uiState.showAddSubscriberDialog) {
        AddSubscriberDialog(viewModel = viewModel)
    }
    if (uiState.showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissErrorDialog() },
            confirmButton = {
                Button(onClick = { viewModel.dismissErrorDialog() }) { Text("OK") }
            },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage ?: "Unknown error") }
        )
    }
}

@Composable
fun AddSubscriberDialog(viewModel: SubscriberViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    AlertDialog(
        onDismissRequest = { viewModel.showAddSubscriberDialog(false) },
        confirmButton = {
            Button(onClick = { viewModel.subscribeToTopic() }) { Text("Subscribe") }
        },
        dismissButton = {
            Button(onClick = { viewModel.showAddSubscriberDialog(false) }) { Text("Cancel") }
        },
        title = { Text("Add Subscriber") },
        text = {
            Column {
                OutlinedTextField(
                    value = uiState.topicInput,
                    onValueChange = { viewModel.updateTopicInput(it) },
                    label = { Text("Topic") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.typeInput,
                    onValueChange = { viewModel.updateTypeInput(it) },
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.labelInput,
                    onValueChange = { viewModel.updateLabelInput(it) },
                    label = { Text("Label (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}