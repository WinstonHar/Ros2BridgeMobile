package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.examples.testros2jsbridge.presentation.ui.components.ControllerButton
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.domain.model.AppAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ControllerOverviewScreen(
    viewModel: ControllerViewModel = hiltViewModel(),
    backgroundImageRes: Int? = null,
    onAbxyButtonClick: (String) -> Unit = {},
    onPresetSwap: (ControllerPreset) -> Unit = {}
) {
    val selectedPreset: ControllerPreset? by viewModel.selectedPreset.collectAsState()
    val presets: List<ControllerPreset> by viewModel.presets.collectAsState()
    val buttonAssignments: Map<String, AppAction> by viewModel.buttonAssignments.collectAsState()
    var showPresetsOverlay by remember { mutableStateOf(false) }
    val overlayHideJob = remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Use BoxWithConstraints for future layout logic if needed
            // Background image if provided
            backgroundImageRes?.let {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.Center
                )
            }
            // Main layout: triggers, joysticks, ABXY plus, select/start
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
            ) {
                // L1/L2 (top left)
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("L1", style = MaterialTheme.typography.bodyMedium)
                    Text(buttonAssignments["L1"]?.displayName ?: "<none>", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("L2", style = MaterialTheme.typography.bodyMedium)
                    Text(buttonAssignments["L2"]?.displayName ?: "<none>", style = MaterialTheme.typography.bodySmall)
                }
                // R1/R2 (top right)
                Column(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("R1", style = MaterialTheme.typography.bodyMedium)
                    Text(buttonAssignments["R1"]?.displayName ?: "<none>", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("R2", style = MaterialTheme.typography.bodyMedium)
                    Text(buttonAssignments["R2"]?.displayName ?: "<none>", style = MaterialTheme.typography.bodySmall)
                }
                // Left joystick (center left)
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Left Joystick", style = MaterialTheme.typography.bodyMedium)
                    Text(buttonAssignments["LeftJoystick"]?.displayName ?: "<none>", style = MaterialTheme.typography.bodySmall)
                }
                // Right joystick (center right)
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Right Joystick", style = MaterialTheme.typography.bodyMedium)
                    Text(buttonAssignments["RightJoystick"]?.displayName ?: "<none>", style = MaterialTheme.typography.bodySmall)
                }
                // Select/Start (bottom center)
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(buttonAssignments["Select"]?.displayName ?: "<none>", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(24.dp))
                    Text("Start", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(buttonAssignments["Start"]?.displayName ?: "<none>", style = MaterialTheme.typography.bodySmall)
                }
                // ABXY plus layout (center, plus)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(230.dp)
                        .zIndex(1f)
                ) {
                    // Y (top)
                    ControllerButton(
                        labelText = "Y",
                        assignedAction = buttonAssignments["Button Y"],
                        onPress = { onAbxyButtonClick("Y") },
                        onRelease = {},
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (32).dp),
                        textAlignCenter = true
                    )
                    // X (left)
                    ControllerButton(
                        labelText = "X",
                        assignedAction = buttonAssignments["Button X"],
                        onPress = { onAbxyButtonClick("X") },
                        onRelease = {},
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (32).dp),
                        textAlignCenter = true
                    )
                    // B (right)
                    ControllerButton(
                        labelText = "B",
                        assignedAction = buttonAssignments["Button B"],
                        onPress = { onAbxyButtonClick("B") },
                        onRelease = {},
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (-32).dp),
                        textAlignCenter = true
                    )
                    // A (bottom)
                    ControllerButton(
                        labelText = "A",
                        assignedAction = buttonAssignments["Button A"],
                        onPress = { onAbxyButtonClick("A") },
                        onRelease = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-32).dp),
                        textAlignCenter = true
                    )
                    // Center label (optional, for visual reference)
                    /*Text(
                        text = "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Center)
                    )*/
                }
                // Preset name (top center)
                Text(
                    text = selectedPreset?.name ?: "No Preset Selected",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).zIndex(2f)
                )
            }
            // Overview of all button assignments (bottom left, scrollable if needed)
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).widthIn(max = 320.dp)
            ) {
                Text("Button Assignments", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    buttonAssignments.forEach { (btn, action) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = btn, modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.bodyMedium)
                            Text(text = action.displayName, modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            // Presets overlay popup (bottom anchored, only visible when swapping)
            if (showPresetsOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        presets.forEachIndexed { idx, preset ->
                            val isSelected = preset.name == selectedPreset?.name
                            Button(
                                onClick = {
                                    onPresetSwap(preset)
                                    // Hide overlay after swap
                                    overlayHideJob.value?.cancel()
                                    showPresetsOverlay = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF606DB4) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(preset.name)
                            }
                        }
                    }
                }
            }
        }
        // Show overlay when preset swap is triggered
        LaunchedEffect(selectedPreset) {
            // Only show overlay if swap was triggered
            if (showPresetsOverlay) {
                overlayHideJob.value?.cancel()
                overlayHideJob.value = coroutineScope.launch {
                    delay(1500)
                    showPresetsOverlay = false
                }
            }
        }
        // External trigger to show overlay
        LaunchedEffect(Unit) {
            // Provide a way to trigger overlay externally, e.g. via a callback or event
            // For now, expose a function to show overlay
            viewModel.showPresetsOverlay = {
                showPresetsOverlay = true
                overlayHideJob.value?.cancel()
                overlayHideJob.value = coroutineScope.launch {
                    delay(1500)
                    showPresetsOverlay = false
                }
            }
        }
    }
}