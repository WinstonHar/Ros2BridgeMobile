package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.presentation.ui.components.ControllerButton
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun keyCodeToButtonName(keyCode: Int): String? = when (keyCode) {
    android.view.KeyEvent.KEYCODE_BUTTON_A -> "Button A"
    android.view.KeyEvent.KEYCODE_BUTTON_B -> "Button B"
    android.view.KeyEvent.KEYCODE_BUTTON_X -> "Button X"
    android.view.KeyEvent.KEYCODE_BUTTON_Y -> "Button Y"
    android.view.KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
    android.view.KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
    android.view.KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
    android.view.KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
    android.view.KeyEvent.KEYCODE_BUTTON_START -> "Start"
    android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
    else -> null
}

data class ControllerOverviewScreenNavArgs(
    val selectedConfigName: String
)

@OptIn(ExperimentalComposeUiApi::class)
@Destination(navArgsDelegate = ControllerOverviewScreenNavArgs::class)
@Composable
fun ControllerOverviewScreen(
    viewModel: ControllerViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val showPresetsOverlay by viewModel.showPresetsOverlay.collectAsState()
    val overlayHideJob = remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Action Details") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton({ showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    LaunchedEffect(uiState.config.buttonAssignments, selectedPreset, presets) {
        Logger.d("ControllerOverviewScreen", "buttonAssignments: ${uiState.config.buttonAssignments}")
        Logger.d("ControllerOverviewScreen", "presets: ${presets.joinToString { it.name }}")
        Logger.d("ControllerOverviewScreen", "selectedPreset: ${selectedPreset?.name}")
    }
    val focusRequester = remember { FocusRequester() }
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                viewModel.handleControllerInputUseCase.handleKeyEvent(keyEvent.nativeKeyEvent.keyCode, uiState.config.buttonAssignments)?.let {
                    viewModel.triggerAppAction(it)
                }
                true
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("L1", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = uiState.config.buttonAssignments["L1"]?.displayName ?: "<none>",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            uiState.config.buttonAssignments["L1"]?.let {
                                dialogMessage = it.msg
                                showDialog = true
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("L2", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = uiState.config.buttonAssignments["L2"]?.displayName ?: "<none>",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            uiState.config.buttonAssignments["L2"]?.let {
                                dialogMessage = it.msg
                                showDialog = true
                            }
                        }
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("R1", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = uiState.config.buttonAssignments["R1"]?.displayName ?: "<none>",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            uiState.config.buttonAssignments["R1"]?.let {
                                dialogMessage = it.msg
                                showDialog = true
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("R2", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = uiState.config.buttonAssignments["R2"]?.displayName ?: "<none>",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            uiState.config.buttonAssignments["R2"]?.let {
                                dialogMessage = it.msg
                                showDialog = true
                            }
                        }
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Left Joystick", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = uiState.config.buttonAssignments["LeftJoystick"]?.displayName ?: "<none>",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            uiState.config.buttonAssignments["LeftJoystick"]?.let {
                                dialogMessage = it.msg
                                showDialog = true
                            }
                        }
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Right Joystick", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = uiState.config.buttonAssignments["RightJoystick"]?.displayName ?: "<none>",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            uiState.config.buttonAssignments["RightJoystick"]?.let {
                                dialogMessage = it.msg
                                showDialog = true
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.config.buttonAssignments["Select"]?.displayName ?: "<none>",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            uiState.config.buttonAssignments["Select"]?.let {
                                dialogMessage = it.msg
                                showDialog = true
                            }
                        }
                    )
                    Spacer(Modifier.width(24.dp))
                    Text("Start", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.config.buttonAssignments["Start"]?.displayName ?: "<none>",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            uiState.config.buttonAssignments["Start"]?.let {
                                dialogMessage = it.msg
                                showDialog = true
                            }
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(230.dp)
                        .zIndex(1f)
                ) {
                    val buttons = listOf("Y", "X", "B", "A")
                    buttons.forEach { button ->
                        val buttonName = "Button $button"
                        val assignedAction = uiState.config.buttonAssignments[buttonName]
                        ControllerButton(
                            labelText = button,
                            assignedAction = assignedAction,
                            onPress = { assignedAction?.let { viewModel.triggerAppAction(it) } },
                            onRelease = { },
                            modifier = Modifier
                                .align(
                                    when (button) {
                                        "Y" -> Alignment.TopCenter
                                        "X" -> Alignment.CenterStart
                                        "B" -> Alignment.CenterEnd
                                        "A" -> Alignment.BottomCenter
                                        else -> Alignment.Center
                                    }
                                )
                                .offset(
                                    x = when (button) {
                                        "X" -> 32.dp
                                        "B" -> (-32).dp
                                        else -> 0.dp
                                    },
                                    y = when (button) {
                                        "Y" -> 32.dp
                                        "A" -> (-32).dp
                                        else -> 0.dp
                                    }
                                ),
                            textAlignCenter = true
                        )
                        Text(
                            text = assignedAction?.displayName ?: "<none>",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(
                                    when (button) {
                                        "Y" -> Alignment.TopCenter
                                        "X" -> Alignment.CenterStart
                                        "B" -> Alignment.CenterEnd
                                        "A" -> Alignment.BottomCenter
                                        else -> Alignment.Center
                                    }
                                )
                                .offset(
                                    x = when (button) {
                                        "X" -> 32.dp
                                        "B" -> (-32).dp
                                        else -> 0.dp
                                    },
                                    y = when (button) {
                                        "Y" -> 72.dp
                                        "A" -> (-72).dp
                                        else -> 0.dp
                                    }
                                )
                                .clickable {
                                    assignedAction?.let {
                                        dialogMessage = it.msg
                                        showDialog = true
                                    }
                                }
                        )
                    }
                }
                Text(
                    text = selectedPreset?.name ?: "No Preset Selected",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).zIndex(2f)
                )
            }

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
                                    viewModel.selectPreset(preset.name)
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

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

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
