package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.presentation.ui.components.ControllerButton
import com.examples.testros2jsbridge.presentation.ui.components.TopicSelector

@Composable
fun ControllerConfigScreen(
    controllerButtons: List<String>,
    appActions: List<String>,
    presets: List<String>,
    selectedPreset: String?,
    onPresetSelected: (String) -> Unit,
    onAddPreset: () -> Unit,
    onRemovePreset: () -> Unit,
    onSavePreset: (String) -> Unit,
    onControllerButtonAssign: (String, String) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Connected controllers:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        // Controller Buttons List
        Text(text = "Controller Buttons", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            controllerButtons.forEach { btn ->
                ControllerButton(
                    buttonName = btn,
                    onClick = { onControllerButtonAssign(btn, "") }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // App Actions Panel
        Text(text = "Available App Actions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        var selectedButton by remember { mutableStateOf<String?>(null) }
        Row(modifier = Modifier.fillMaxWidth()) {
            controllerButtons.forEach { btn ->
                ControllerButton(
                    buttonName = btn,
                    onClick = { selectedButton = btn }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TopicSelector(
            topics = appActions,
            selectedTopic = null,
            onTopicSelected = { action ->
                selectedButton?.let { btn ->
                    onControllerButtonAssign(btn, action)
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
                onDismissRequest = {},
                modifier = Modifier.weight(1f)
            ) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset) },
                        onClick = { onPresetSelected(preset) }
                    )
                }
            }
            Button(onClick = onAddPreset) { Text("Add") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onRemovePreset) { Text("Remove") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = selectedPreset ?: "",
            onValueChange = { onSavePreset(it) },
            label = { Text("Preset Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("A", "B", "X", "Y").forEach { btn ->
                Text(text = "$btn:", modifier = Modifier.padding(end = 4.dp))
                TopicSelector(
                    topics = appActions,
                    selectedTopic = null,
                    onTopicSelected = { action -> onControllerButtonAssign(btn, action) },
                    label = "$btn Button Action"
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onSavePreset(selectedPreset ?: "") }, modifier = Modifier.align(Alignment.End)) {
            Text("Save Preset")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}