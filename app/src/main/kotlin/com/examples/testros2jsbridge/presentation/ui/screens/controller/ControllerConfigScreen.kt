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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.presentation.ui.components.TopicSelector
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.examples.testros2jsbridge.domain.model.AppAction

@Destination
@Composable
fun ControllerConfigScreen(
    configName: String,
    viewModel: ControllerViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    val uiState by viewModel.uiState.collectAsState()
    val appActions by viewModel.appActions.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    uiState.selectedPreset?.name
    val joystickMappings = uiState.config.joystickMappings
    rememberScrollState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    val buttonRows = listOf(
        listOf("Button A", "Button B", "Button X", "Button Y"),
        listOf("L1", "R1", "L2", "R2"),
        listOf("Start", "Select")
    )
    buttonRows.flatten()
    val mappings = joystickMappings.toMutableList()

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Config: ${uiState.config.name}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveConfig()
                            navigator.popBackStack()
                        },
                        enabled = hasUnsavedChanges && uiState.config.name.isNotBlank(),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Save")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    buttonRows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            row.forEach { btn ->
                                Column(
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = btn,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    TopicSelector(
                                        topics = appActions,
                                        selectedTopic = uiState.config.buttonAssignments[btn],
                                        onTopicSelected = { action ->
                                            viewModel.assignButton(btn, action, context = context)
                                        },
                                        label = ""
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Joystick Mappings", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                val fixedMappings = List(2) { idx -> mappings.getOrNull(idx) ?: JoystickMapping() }
                fixedMappings.forEachIndexed { idx, mapping ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = mapping.displayName,
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList()
                                        if (idx < updated.size) {
                                            updated[idx] = mapping.copy(displayName = v)
                                        } else {
                                            updated.add(mapping.copy(displayName = v))
                                        }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = mapping.topic?.value ?: "",
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList()
                                        if (idx < updated.size) {
                                            updated[idx] = mapping.copy(topic = if (v.isBlank()) null else com.examples.testros2jsbridge.domain.model.RosId(v))
                                        } else {
                                            updated.add(mapping.copy(topic = if (v.isBlank()) null else com.examples.testros2jsbridge.domain.model.RosId(v)))
                                        }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Topic") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                TextField(
                                    value = mapping.type ?: "",
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList()
                                        if (idx < updated.size) {
                                            updated[idx] = mapping.copy(type = v)
                                        } else {
                                            updated.add(mapping.copy(type = v))
                                        }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Type") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = mapping.axisX.toString(),
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList()
                                        val axisX = v.toIntOrNull() ?: mapping.axisX
                                        if (idx < updated.size) {
                                            updated[idx] = mapping.copy(axisX = axisX)
                                        } else {
                                            updated.add(mapping.copy(axisX = axisX))
                                        }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Axis X") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                TextField(
                                    value = mapping.axisY.toString(),
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList()
                                        val axisY = v.toIntOrNull() ?: mapping.axisY
                                        if (idx < updated.size) {
                                            updated[idx] = mapping.copy(axisY = axisY)
                                        } else {
                                            updated.add(mapping.copy(axisY = axisY))
                                        }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Axis Y") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = mapping.max?.toString() ?: "",
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList()
                                        val max = v.toFloatOrNull() ?: mapping.max
                                        if (idx < updated.size) {
                                            updated[idx] = mapping.copy(max = max)
                                        } else {
                                            updated.add(mapping.copy(max = max))
                                        }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Max") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                TextField(
                                    value = mapping.step?.toString() ?: "",
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList()
                                        val step = v.toFloatOrNull() ?: mapping.step
                                        if (idx < updated.size) {
                                            updated[idx] = mapping.copy(step = step)
                                        } else {
                                            updated.add(mapping.copy(step = step))
                                        }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Step") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                TextField(
                                    value = mapping.deadzone?.toString() ?: "",
                                    onValueChange = { v: String ->
                                        val updated = mappings.toMutableList()
                                        val deadzone = v.toFloatOrNull() ?: mapping.deadzone
                                        if (idx < updated.size) {
                                            updated[idx] = mapping.copy(deadzone = deadzone)
                                        } else {
                                            updated.add(mapping.copy(deadzone = deadzone))
                                        }
                                        viewModel.updateJoystickMappings(updated)
                                    },
                                    label = { Text("Deadzone") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { navigator.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}
