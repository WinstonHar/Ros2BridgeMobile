package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.presentation.state.PublisherUiState
import com.examples.testros2jsbridge.presentation.mapper.PublisherUiMapper
import com.examples.testros2jsbridge.domain.model.Publisher
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PublisherScreen(
    viewModel: PublisherViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val publisher = uiState.selectedPublisher
    val pubUi = publisher?.let { PublisherUiMapper.toUiModel(it) }

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
        Text(text = "Publisher Details", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (pubUi == null) {
            Text("No publisher selected.")
            Button(onClick = onBack) { Text("Back") }
        } else {
            Text("Label: ${pubUi.label}", style = MaterialTheme.typography.titleMedium)
            Text("Enabled: ${pubUi.isEnabled}", style = MaterialTheme.typography.bodySmall)
            pubUi.lastPublished?.let {
                Text("Last Published: $it", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.publishMessage() }, enabled = pubUi.isEnabled) {
                Text("Publish Message")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}
}