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

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image if provided
        backgroundImageRes?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            // Preset name and swap
            Text(
                text = selectedPreset?.name ?: "No Preset Selected",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // ABXY buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("A", "B", "X", "Y").forEach { btn ->
                    val assignedAction = selectedPreset?.abxy?.get(btn)
                    ControllerButton(
                        label = { Text(btn) },
                        assignedAction = assignedAction?.let { actionName ->
                            viewModel.appActions.value.find { it.displayName == actionName }
                        },
                        onPress = { onAbxyButtonClick(btn) },
                        onRelease = {},
                        modifier = Modifier.padding(8.dp),
                        labelText = btn
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Overview of all button assignments
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
                    verticalAlignment = Alignment.CenterVertically
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
                                containerColor = if (isSelected) Color(0xFF606DB4) else Color.LightGray,
                                contentColor = if (isSelected) Color.White else Color.Black
                            ),
                            modifier = Modifier.padding(end = 8.dp)
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