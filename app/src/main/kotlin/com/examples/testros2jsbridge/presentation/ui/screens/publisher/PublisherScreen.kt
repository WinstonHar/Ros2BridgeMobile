package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.layout.Arrangement
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
import com.examples.testros2jsbridge.core.util.Logger


@Composable
fun PublisherScreen(
    viewModel: PublisherViewModel = hiltViewModel(),
    rosBridgeViewModel: com.examples.testros2jsbridge.core.ros.RosBridgeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
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
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Publisher", style = MaterialTheme.typography.titleLarge)
                    androidx.compose.material3.Button(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                PublisherListScreen(
                    viewModel = viewModel,
                    onPublisherSelected = { publisher -> viewModel.selectPublisher(publisher) },
                    onEditPublisher = { publisher -> viewModel.updatePublisher(publisher) },
                    onDeletePublisher = { publisher -> viewModel.deletePublisher(publisher) }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Column {
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
                        androidx.compose.material3.Button(onClick = { viewModel.publishMessage(rosBridgeViewModel); Logger.d("PublisherScreen", "Publish button clicked")}, enabled = pubUi.isEnabled) {
                            Text("Publish Message")
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
            item {
                val protocolViewModel: ProtocolViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                protocolViewModel.rosBridgeViewModel = rosBridgeViewModel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    CustomProtocolScreen(
                        viewModel = protocolViewModel,
                        onBack = onBack
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}