package com.examples.testros2jsbridge.presentation.ui.screens.protocol

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.presentation.state.ProtocolUiState

@Composable
fun CustomProtocolScreen(
    viewModel: ProtocolViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Custom Protocols", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Available Messages (.msg)", style = MaterialTheme.typography.titleMedium)
        uiState.availableMessages.forEach { file ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = file.name, modifier = Modifier.weight(1f))
                Button(onClick = { viewModel.importProtocols(setOf(file.name)) }) { Text("Import") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Available Services (.srv)", style = MaterialTheme.typography.titleMedium)
        uiState.availableServices.forEach { file ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = file.name, modifier = Modifier.weight(1f))
                Button(onClick = { viewModel.importProtocols(setOf(file.name)) }) { Text("Import") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Available Actions (.action)", style = MaterialTheme.typography.titleMedium)
        uiState.availableActions.forEach { file ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = file.name, modifier = Modifier.weight(1f))
                Button(onClick = { viewModel.importProtocols(setOf(file.name)) }) { Text("Import") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.importProtocols(uiState.selectedProtocols) },
            enabled = uiState.selectedProtocols.isNotEmpty()
        ) {
            Text("Import Selected Protocols")
        }

        if (uiState.isImporting) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }

        if (uiState.showErrorDialog && uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissErrorDialog,
                title = { Text("Error") },
                text = { Text(uiState.errorMessage ?: "") },
                confirmButton = {
                    Button(onClick = viewModel::dismissErrorDialog) { Text("OK") }
                }
            )
        }
    }
}