package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.presentation.ui.components.TopicSelector
import com.examples.testros2jsbridge.presentation.ui.screens.destinations.ControllerConfigScreenDestination
import com.examples.testros2jsbridge.util.sanitizeConfigName
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun ControllerScreen(
    viewModel: ControllerViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    val uiState by viewModel.uiState.collectAsState()
    val appActions by viewModel.appActions.collectAsState()
    val context = LocalContext.current

    // Ensure custom protocol actions are loaded and merged
    LaunchedEffect(context) {
        viewModel.loadCustomAppActions(context)
    }

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

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Controllers", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { navigator.popBackStack() }) { Text("Back") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Controller Configurations",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown for selecting controller configs
                var configExpanded by remember { mutableStateOf(false) }
                var newConfigName by remember { mutableStateOf("") }

                ExposedDropdownMenuBox(
                    expanded = configExpanded,
                    onExpandedChange = {
                        configExpanded = !configExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedConfigName,
                        onValueChange = {},
                        label = { Text("Select Config") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = configExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = configExpanded,
                        onDismissRequest = { configExpanded = false }
                    ) {
                        uiState.controllerConfigs.forEach { config ->
                            DropdownMenuItem(
                                text = { Text(config.name) },
                                onClick = {
                                    configExpanded = false
                                    Logger.d("ControllerScreen", "Dropdown selected: ${config.name}")
                                    viewModel.selectControllerConfig(config.name)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("New Config") },
                            onClick = {
                                newConfigName = ""
                                configExpanded = false
                                viewModel.selectControllerConfig("New Config")
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                var configNameError by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = newConfigName,
                    onValueChange = { if (uiState.selectedConfigName == "New Config") newConfigName = it },
                    label = { Text("New Config Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.selectedConfigName == "New Config",
                    isError = configNameError
                )
                if (configNameError) {
                    LaunchedEffect(configNameError) {
                        delay(500)
                        configNameError = false
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Add, Remove, Configure buttons
                Row(modifier = Modifier.fillMaxWidth()) {
                    val isNewConfig = uiState.selectedConfigName == "New Config"

                    // Add button with error flash on disabled click, matches Remove/Configure style
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Button(
                            onClick = {
                                val sanitizedName = sanitizeConfigName(newConfigName)
                                Logger.d(
                                    "ControllerScreen",
                                    "Adding new config with name: $sanitizedName"
                                )
                                viewModel.addControllerConfig(sanitizedName, context = context)
                                newConfigName = ""
                                viewModel.selectControllerConfig(sanitizedName)
                            },
                            enabled = isNewConfig && newConfigName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("Add")
                        }
                        if (!(isNewConfig && newConfigName.isNotBlank())) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clickable(
                                        enabled = true,
                                        onClick = { configNameError = true }
                                    )
                            ) {}
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val sanitizedName = sanitizeConfigName(uiState.selectedConfigName)
                            viewModel.removeControllerConfig(sanitizedName, context = context)
                            viewModel.selectControllerConfig("New Config")
                        },
                        enabled = !isNewConfig,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val sanitizedName = sanitizeConfigName(uiState.selectedConfigName)
                            if (uiState.selectedConfigName == "New Config") {
                                Logger.d("ControllerScreen", "Navigation blocked: selectedConfigName is 'New Config'.")
                                // Optionally show a Snackbar or Toast here
                            } else {
                                viewModel.selectControllerConfig(sanitizedName)
                                Logger.d(
                                    "ControllerScreen",
                                    "Leaving ControllerScreen, selected config: $sanitizedName"
                                )
                                navigator.navigate(ControllerConfigScreenDestination(sanitizedName))
                            }
                        },
                        enabled = !isNewConfig,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Configure")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Controller Buttons", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        uiState.controllerButtons.forEach { btn ->
                            Logger.d("ControllerScreen","Button: $btn")
                            val action = uiState.buttonAssignments[btn]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = btn, modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.bodyMedium)
                                Text(text = action?.displayName ?: "(none)", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // App Actions
                Text(text = "Available App Actions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                // App Actions
                var selectedAction by remember { mutableStateOf<AppAction?>(null) }
                Logger.d(
                    "ControllerScreen",
                    "Rendering TopicSelector with appActions: ${appActions.map { it.displayName }}"
                )
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        TopicSelector(
                            topics = appActions,
                            selectedTopic = selectedAction,
                            onTopicSelected = { action: AppAction? ->
                                Logger.d("ControllerScreen", "Selected App Action: $action")
                                selectedAction = action
                                action?.let {
                                    viewModel.assignButton(
                                        it.id, it,
                                        context = context
                                    )
                                }
                             },
                            label = "Select Action"
                        )
                        // Show info for selected action
                        selectedAction?.let { action ->
                            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Text(
                                    text = "Selected App Action Details",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Topic: ${action.topic}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Type: ${action.type}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Contents: ${action.msg}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Preset Management
                Text(
                    text = "Controller Presets (ABXY)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                var selectedPresetName by remember { mutableStateOf("New Preset") }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    }
                ) {
                    OutlinedTextField(
                        value = selectedPresetName,
                        onValueChange = {},
                        label = { Text("Select Preset") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        uiState.presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedPresetName = preset.name
                                    expanded = false
                                    viewModel.selectPreset(preset.name)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("New Preset") },
                            onClick = {
                                selectedPresetName = "New Preset"
                                expanded = false
                                viewModel.selectPreset("New Preset")
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val isNewPreset = selectedPresetName == "New Preset"
                var presetName by remember(selectedPresetName, uiState.presets) {
                    mutableStateOf(
                        if (selectedPresetName == "New Preset") "" else selectedPresetName
                    )
                }
                var presetNameError by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedPresetName = presetName
                            },
                            enabled = isNewPreset && presetName.isNotBlank() && uiState.presets.none { it.name == presetName },
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("Add")
                        }
                        if (!(isNewPreset && presetName.isNotBlank() && uiState.presets.none { it.name == presetName })) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clickable(
                                        enabled = true,
                                        onClick = { presetNameError = true }
                                    )
                            ) {}
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.removePreset(
                                context = context
                            )
                        },
                        enabled = !isNewPreset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.savePreset(selectedPresetName)
                        },
                        enabled = !isNewPreset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = presetName,
                    onValueChange = {
                        if (isNewPreset) presetName = it
                    },
                    label = { Text("Preset Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isNewPreset,
                    isError = presetNameError
                )
                if (presetNameError) {
                    LaunchedEffect(presetNameError) {
                        delay(500)
                        presetNameError = false
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // ABXY Dropdowns
                Column(modifier = Modifier.fillMaxWidth()) {
                    listOf("A", "B", "X", "Y").forEach { btn ->
                        Text(text = "$btn:", modifier = Modifier.padding(end = 4.dp))
                        TopicSelector(
                            topics = uiState.appActions,
                            selectedTopic = uiState.buttonAssignments[btn],
                            onTopicSelected = { action ->
                                viewModel.assignAbxyButton(btn, action?.displayName ?: "", context = context)
                            },
                            label = "$btn Button Action"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Logger.d("ControllerScreen", "Finished rendering ABXY buttons")
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
                            importLauncher.launch(
                                arrayOf(
                                    "application/x-yaml",
                                    "text/yaml",
                                    "text/plain",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import Config")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


