package com.examples.testros2jsbridge.presentation.ui.screens.subscriber

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.presentation.ui.components.CollapsibleMessageHistoryList

@Composable
fun SubscriberScreen(
    viewModel: SubscriberViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onRestoreTab: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val subscribers = uiState.subscribers
    val selectedSubscriber = uiState.selectedSubscriber
    val messageHistory = uiState.messageHistory

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .nestedScroll(rememberNestedScrollInteropConnection())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Subscribers", style = MaterialTheme.typography.titleLarge)
                Button(onClick = {
                    onRestoreTab?.invoke()
                    onBack()
                }) { Text("Back") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.showAddSubscriberDialog(true) }) {
                Text("Add Subscriber")
            }
            Spacer(modifier = Modifier.height(8.dp))
            // TopicListScreen is now inline and aligned with the rest of the screen
            TopicListScreen(
                viewModel = viewModel,
                onTopicSelected = { topic: String, type: String ->
                    viewModel.updateTopicInput(topic)
                    viewModel.updateTypeInput(type)
                    viewModel.showAddSubscriberDialog(true)
                },
                onSubscribe = { topic: String, type: String ->
                    viewModel.updateTopicInput(topic)
                    viewModel.updateTypeInput(type)
                    viewModel.subscribeToTopic()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
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
                                subscriber.label?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(
                                    text = "${subscriber.topic.value} (${subscriber.type})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Last: ${subscriber.lastMessage ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
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
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    CollapsibleMessageHistoryList(messageHistory = messageHistory)
                }
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