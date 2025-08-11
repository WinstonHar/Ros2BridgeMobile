package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherListScreen
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.CreatePublisherScreen
import androidx.compose.foundation.layout.Arrangement

@Composable
fun PublisherScreen(
    viewModel: PublisherViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val publisher = uiState.selectedPublisher
    val pubUi = publisher?.let { com.examples.testros2jsbridge.presentation.mapper.PublisherUiMapper.toUiModel(it) }
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Publisher", style = MaterialTheme.typography.titleLarge)
                androidx.compose.material3.Button(
                    onClick = onBack
                ) {
                    Text("Back")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Publisher List at the top (no extra padding)
            PublisherListScreen(
                viewModel = viewModel,
                onPublisherSelected = { publisher -> viewModel.selectPublisher(publisher) },
                onEditPublisher = { publisher -> viewModel.selectPublisher(publisher) },
                onDeletePublisher = { publisher -> viewModel.deletePublisher(publisher) }
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Publisher details and publish button (old logic) with padding
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = "Publisher Details", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                if (pubUi == null) {
                    Text("No publisher selected.")
                } else {
                    Text("Label: ${pubUi.label}", style = MaterialTheme.typography.titleMedium)
                    Text("Enabled: ${pubUi.isEnabled}", style = MaterialTheme.typography.bodySmall)
                    pubUi.lastPublished?.let {
                        Text("Last Published: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(onClick = { viewModel.publishMessage() }, enabled = pubUi.isEnabled) {
                        Text("Publish Message")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            // Create Publisher at the bottom (no extra padding)
            CreatePublisherScreen(
                viewModel = viewModel,
                onPublisherCreated = { viewModel.selectPublisher(it) },
                onCancel = { viewModel.clearPublisherInputFields() }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}