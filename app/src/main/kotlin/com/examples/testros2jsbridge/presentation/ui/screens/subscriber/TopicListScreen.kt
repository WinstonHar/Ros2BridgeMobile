package com.examples.testros2jsbridge.presentation.ui.screens.subscriber

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination

@Destination
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
            modifier = Modifier.fillMaxSize()
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