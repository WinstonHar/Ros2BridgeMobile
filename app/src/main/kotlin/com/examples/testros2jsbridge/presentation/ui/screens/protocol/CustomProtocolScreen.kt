package com.examples.testros2jsbridge.presentation.ui.screens.protocol

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import com.examples.testros2jsbridge.domain.model.AppAction
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CustomProtocolScreen(
    viewModel: ProtocolViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var editingAction by remember { mutableStateOf<AppAction?>(null) }
    var actionDisplayName by remember { mutableStateOf(TextFieldValue()) }
    var actionTopic by remember { mutableStateOf(TextFieldValue()) }
    var actionType by remember { mutableStateOf(TextFieldValue()) }
    var actionSource by remember { mutableStateOf(TextFieldValue()) }
    var actionMsg by remember { mutableStateOf(TextFieldValue()) }
    var importResult by remember { mutableStateOf<List<com.examples.testros2jsbridge.domain.model.CustomProtocol>>(emptyList()) }
    var selectedImportedProtocols by remember { mutableStateOf<Set<String>>(emptySet()) }
    var protocolFieldValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedProtocolForConfig by remember { mutableStateOf<com.examples.testros2jsbridge.domain.model.CustomProtocol?>(null) }
    val customAppActions = viewModel.customAppActions.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.loadProtocols(context)
        viewModel.loadCustomAppActions(context)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Custom Protocols", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Protocol selection lists
        Text(text = "Available Messages (.msg)", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
            items(uiState.availableMessages) { file ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.selectedProtocols.contains(file.importPath),
                        onCheckedChange = { checked ->
                            viewModel.toggleProtocolSelection(file.importPath)
                        }
                    )
                    Text(text = file.name, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Available Services (.srv)", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
            items(uiState.availableServices) { file ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.selectedProtocols.contains(file.importPath),
                        onCheckedChange = { checked ->
                            viewModel.toggleProtocolSelection(file.importPath)
                        }
                    )
                    Text(text = file.name, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Available Actions (.action)", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
            items(uiState.availableActions) { file ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.selectedProtocols.contains(file.importPath),
                        onCheckedChange = { checked ->
                            viewModel.toggleProtocolSelection(file.importPath)
                        }
                    )
                    Text(text = file.name, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))



        Button(
            onClick = {
                viewModel.importProtocols(context, selectedImportedProtocols)
            },
            enabled = selectedImportedProtocols.isNotEmpty()
        ) {
            Text("Import Selected Protocols")
        }

        // After import, show configuration form for each imported protocol
        if (selectedImportedProtocols.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Configure Imported Protocols:", style = MaterialTheme.typography.bodyMedium)
            uiState.availableMessages.plus(uiState.availableServices).plus(uiState.availableActions)
                .filter { selectedImportedProtocols.contains(it.importPath) }
                .forEach { proto ->
                    var localFieldValues by remember(proto.importPath) { mutableStateOf<Map<String, String>>(emptyMap()) }
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("${proto.name} (${proto.type})", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = localFieldValues["displayName"] ?: proto.name,
                                onValueChange = { localFieldValues = localFieldValues.toMutableMap().apply { put("displayName", it) } },
                                label = { Text("Display Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = localFieldValues["topic"] ?: "",
                                onValueChange = { localFieldValues = localFieldValues.toMutableMap().apply { put("topic", it) } },
                                label = { Text("Topic") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = localFieldValues["type"] ?: proto.type.name,
                                onValueChange = { localFieldValues = localFieldValues.toMutableMap().apply { put("type", it) } },
                                label = { Text("Type") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = localFieldValues["source"] ?: "",
                                onValueChange = { localFieldValues = localFieldValues.toMutableMap().apply { put("source", it) } },
                                label = { Text("Source") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = localFieldValues["msg"] ?: "",
                                onValueChange = { localFieldValues = localFieldValues.toMutableMap().apply { put("msg", it) } },
                                label = { Text("Message (JSON)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(onClick = {
                                    val id = java.util.UUID.randomUUID().toString()
                                    viewModel.saveCustomAppAction(
                                        context,
                                        AppAction(
                                            id = id,
                                            displayName = localFieldValues["displayName"] ?: proto.name,
                                            topic = localFieldValues["topic"] ?: "",
                                            type = localFieldValues["type"] ?: proto.type.name,
                                            source = localFieldValues["source"] ?: "",
                                            msg = localFieldValues["msg"] ?: ""
                                        )
                                    )
                                    // Optionally clear the form after save
                                    localFieldValues = emptyMap()
                                }) {
                                    Text("Save as App Action")
                                }
                            }
                        }
                    }
                }
        }

        // Show dynamic form for selected imported protocol
        selectedProtocolForConfig?.let { proto ->
            Spacer(modifier = Modifier.height(16.dp))
            Text("Configure Protocol: ${proto.name}", style = MaterialTheme.typography.titleMedium)
            // Example fields: name, importPath, type (add more as needed)
            OutlinedTextField(
                value = protocolFieldValues["displayName"] ?: "",
                onValueChange = { protocolFieldValues = protocolFieldValues.toMutableMap().apply { put("displayName", it) } },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = protocolFieldValues["topic"] ?: "",
                onValueChange = { protocolFieldValues = protocolFieldValues.toMutableMap().apply { put("topic", it) } },
                label = { Text("Topic") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = protocolFieldValues["type"] ?: proto.type.name,
                onValueChange = { protocolFieldValues = protocolFieldValues.toMutableMap().apply { put("type", it) } },
                label = { Text("Type") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = protocolFieldValues["source"] ?: "",
                onValueChange = { protocolFieldValues = protocolFieldValues.toMutableMap().apply { put("source", it) } },
                label = { Text("Source") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = protocolFieldValues["msg"] ?: "",
                onValueChange = { protocolFieldValues = protocolFieldValues.toMutableMap().apply { put("msg", it) } },
                label = { Text("Message (JSON)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    // Save as AppAction
                    val id = java.util.UUID.randomUUID().toString()
                    viewModel.saveCustomAppAction(
                        context,
                        AppAction(
                            id = id,
                            displayName = protocolFieldValues["displayName"] ?: proto.name,
                            topic = protocolFieldValues["topic"] ?: "",
                            type = protocolFieldValues["type"] ?: proto.type.name,
                            source = protocolFieldValues["source"] ?: "",
                            msg = protocolFieldValues["msg"] ?: ""
                        )
                    )
                    // Reset form
                    protocolFieldValues = emptyMap()
                    selectedProtocolForConfig = null
                }) {
                    Text("Save as App Action")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Action Editor
        Text(text = if (editingAction == null) "Create Custom App Action" else "Edit Custom App Action", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = actionDisplayName,
            onValueChange = { actionDisplayName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = actionTopic,
            onValueChange = { actionTopic = it },
            label = { Text("Topic") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = actionType,
            onValueChange = { actionType = it },
            label = { Text("Type") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = actionSource,
            onValueChange = { actionSource = it },
            label = { Text("Source") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = actionMsg,
            onValueChange = { actionMsg = it },
            label = { Text("Message (JSON)") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (editingAction != null) {
                Button(onClick = {
                    editingAction = null
                    actionDisplayName = TextFieldValue()
                    actionTopic = TextFieldValue()
                    actionType = TextFieldValue()
                    actionSource = TextFieldValue()
                    actionMsg = TextFieldValue()
                }) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Button(onClick = {
                val id = editingAction?.id ?: java.util.UUID.randomUUID().toString()
                viewModel.saveCustomAppAction(
                    context,
                    AppAction(
                        id = id,
                        displayName = actionDisplayName.text,
                        topic = actionTopic.text,
                        type = actionType.text,
                        source = actionSource.text,
                        msg = actionMsg.text
                    )
                )
                editingAction = null
                actionDisplayName = TextFieldValue()
                actionTopic = TextFieldValue()
                actionType = TextFieldValue()
                actionSource = TextFieldValue()
                actionMsg = TextFieldValue()
            }) {
                Text(if (editingAction == null) "Save App Action" else "Update App Action")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // List of saved custom app actions
        Text(text = "Custom App Actions", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
            items(customAppActions) { action ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = action.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(text = "Topic: ${action.topic}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Type: ${action.type}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Source: ${action.source}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            editingAction = action
                            actionDisplayName = TextFieldValue(action.displayName)
                            actionTopic = TextFieldValue(action.topic)
                            actionType = TextFieldValue(action.type)
                            actionSource = TextFieldValue(action.source)
                            actionMsg = TextFieldValue(action.msg)
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

        if (uiState.isImporting) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }

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