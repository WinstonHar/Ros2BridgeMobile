package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.presentation.mapper.PublisherUiMapper

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