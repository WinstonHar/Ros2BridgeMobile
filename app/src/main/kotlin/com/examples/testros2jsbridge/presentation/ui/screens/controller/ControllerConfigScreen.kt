
package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.presentation.ui.components.TopicSelector

@Composable
fun ControllerConfigScreen(
    viewModel: ControllerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val detectedControllerButtons by viewModel.detectedControllerButtons.collectAsState()
    val appActions by viewModel.appActions.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val presets by viewModel.presets.collectAsState()
    val selectedPreset = uiState.selectedPreset?.name
    val joystickMappings = uiState.config.joystickMappings
    val scrollState = rememberScrollState()
    val presetObj = presets.find { it.name == selectedPreset }
    val buttonAssignments = presetObj?.buttonAssignments ?: emptyMap()
    var presetDropdownExpanded by remember { mutableStateOf(false) }

    // Joystick mappings are now passed in and persisted via callback
    val mappings = remember(joystickMappings) { joystickMappings.toMutableList() }

    // Real controller connection status using InputDevice
    val isControllerConnected = remember {
        android.view.InputDevice.getDeviceIds().any { deviceId ->
            android.view.InputDevice.getDevice(deviceId)?.let { device ->
                val sources = device.sources
                (sources and android.view.InputDevice.SOURCE_GAMEPAD == android.view.InputDevice.SOURCE_GAMEPAD) ||
                (sources and android.view.InputDevice.SOURCE_JOYSTICK == android.view.InputDevice.SOURCE_JOYSTICK)
            } ?: false
        }
    }


    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                Text(
                    text = "Controller Button Assignments",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (detectedControllerButtons.isEmpty()) {
                    Text(
                        text = "No controller buttons detected. Connect a controller.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        detectedControllerButtons.forEach { btn ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(text = btn, modifier = Modifier.width(32.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                TopicSelector(
                                    topics = appActions,
                                    selectedTopic = buttonAssignments[btn],
                                    onTopicSelected = { action ->
                                        viewModel.assignButton(btn, action, context = context)
                                    },
                                    label = "Assign Action"
                                )
                            }
                        }
                    }
                }

                // --- Add Preset Section ---
                var newPresetName by remember { mutableStateOf("") }
                Row(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("New Preset Name") },
                        modifier = Modifier.weight(2f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newPresetName.isNotBlank() && presets.none { it.name == newPresetName }) {
                                viewModel.addPreset(newPresetName, context = context)
                                newPresetName = ""
                            }
                        },
                        enabled = newPresetName.isNotBlank() && presets.none { it.name == newPresetName },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Preset")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Joystick Mappings", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
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
                                        val updated = mappings.toMutableList()
                                            .apply { set(idx, mapping.copy(displayName = v)) }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    val updated = mappings.toMutableList().apply { removeAt(idx) }
                                    viewModel.updateJoystickMappings(updated)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove Mapping"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = mapping.topic?.value ?: "",
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList().apply {
                                            set(
                                                idx,
                                                mapping.copy(
                                                    topic = if (v.isBlank()) null else com.examples.testros2jsbridge.domain.model.RosId(
                                                        v
                                                    )
                                                )
                                            )
                                        }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Topic") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = mapping.type ?: "",
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList()
                                            .apply { set(idx, mapping.copy(type = v)) }
                                        viewModel.updateJoystickMappings(updated)
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
                                            val updated = mappings.toMutableList()
                                                .apply { set(idx, mapping.copy(axisX = it)) }
                                            viewModel.updateJoystickMappings(updated)
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
                                            val updated = mappings.toMutableList()
                                                .apply { set(idx, mapping.copy(axisY = it)) }
                                            viewModel.updateJoystickMappings(updated)
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
                                            val updated = mappings.toMutableList()
                                                .apply { set(idx, mapping.copy(max = it)) }
                                            viewModel.updateJoystickMappings(updated)
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
                                            val updated = mappings.toMutableList()
                                                .apply { set(idx, mapping.copy(step = it)) }
                                            viewModel.updateJoystickMappings(updated)
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
                                            val updated = mappings.toMutableList()
                                                .apply { set(idx, mapping.copy(deadzone = it)) }
                                            viewModel.updateJoystickMappings(updated)
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
                    viewModel.updateJoystickMappings(updated)
                }) {
                    Text("Add Joystick Mapping")
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { onBack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}