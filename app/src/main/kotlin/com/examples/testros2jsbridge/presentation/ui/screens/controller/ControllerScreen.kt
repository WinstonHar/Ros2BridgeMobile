package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import com.examples.testros2jsbridge.presentation.ui.components.ControllerButton
import com.examples.testros2jsbridge.presentation.ui.components.TopicSelector
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ControllerScreen(
    viewModel: ControllerViewModel = hiltViewModel(),
    onNavigateToConfig: (ControllerPreset) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // SAF launchers for export/import
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-yaml"),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.openOutputStream(uri)?.let { out ->
                    viewModel.exportConfig(out)
                }
            }
        }
    )
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.openInputStream(uri)?.let { inp ->
                    viewModel.importConfig(inp)
                }
            }
        }
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Controllers", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        // List of available controllers (presets)
        uiState.presets.forEach { preset ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = preset.name, modifier = Modifier.weight(1f))
                Button(onClick = { onNavigateToConfig(preset) }) {
                    Text("Configure")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Controller Buttons
        Text(text = "Controller Buttons", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            uiState.controllerButtons.forEach { btn ->
                val assigned = uiState.buttonAssignments[btn]
                ControllerButton(
                    label = { Text(btn) },
                    assignedAction = assigned,
                    onPress = { viewModel.assignButton(btn, assigned) },
                    onRelease = onBack,
                    modifier = Modifier,
                    labelText = btn
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // App Actions
        Text(text = "Available App Actions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        var selectedButton by remember { mutableStateOf<String?>(null) }
        Row(modifier = Modifier.fillMaxWidth()) {
            uiState.controllerButtons.forEach { btn ->
                val assigned = uiState.buttonAssignments[btn]
                ControllerButton(
                    label = { Text(btn) },
                    assignedAction = assigned,
                    onPress = { viewModel.assignButton(btn, assigned) },
                    onRelease = onBack,
                    modifier = Modifier,
                    labelText = btn
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TopicSelector(
            topics = uiState.appActions,
            selectedTopic = selectedButton?.let { uiState.buttonAssignments[it] },
            onTopicSelected = { action ->
                selectedButton?.let { btn ->
                    viewModel.assignButton(btn, action)
                }
            },
            label = "App Action"
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Preset Management
        Text(text = "Controller Presets (ABXY)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DropdownMenu(
                expanded = false,
                onDismissRequest = onBack,
                modifier = Modifier.weight(1f)
            ) {
                uiState.presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.name) },
                        onClick = { viewModel.selectPreset(preset.name) }
                    )
                }
            }
            Button(onClick = { viewModel.addPreset() }) { Text("Add") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.removePreset() }) { Text("Remove") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.selectedPreset?.name ?: "",
            onValueChange = { viewModel.savePreset(it) },
            label = { Text("Preset Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("A", "B", "X", "Y").forEach { btn ->
                Text(text = "$btn:", modifier = Modifier.padding(end = 4.dp))
                TopicSelector(
                    topics = uiState.appActions,
                    selectedTopic = uiState.buttonAssignments[btn],
                    onTopicSelected = { action ->
                        viewModel.assignAbxyButton(btn, action?.displayName ?: "")
                    },
                    label = "$btn Button Action"
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.savePreset(uiState.selectedPreset?.name ?: "") }, modifier = Modifier.align(
            Alignment.End)) {
            Text("Save Preset")
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Export/Import Config
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { exportLauncher.launch("controller_config.yaml") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Export Config")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    importLauncher.launch(arrayOf("application/x-yaml", "text/yaml", "text/plain", "application/octet-stream", "*/*"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Import Config")
            }
        }
    }
}