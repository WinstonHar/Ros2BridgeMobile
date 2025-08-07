package com.examples.testros2jsbridge.presentation.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingScreen(viewModel: SettingsViewModel = hiltViewModel(), onBack: () -> Unit) {
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
        Text(text = "App Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Theme selection
        Text(text = "Theme", style = MaterialTheme.typography.titleMedium)
        Row {
            RadioButton(selected = uiState.theme == "light", onClick = { viewModel.setTheme("light") })
            Text("Light", modifier = Modifier.padding(end = 8.dp))
            RadioButton(selected = uiState.theme == "dark", onClick = { viewModel.setTheme("dark") })
            Text("Dark", modifier = Modifier.padding(end = 8.dp))
            RadioButton(selected = uiState.theme == "system", onClick = { viewModel.setTheme("system") })
            Text("System")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Language selection
        Text(text = "Language", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = uiState.language,
            onValueChange = viewModel::setLanguage,
            label = { Text("Language Code") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Notifications toggle
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = uiState.notificationsEnabled, onCheckedChange = viewModel::setNotificationsEnabled)
            Text("Enable Notifications")
        }
        Spacer(modifier = Modifier.height(16.dp))


        // Auto-connect toggle (offloaded to reconnectOnFailure)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = uiState.reconnectOnFailure, onCheckedChange = { viewModel.setReconnectOnFailure(it) })
            Text("Auto-reconnect on failure")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Last connection info
        Text(text = "Last Connected IP: ${uiState.lastConnectedIp}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Last Connected Port: ${uiState.lastConnectedPort}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Save button
        Button(onClick = viewModel::saveSettings, enabled = !uiState.isSaving) {
            Text(if (uiState.isSaving) "Saving..." else "Save Settings")
        }

        // Error dialog
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
}