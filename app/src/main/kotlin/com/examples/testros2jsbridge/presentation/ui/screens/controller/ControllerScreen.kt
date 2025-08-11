package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.presentation.ui.components.ControllerButton
import com.examples.testros2jsbridge.presentation.ui.components.TopicSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerScreen(
    viewModel: ControllerViewModel = hiltViewModel(),
    navController: NavController,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val appActions by viewModel.appActions.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(text = "Controllers", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onBack) { Text("Back") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // List of available controllers (presets)
                uiState.presets.forEach { preset ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = preset.name, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            // Navigate to ControllerConfigScreen, passing preset name as argument
                            navController.navigate("controller_config_screen/${preset.name}")
                        }) {
                            Text("Configure")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Controller Config Management
                Text(
                    text = "Controller Configurations",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown for selecting controller configs
                var configExpanded by remember { mutableStateOf(false) }
                var selectedConfigName by remember { mutableStateOf("New Config") }
                var newConfigName by remember { mutableStateOf("") }

                ExposedDropdownMenuBox(
                    expanded = configExpanded,
                    onExpandedChange = {
                        configExpanded = !configExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = selectedConfigName,
                        onValueChange = {},
                        label = { Text("Select Config") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
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
                                    selectedConfigName = config.name
                                    configExpanded = false
                                    viewModel.selectControllerConfig(config.name)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("New Config") },
                            onClick = {
                                selectedConfigName = "New Config"
                                configExpanded = false
                                viewModel.selectControllerConfig("New Config")
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Text entry for new config name (disabled if a named config is selected)
                var configNameError by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = newConfigName,
                    onValueChange = { if (selectedConfigName == "New Config") newConfigName = it },
                    label = { Text("New Config Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedConfigName == "New Config",
                    isError = configNameError
                )
                if (configNameError) {
                    LaunchedEffect(configNameError) {
                        kotlinx.coroutines.delay(500)
                        configNameError = false
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Add, Remove, Configure buttons
                Row(modifier = Modifier.fillMaxWidth()) {
                    val isNewConfig = selectedConfigName == "New Config"

                    // Add button with error flash on disabled click, matches Remove/Configure style
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.addControllerConfig(newConfigName)
                                selectedConfigName = newConfigName
                                newConfigName = ""
                            },
                            enabled = isNewConfig && newConfigName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("Add")
                        }
                        if (!(isNewConfig && newConfigName.isNotBlank())) {
                            androidx.compose.foundation.layout.Box(
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
                            viewModel.removeControllerConfig(selectedConfigName)
                            // Reset selection if the removed config was selected
                            selectedConfigName = "New Config"
                        },
                        enabled = !isNewConfig,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            navController.navigate("controller_config_screen/$selectedConfigName")
                        },
                        enabled = !isNewConfig,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Configure")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Controller Buttons
                Text(text = "Controller Buttons", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    uiState.controllerButtons.forEach { btn ->
                        val assigned = uiState.buttonAssignments[btn]
                        ControllerButton(
                            label = { Text(btn) },
                            assignedAction = assigned,
                            onPress = { viewModel.assignButton(btn, assigned) },
                            onRelease = onBack,
                            modifier = Modifier,
                            labelText = btn
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // App Actions
                Text(text = "Available App Actions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                var selectedButton by remember { mutableStateOf<String?>(null) }
                Row(modifier = Modifier.fillMaxWidth()) {
                    uiState.controllerButtons.forEach { btn ->
                        val assigned = uiState.buttonAssignments[btn]
                        ControllerButton(
                            label = { Text(btn) },
                            assignedAction = assigned,
                            onPress = { viewModel.assignButton(btn, assigned) },
                            onRelease = onBack,
                            modifier = Modifier,
                            labelText = btn
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // App Actions
                var selectedAction by remember { mutableStateOf<AppAction?>(null) }
                Logger.d("ControllerScreen", "Rendering TopicSelector with appActions: ${appActions.map { it.displayName }}")
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
                                action?.let { viewModel.assignButton(it.id, it) }
                            },
                            label = "Select Action"
                        )
                        // Show info for selected action
                        selectedAction?.let { action ->
                            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Text(text = "Selected App Action Details", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Topic: ${action.topic}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Type: ${action.type}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Contents: ${action.msg}", style = MaterialTheme.typography.bodyMedium)
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

                // Dropdown for selecting presets
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
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
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

                // Preset Name Field (move above Row for scope)
                val isNewPreset = selectedPresetName == "New Preset"
                var presetName by remember(selectedPresetName, uiState.presets) {
                    mutableStateOf(
                        if (isNewPreset) "" else uiState.presets.find { it.name == selectedPresetName }?.name ?: ""
                    )
                }
                var presetNameError by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth()) {
                    val isUpdating = remember { mutableStateOf(false) }

                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.addPreset(presetName)
                                selectedPresetName = presetName
                            },
                            enabled = isNewPreset && presetName.isNotBlank() && uiState.presets.none { it.name == presetName },
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("Add")
                        }
                        if (!(isNewPreset && presetName.isNotBlank() && uiState.presets.none { it.name == presetName })) {
                            androidx.compose.foundation.layout.Box(
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
                        onClick = { viewModel.removePreset() },
                        enabled = !isNewPreset && !isUpdating.value,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isUpdating.value = true
                            viewModel.savePreset(selectedPresetName)
                            isUpdating.value = false
                        },
                        enabled = !isNewPreset && !isUpdating.value,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isUpdating.value) "Updating..." else "Update")
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
                        kotlinx.coroutines.delay(500)
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
                                viewModel.assignAbxyButton(btn, action?.displayName ?: "")
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