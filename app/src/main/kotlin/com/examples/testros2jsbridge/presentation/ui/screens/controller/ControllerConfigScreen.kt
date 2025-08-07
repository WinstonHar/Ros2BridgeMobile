package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.presentation.ui.components.ControllerButton
import com.examples.testros2jsbridge.presentation.ui.components.TopicSelector

@Composable
fun ControllerConfigScreen(
    controllerButtons: List<String>,
    appActions: List<AppAction>,
    presets: List<ControllerPreset>,
    selectedPreset: String?,
    onPresetSelected: (String) -> Unit,
    onAddPreset: () -> Unit,
    onRemovePreset: () -> Unit,
    onSavePreset: (String) -> Unit,
    onControllerButtonAssign: (String, AppAction?) -> Unit,
) {
    // State: map of button name to assigned AppAction
    var buttonAssignments by remember { mutableStateOf<Map<String, AppAction?>>(emptyMap()) }
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
        Text(text = "Connected controllers:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        // Controller Buttons List
        Text(text = "Controller Buttons", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            controllerButtons.forEach { btn ->
                val assigned = buttonAssignments[btn]
                ControllerButton(
                    label = { Text(btn) },
                    assignedAction = assigned,
                    onPress = { onControllerButtonAssign(btn, assigned) },
                    onRelease = {},
                    modifier = Modifier,
                    labelText = btn
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
                val assigned = buttonAssignments[btn]
                ControllerButton(
                    label = { Text(btn) },
                    assignedAction = assigned,
                    onPress = { selectedButton = btn },
                    onRelease = {},
                    modifier = Modifier,
                    labelText = btn
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TopicSelector(
            topics = appActions,
            selectedTopic = selectedButton?.let { buttonAssignments[it] },
            onTopicSelected = { action ->
                selectedButton?.let { btn ->
                    buttonAssignments = buttonAssignments.toMutableMap().apply { put(btn, action) }
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
                        text = { Text(preset.name) },
                        onClick = { onPresetSelected(preset.name) }
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
                val assigned = buttonAssignments[btn]
                TopicSelector(
                    topics = appActions,
                    selectedTopic = assigned,
                    onTopicSelected = { action ->
                        buttonAssignments = buttonAssignments.toMutableMap().apply { put(btn, action) }
                        onControllerButtonAssign(btn, action)
                    },
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
}