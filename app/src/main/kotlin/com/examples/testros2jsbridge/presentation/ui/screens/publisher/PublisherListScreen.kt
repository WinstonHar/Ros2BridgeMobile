package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.presentation.state.PublisherUiState
import com.examples.testros2jsbridge.presentation.mapper.PublisherUiMapper
import com.examples.testros2jsbridge.domain.model.Publisher

@Composable
fun PublisherListScreen(
    viewModel: PublisherViewModel,
    onPublisherSelected: (Publisher) -> Unit,
    onEditPublisher: (Publisher) -> Unit,
    onDeletePublisher: (Publisher) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val publishers = uiState.publishers.map { PublisherUiMapper.toUiModel(it) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Publishers", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (publishers.isEmpty()) {
            Text("No publishers found.")
        } else {
            publishers.forEach { pubUi ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { uiState.publishers.find { it.id?.value == pubUi.id }?.let(onPublisherSelected) }
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(pubUi.label, style = MaterialTheme.typography.titleMedium)
                            Text("Enabled: ${pubUi.isEnabled}", style = MaterialTheme.typography.bodySmall)
                            pubUi.lastPublished?.let {
                                Text("Last Published: $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                uiState.publishers.find { it.id?.value == pubUi.id }?.let(onEditPublisher)
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                uiState.publishers.find { it.id?.value == pubUi.id }?.let(onDeletePublisher)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}