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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.presentation.mapper.PublisherUiMapper

@Composable
fun PublisherListScreen(
    viewModel: PublisherViewModel  = hiltViewModel(),
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