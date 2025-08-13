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
import androidx.compose.ui.input.pointer.pointerInteropFilter
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
import com.examples.testros2jsbridge.core.util.Logger

fun keyCodeToButtonName(keyCode: Int): String? = when (keyCode) {
    android.view.KeyEvent.KEYCODE_BUTTON_A -> "A"
    android.view.KeyEvent.KEYCODE_BUTTON_B -> "B"
    android.view.KeyEvent.KEYCODE_BUTTON_X -> "X"
    android.view.KeyEvent.KEYCODE_BUTTON_Y -> "Y"
    android.view.KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
    android.view.KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
    android.view.KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
    android.view.KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
    android.view.KeyEvent.KEYCODE_BUTTON_START -> "Start"
    android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
    else -> null
}

@Composable
fun ControllerOverviewScreen(
    viewModel: ControllerViewModel = hiltViewModel(),
    selectedConfigName: String,
    backgroundImageRes: Int? = null,
    onAbxyButtonClick: (String) -> Unit = {},
    onPresetSwap: (ControllerPreset) -> Unit = {}
) {
    LaunchedEffect(selectedConfigName) {
        viewModel.selectControllerConfig(selectedConfigName)
    }

    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.controllerConfigs.find { it.name == selectedConfigName }
    val buttonAssignments = config?.buttonAssignments ?: emptyMap()
    val selectedPreset: ControllerPreset? by viewModel.selectedPreset.collectAsState()
    val presets: List<ControllerPreset> by viewModel.presets.collectAsState()
    val showPresetsOverlay by viewModel.showPresetsOverlay.collectAsState()
    val overlayHideJob = remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedConfigName, config, selectedPreset, presets) {
        Logger.d("ControllerOverviewScreen", "selectedConfigName: $selectedConfigName")
        Logger.d("ControllerOverviewScreen", "config: ${config?.name} (exists: ${config != null})")
        Logger.d("ControllerOverviewScreen", "presets: ${presets.joinToString { it.name }}")
        Logger.d("ControllerOverviewScreen", "selectedPreset: ${selectedPreset?.name}")
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .pointerInteropFilter { event: android.view.InputEvent ->
                    when (event) {
                        is android.view.KeyEvent -> {
                            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                                // Use existing handleKeyEvent logic from HandleControllerInputUseCase
                                val action = viewModel.handleControllerInputUseCase.handleKeyEvent(event.keyCode, buttonAssignments)
                                if (action != null) {
                                    viewModel.triggerAppAction(action)
                                }
                            }
                            true
                        }
                        is android.view.MotionEvent -> {
                            if (event.source and android.view.InputDevice.SOURCE_JOYSTICK == android.view.InputDevice.SOURCE_JOYSTICK &&
                                event.action == android.view.MotionEvent.ACTION_MOVE) {
                                // For each joystick mapping, extract axes and publish
                                config?.joystickMappings?.forEach { mapping ->
                                    val device = event.device ?: return@forEach
                                    val x = event.getAxisValue(mapping.axisX)
                                    val y = event.getAxisValue(mapping.axisY)
                                    val (qx, qy) = viewModel.handleControllerInputUseCase.handleJoystickInput(x, y, mapping)
                                    val msgJson = when (mapping.type) {
                                        "geometry_msgs/msg/Twist" ->
                                            "{" +
                                                "\"linear\": {\"x\": $qx, \"y\": 0.0, \"z\": 0.0}, " +
                                                "\"angular\": {\"x\": 0.0, \"y\": 0.0, \"z\": $qy}" +
                                            "}"
                                        "geometry_msgs/msg/TwistStamped" -> {
                                            val now = System.currentTimeMillis()
                                            val secs = now / 1000
                                            val nsecs = (now % 1000) * 1_000_000
                                            val twist = "{\"linear\": {\"x\": $qx, \"y\": 0.0, \"z\": 0.0}, " +
                                                "\"angular\": {\"x\": 0.0, \"y\": 0.0, \"z\": $qy}}"
                                            "{" +
                                                "\"header\": {\"stamp\": {\"sec\": $secs, \"nanosec\": $nsecs}, \"frame_id\": \"\"}, " +
                                                "\"twist\": $twist" +
                                            "}"
                                        }
                                        else ->
                                            // Fallback: just send x/y as fields
                                            "{" +
                                                "\"x\": $qx, \"y\": $qy" +
                                            "}"
                                    }
                                    val topic = mapping.topic?.value ?: return@forEach
                                    val type = mapping.type ?: return@forEach
                                    val action = com.examples.testros2jsbridge.domain.model.AppAction(
                                        id = "joystick_${mapping.displayName}",
                                        displayName = mapping.displayName,
                                        topic = topic,
                                        type = type,
                                        source = "joystick",
                                        msg = msgJson
                                    )
                                    viewModel.triggerAppAction(action)
                                }
                                true
                            } else false
                        }
                        else -> false
                    }
                }
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
                        assignedAction = buttonAssignments["Y"],
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
                        assignedAction = buttonAssignments["X"],
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
                        assignedAction = buttonAssignments["B"],
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
                        assignedAction = buttonAssignments["A"],
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
                                    overlayHideJob.value?.cancel()
                                    viewModel.hidePresetsOverlay()
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
    //
        LaunchedEffect(showPresetsOverlay) {
            if (showPresetsOverlay) {
                overlayHideJob.value?.cancel()
                overlayHideJob.value = coroutineScope.launch {
                    delay(1500)
                    viewModel.hidePresetsOverlay()
                }
            }
        }
    }
}