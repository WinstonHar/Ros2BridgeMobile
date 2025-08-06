package com.examples.testros2jsbridge.presentation.ui.screens.connection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examples.testros2jsbridge.presentation.ui.components.RosConnectionCard

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    RosConnectionCard(
        ipAddress = uiState.ipInput,
        port = uiState.portInput,
        isConnected = uiState.isConnected,
        connectionStatus = uiState.connectionStatus,
        onIpAddressChange = viewModel::onIpAddressChange,
        onPortChange = viewModel::onPortChange,
        onConnect = { viewModel.connect(uiState.ipInput, uiState.portInput) },
        onDisconnect = { viewModel.disconnect() }
    )
    if (!uiState.errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Error: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
    }
}