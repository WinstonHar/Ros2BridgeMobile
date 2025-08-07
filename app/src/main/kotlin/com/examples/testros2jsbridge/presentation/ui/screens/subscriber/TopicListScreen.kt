package com.examples.testros2jsbridge.presentation.ui.screens.subscriber

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TopicListScreen(
    viewModel: SubscriberViewModel = hiltViewModel(),
    onTopicSelected: (String, String) -> Unit,
    onSubscribe: (String, String) -> Unit
) {
    val availableTopics by viewModel.availableTopics.collectAsState()
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
        Text(text = "Available Topics", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.fetchAvailableTopics() }) {
            Text("Refresh Topics")
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (availableTopics.isEmpty()) {
            Text("No topics found.", style = MaterialTheme.typography.bodyMedium)
        } else {
            availableTopics.forEach { (topic, type) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onTopicSelected(topic, type) },
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = topic, style = MaterialTheme.typography.bodyLarge)
                            Text(text = type, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { onSubscribe(topic, type) }) {
                            Text("Subscribe")
                        }
                    }
                }
            }
        }
    }
}
}