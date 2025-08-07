package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.ControllerPreset

import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.presentation.ui.components.TopicSelector

@Composable
fun ControllerConfigScreen(
    controllerButtons: List<String>,
    appActions: List<AppAction>,
    presets: List<ControllerPreset>,
    selectedPreset: String?,
    joystickMappings: List<JoystickMapping>,
    onPresetSelected: (String) -> Unit,
    onAddPreset: () -> Unit,
    onRemovePreset: () -> Unit,
    onSavePreset: (String) -> Unit,
    onControllerButtonAssign: (String, AppAction?) -> Unit,
    onJoystickMappingsChanged: (List<JoystickMapping>) -> Unit,
) {
    val presetObj = presets.find { it.name == selectedPreset }
    var buttonAssignments by remember(selectedPreset, presets) {
        mutableStateOf(
            controllerButtons.associateWith { btn ->
                presetObj?.buttonAssignments?.get(btn)
            }
        )
    }
    var presetDropdownExpanded by remember { mutableStateOf(false) }

    // Joystick mappings are now passed in and persisted via callback
    val mappings = remember(joystickMappings) { joystickMappings.toMutableList() }

    // Simulate controller connection status (replace with real status if available)
    val isControllerConnected = controllerButtons.isNotEmpty()

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
            // Controller connection status
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Controllers",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isControllerConnected) "Connected" else "Not Connected",
                    color = if (isControllerConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Controller Button Assignments", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            // Always show all controller buttons as a vertical list with assignment selectors
            if (controllerButtons.isEmpty()) {
                Text("No controller buttons detected. Connect a controller.", color = MaterialTheme.colorScheme.error)
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    controllerButtons.forEach { btn ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(text = btn, modifier = Modifier.width(32.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            TopicSelector(
                                topics = appActions,
                                selectedTopic = buttonAssignments[btn],
                                onTopicSelected = { action ->
                                    buttonAssignments = buttonAssignments.toMutableMap().apply { put(btn, action) }
                                    onControllerButtonAssign(btn, action)
                                },
                                label = "Assign Action"
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Controller Preset (ABXY) Assignments", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            // ABXY assignment section
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                listOf("A", "B", "X", "Y").forEach { btn ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = btn)
                        TopicSelector(
                            topics = appActions,
                            selectedTopic = buttonAssignments[btn],
                            onTopicSelected = { action ->
                                buttonAssignments = buttonAssignments.toMutableMap().apply { put(btn, action) }
                                onControllerButtonAssign(btn, action)
                            },
                            label = "$btn Action"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(modifier = Modifier.height(16.dp))

            // Preset Management
            Text(text = "Controller Presets", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(onClick = { presetDropdownExpanded = true }) {
                        Text(selectedPreset ?: "Select Preset")
                    }
                    DropdownMenu(
                        expanded = presetDropdownExpanded,
                        onDismissRequest = { presetDropdownExpanded = false },
                    ) {
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    onPresetSelected(preset.name)
                                    presetDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onSavePreset(selectedPreset ?: "") }, modifier = Modifier.align(Alignment.End)) {
                Text("Save Preset")
            }
            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(modifier = Modifier.height(16.dp))

            // Joystick Mapping Config Section
            Text(text = "Joystick Mappings", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                mappings.forEachIndexed { idx, mapping ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = mapping.displayName,
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList().apply { set(idx, mapping.copy(displayName = v)) }
                                        onJoystickMappingsChanged(updated)
                                    },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    val updated = mappings.toMutableList().apply { removeAt(idx) }
                                    onJoystickMappingsChanged(updated)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Mapping")
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = mapping.topic?.value ?: "",
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList().apply { set(idx, mapping.copy(topic = if (v.isBlank()) null else com.examples.testros2jsbridge.domain.model.RosId(v))) }
                                        onJoystickMappingsChanged(updated)
                                    },
                                    label = { Text("Topic") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = mapping.type ?: "",
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList().apply { set(idx, mapping.copy(type = v)) }
                                        onJoystickMappingsChanged(updated)
                                    },
                                    label = { Text("Type") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = mapping.axisX.toString(),
                                    onValueChange = { v: String ->
                                        v.toIntOrNull()?.let {
                                            val updated = mappings.toMutableList().apply { set(idx, mapping.copy(axisX = it)) }
                                            onJoystickMappingsChanged(updated)
                                        }
                                    },
                                    label = { Text("Axis X") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = mapping.axisY.toString(),
                                    onValueChange = { v: String ->
                                        v.toIntOrNull()?.let {
                                            val updated = mappings.toMutableList().apply { set(idx, mapping.copy(axisY = it)) }
                                            onJoystickMappingsChanged(updated)
                                        }
                                    },
                                    label = { Text("Axis Y") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = mapping.max?.toString() ?: "",
                                    onValueChange = { v: String ->
                                        v.toFloatOrNull()?.let {
                                            val updated = mappings.toMutableList().apply { set(idx, mapping.copy(max = it)) }
                                            onJoystickMappingsChanged(updated)
                                        }
                                    },
                                    label = { Text("Max") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = mapping.step?.toString() ?: "",
                                    onValueChange = { v: String ->
                                        v.toFloatOrNull()?.let {
                                            val updated = mappings.toMutableList().apply { set(idx, mapping.copy(step = it)) }
                                            onJoystickMappingsChanged(updated)
                                        }
                                    },
                                    label = { Text("Step") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = mapping.deadzone?.toString() ?: "",
                                    onValueChange = { v: String ->
                                        v.toFloatOrNull()?.let {
                                            val updated = mappings.toMutableList().apply { set(idx, mapping.copy(deadzone = it)) }
                                            onJoystickMappingsChanged(updated)
                                        }
                                    },
                                    label = { Text("Deadzone") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    val updated = mappings.toMutableList().apply {
                        add(
                            JoystickMapping(
                                displayName = "New Mapping",
                                topic = null,
                                type = "",
                                axisX = 0,
                                axisY = 1,
                                max = 1.0f,
                                step = 0.2f,
                                deadzone = 0.1f
                            )
                        )
                    }
                    onJoystickMappingsChanged(updated)
                }) {
                    Text("Add Joystick Mapping")
                }
            }
        }
    }

}

