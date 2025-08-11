package com.examples.testros2jsbridge.presentation.ui.screens.protocol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.presentation.state.ProtocolUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProtocolScreen(
    viewModel: ProtocolViewModel = hiltViewModel(),
    controllerViewModel: com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activeProtocol by viewModel.activeProtocol.collectAsState()
    val protocolFields by viewModel.protocolFields.collectAsState()
    val protocolFieldValues by viewModel.protocolFieldValues.collectAsState()
    val editingAction by viewModel.editingAppAction.collectAsState()
    val customAppActions = viewModel.customAppActions.collectAsState().value

    // Local state for AppAction editor
    var actionDisplayName by remember { mutableStateOf(TextFieldValue()) }
    var actionTopic by remember { mutableStateOf(TextFieldValue()) }
    var actionType by remember { mutableStateOf(TextFieldValue()) }
    var actionSource by remember { mutableStateOf(TextFieldValue()) }
    var actionMsg by remember { mutableStateOf(TextFieldValue()) }

    // Sync local state with editingAction
    LaunchedEffect(editingAction) {
        val action = editingAction
        if (action != null) {
            actionDisplayName = TextFieldValue(action.displayName)
            actionTopic = TextFieldValue(action.topic)
            actionType = TextFieldValue(action.type)
            actionSource = TextFieldValue(action.source)
            actionMsg = TextFieldValue(action.msg)
        } else {
            actionDisplayName = TextFieldValue()
            actionTopic = TextFieldValue()
            actionType = TextFieldValue()
            actionSource = TextFieldValue()
            actionMsg = TextFieldValue()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProtocols(context)
        viewModel.loadCustomAppActions(context)
    }

    // For hiding keyboard
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .nestedScroll(rememberNestedScrollInteropConnection()),
            verticalArrangement = Arrangement.Top
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Custom Protocols", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onBack) { Text("Back") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Dropdowns for each protocol type
            item {
                var expandedMsg by remember { mutableStateOf(false) }
                var expandedSrv by remember { mutableStateOf(false) }
                var expandedAct by remember { mutableStateOf(false) }
                var selectedMsg by remember { mutableStateOf<ProtocolUiState.ProtocolFile?>(null) }
                var selectedSrv by remember { mutableStateOf<ProtocolUiState.ProtocolFile?>(null) }
                var selectedAct by remember { mutableStateOf<ProtocolUiState.ProtocolFile?>(null) }

                Text(text = "Select Message Protocol", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedMsg,
                    onExpandedChange = { expandedMsg = it }
                ) {
                    OutlinedTextField(
                        value = selectedMsg?.name ?: "Select Message",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Message Protocols") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedMsg) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMsg,
                        onDismissRequest = { expandedMsg = false }
                    ) {
                        uiState.availableMessages.forEach { proto ->
                            DropdownMenuItem(
                                text = { Text(proto.name) },
                                onClick = {
                                    selectedMsg = proto
                                    selectedSrv = null
                                    selectedAct = null
                                    expandedMsg = false
                                    viewModel.loadProtocolFields(
                                        context,
                                        CustomProtocol(proto.name, proto.importPath, CustomProtocol.Type.valueOf(proto.type.name))
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Select Service Protocol", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedSrv,
                    onExpandedChange = { expandedSrv = it }
                ) {
                    OutlinedTextField(
                        value = selectedSrv?.name ?: "Select Service",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service Protocols") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedSrv) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSrv,
                        onDismissRequest = { expandedSrv = false }
                    ) {
                        uiState.availableServices.forEach { proto ->
                            DropdownMenuItem(
                                text = { Text(proto.name) },
                                onClick = {
                                    selectedSrv = proto
                                    selectedMsg = null
                                    selectedAct = null
                                    expandedSrv = false
                                    viewModel.loadProtocolFields(
                                        context,
                                        CustomProtocol(proto.name, proto.importPath, CustomProtocol.Type.valueOf(proto.type.name))
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Select Action Protocol", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedAct,
                    onExpandedChange = { expandedAct = it }
                ) {
                    OutlinedTextField(
                        value = selectedAct?.name ?: "Select Action",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action Protocols") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedAct) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAct,
                        onDismissRequest = { expandedAct = false }
                    ) {
                        uiState.availableActions.forEach { proto ->
                            DropdownMenuItem(
                                text = { Text(proto.name) },
                                onClick = {
                                    selectedAct = proto
                                    selectedMsg = null
                                    selectedSrv = null
                                    expandedAct = false
                                    viewModel.loadProtocolFields(
                                        context,
                                        CustomProtocol(proto.name, proto.importPath, CustomProtocol.Type.valueOf(proto.type.name))
                                    )
                                }
                            )
                        }
                    }
                }
            }
            // Dynamic protocol fields for selected protocol
            if (activeProtocol != null && protocolFields.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Configure Fields for ${activeProtocol!!.name}", style = MaterialTheme.typography.titleMedium)

                    // Editable: only Goal fields that are not constants; Non-editable: constants or non-Goal fields
                    val editableFields = remember(protocolFields) { protocolFields.filter { it.section == "Goal" && !it.isConstant } }
                    val fixedFields = remember(protocolFields) { protocolFields.filter { it.isConstant || it.section != "Goal" } }

                    // IME navigation for editable fields only
                    val focusRequesters = remember(editableFields.size + 1) { List(editableFields.size + 1) { FocusRequester() } }

                    Column {
                        // Always show topic field at the top
                        OutlinedTextField(
                            value = protocolFieldValues["topic"] ?: protocolFieldValues["__topic__"] ?: "",
                            onValueChange = { viewModel.updateProtocolFieldValue("topic", it) },
                            label = { Text("topic (string)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesters[0]),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = if (editableFields.isNotEmpty()) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    if (editableFields.isNotEmpty()) {
                                        focusRequesters[1].requestFocus()
                                    }
                                },
                                onDone = { keyboardController?.hide() }
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Render editable fields (Goal section, not constants)
                        editableFields.forEachIndexed { idx, field ->
                            OutlinedTextField(
                                value = protocolFieldValues[field.name] ?: "",
                                onValueChange = { viewModel.updateProtocolFieldValue(field.name, it) },
                                label = { Text(field.name + " (" + field.type + ")") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequesters[idx + 1]),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = if (idx < editableFields.lastIndex) ImeAction.Next else ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        if (idx < editableFields.lastIndex) {
                                            focusRequesters[idx + 2].requestFocus()
                                        }
                                    },
                                    onDone = { keyboardController?.hide() }
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Render fixed fields (constants or non-Goal sections, always non-editable)
                        fixedFields.forEach { field ->
                            OutlinedTextField(
                                value = protocolFieldValues[field.name] ?: "",
                                onValueChange = {},
                                label = { Text(field.name + " (" + field.type + ")" + if (field.isConstant) " [CONST]" else " [${field.section}]" ) },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        // Save as AppAction
                        val id = java.util.UUID.randomUUID().toString()
                        // Build msg JSON from all non-constant, non-meta fields
                        val msgJson = viewModel.buildProtocolMsgJson(protocolFields, protocolFieldValues, protocolFieldValues["topic"] ?: protocolFieldValues["__topic__"])
                        viewModel.saveCustomAppAction(
                            context,
                            AppAction(
                                id = id,
                                displayName = protocolFieldValues["displayName"] ?: activeProtocol!!.name,
                                topic = protocolFieldValues["topic"] ?: "",
                                type = protocolFieldValues["type"] ?: activeProtocol!!.type.name,
                                source = "Protocol",
                                msg = msgJson
                            )
                        )
                        keyboardController?.hide()
                    }) {
                        Text("Save as App Action")
                    }
                }
            }
            // ...existing code...
            // Removed selectedProtocolForConfig block; handled above with activeProtocol
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                // App Action Editor with IME navigation (Next/Done)
                Text(
                    text = if (editingAction == null) "Create Custom App Action" else "Edit Custom App Action",
                    style = MaterialTheme.typography.titleMedium
                )
                // IME navigation for the 5 fields
                val appActionFocusRequesters = remember { List(5) { FocusRequester() } }
                OutlinedTextField(
                    value = actionDisplayName,
                    onValueChange = { actionDisplayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(appActionFocusRequesters[0]),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { appActionFocusRequesters[1].requestFocus() }
                    )
                )
                OutlinedTextField(
                    value = actionTopic,
                    onValueChange = { actionTopic = it },
                    label = { Text("Topic") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(appActionFocusRequesters[1]),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { appActionFocusRequesters[2].requestFocus() }
                    )
                )
                OutlinedTextField(
                    value = actionType,
                    onValueChange = { actionType = it },
                    label = { Text("Type") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(appActionFocusRequesters[2]),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { appActionFocusRequesters[3].requestFocus() }
                    )
                )
                OutlinedTextField(
                    value = actionSource,
                    onValueChange = { actionSource = it },
                    label = { Text("Source") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(appActionFocusRequesters[3]),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { appActionFocusRequesters[4].requestFocus() }
                    )
                )
                OutlinedTextField(
                    value = actionMsg,
                    onValueChange = { actionMsg = it },
                    label = { Text("Message (JSON)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(appActionFocusRequesters[4]),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    singleLine = false,
                    maxLines = 8,
                    // Disable fullscreen extract UI for landscape
                    visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                    interactionSource = remember {
                        val source = androidx.compose.foundation.interaction.MutableInteractionSource()
                        // Set IME_FLAG_NO_EXTRACT_UI via AndroidView if needed
                        source
                    }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (editingAction != null) {
                        Button(onClick = {
                            viewModel.setEditingAppAction(null)
                        }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(onClick = {
                        val id = editingAction?.id ?: java.util.UUID.randomUUID().toString()
                        // Build msg JSON from all non-constant, non-meta fields for edit mode
                        val msgJson = viewModel.buildProtocolMsgJson(protocolFields, protocolFieldValues, protocolFieldValues["topic"] ?: protocolFieldValues["__topic__"])
                        viewModel.saveCustomAppAction(
                            context,
                            AppAction(
                                id = id,
                                displayName = actionDisplayName.text,
                                topic = actionTopic.text,
                                type = actionType.text,
                                source = actionSource.text,
                                msg = msgJson
                            )
                        )
                        viewModel.setEditingAppAction(null)
                    }) {
                        Text(if (editingAction == null) "Save App Action" else "Update App Action")
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                // List of saved custom app actions
                Text(text = "Custom App Actions", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(customAppActions) { action ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = action.displayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Topic: ${action.topic}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Type: ${action.type}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Source: ${action.source}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Msg: ${action.msg}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(onClick = {
                                    controllerViewModel.triggerAppAction(action)
                                }, modifier = Modifier.padding(end = 8.dp)) {
                                    Text("Send")
                                }
                                IconButton(onClick = {
                                    viewModel.setEditingAppAction(action)
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    viewModel.deleteCustomAppAction(context, action.id)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
            item {
                if (uiState.isImporting) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
            }
            item {
                if (uiState.showErrorDialog && uiState.errorMessage != null) {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissErrorDialog,
                        title = { Text("Error") },
                        text = { Text(uiState.errorMessage ?: "") },
                        confirmButton = {
                            Button(onClick = viewModel::dismissErrorDialog) { Text("OK") }
                        }
                    )
                }
            }
        }
    }
}