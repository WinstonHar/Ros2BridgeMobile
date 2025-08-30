package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.EditPublisherDialog

@Composable
fun PublisherListScreen(
    viewModel: PublisherViewModel  = hiltViewModel(),
    onPublisherSelected: (Publisher) -> Unit,
    onEditPublisher: (Publisher) -> Unit,
    onDeletePublisher: (Publisher) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val publishers = uiState.publishers
    var editingPublisher by remember { mutableStateOf<Publisher?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        if (publishers.isEmpty()) {
            Text("No publishers found.")
        } else {
            publishers.forEach{ pubUi ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onPublisherSelected(pubUi) }
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pubUi.label ?: pubUi.topic.value, style = MaterialTheme.typography.titleMedium)
                            Text("Topic: ${pubUi.topic}", style = MaterialTheme.typography.bodySmall)
                            Text("Type: ${pubUi.messageType}", style = MaterialTheme.typography.bodySmall)
                            pubUi.lastPublishedTimestamp?.let {
                                Text("Last Published: $it", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Message JSON:", style = MaterialTheme.typography.bodySmall)
                            Text(pubUi.message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                        }
                        Row {
                            IconButton(onClick = {
                                editingPublisher = pubUi
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                onDeletePublisher(pubUi)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }

        // Show edit dialog if editingPublisher is set
        editingPublisher?.let { publisher ->
            EditPublisherDialog(
                publisher = publisher,
                onConfirm = { updated, originalId ->
                    // Pass both updated publisher and originalId
                    onEditPublisher(updated.copy(id = originalId?.let { com.examples.testros2jsbridge.domain.model.RosId(it) }))
                    editingPublisher = null
                },
                onDismiss = { editingPublisher = null }
            )
        }
    }
}