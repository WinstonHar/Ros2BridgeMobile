package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.data.local.database.RosProtocolType
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.presentation.state.ProtocolUiState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun PublisherScreen(
    navigator: DestinationsNavigator,
    viewModel: ProtocolViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activeProtocol by viewModel.activeProtocol.collectAsState()
    val protocolFields by viewModel.protocolFields.collectAsState()
    val protocolFieldValues by viewModel.protocolFieldValues.collectAsState()
    val editingAction by viewModel.editingAppAction.collectAsState()
    val appActions by viewModel.customAppActions.collectAsState()

    var actionDisplayName by remember { mutableStateOf(TextFieldValue()) }
    var actionTopic by remember { mutableStateOf(TextFieldValue()) }
    var actionType by remember { mutableStateOf(TextFieldValue()) }
    var actionSource by remember { mutableStateOf(TextFieldValue()) }
    var actionMsg by remember { mutableStateOf(TextFieldValue()) }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedPackageName by remember { mutableStateOf("") }

    var selectedMsg by remember { mutableStateOf<ProtocolUiState.ProtocolFile?>(null) }
    var selectedSrv by remember { mutableStateOf<ProtocolUiState.ProtocolFile?>(null) }
    var selectedAct by remember { mutableStateOf<ProtocolUiState.ProtocolFile?>(null) }

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

    LaunchedEffect(uiState.actionSaved) {
        if (uiState.actionSaved) {
            actionDisplayName = TextFieldValue()
            actionTopic = TextFieldValue()
            actionType = TextFieldValue()
            actionSource = TextFieldValue()
            actionMsg = TextFieldValue()
            selectedMsg = null
            selectedSrv = null
            selectedAct = null
            viewModel.onActionSaved()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(text = "Publisher Controls", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Protocol Section
                Text(text = "Create App Action", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPackageName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Package") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        uiState.packageNames.forEach { packageName ->
                            DropdownMenuItem(
                                text = { Text(packageName) },
                                onClick = {
                                    isDropdownExpanded = false
                                    selectedPackageName = packageName
                                    viewModel.onPackageSelected(packageName)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                var expandedMsg by remember { mutableStateOf(false) }
                var expandedSrv by remember { mutableStateOf(false) }
                var expandedAct by remember { mutableStateOf(false) }

                Text(text = "Select a Message Type", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedMsg,
                    onExpandedChange = { expandedMsg = it }
                ) {
                    OutlinedTextField(
                        value = selectedMsg?.name ?: "Select Message",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Message Types") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedMsg) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMsg,
                        onDismissRequest = { expandedMsg = false }
                    ) {
                        uiState.availableMessages.forEach { proto ->
                            val coroutineScope = rememberCoroutineScope()

                            DropdownMenuItem(
                                text = { Text(proto.name) },
                                onClick = {
                                    selectedMsg = proto
                                    selectedSrv = null
                                    selectedAct = null
                                    expandedMsg = false
                                    coroutineScope.launch {
                                        viewModel.loadProtocolFields(
                                            context,
                                            selectedProtocol = proto
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Select a Service Type", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedSrv,
                    onExpandedChange = { expandedSrv = it }
                ) {
                    OutlinedTextField(
                        value = selectedSrv?.name ?: "Select Service",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service Types") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedSrv) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSrv,
                        onDismissRequest = { expandedSrv = false }
                    ) {
                        uiState.availableServices.forEach { proto ->
                            val coroutineScope = rememberCoroutineScope()

                            DropdownMenuItem(
                                text = { Text(proto.name) },
                                onClick = {
                                    selectedSrv = proto
                                    selectedMsg = null
                                    selectedAct = null
                                    expandedSrv = false
                                    coroutineScope.launch {
                                        viewModel.loadProtocolFields(
                                            context,
                                            selectedProtocol = proto
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Select a Action Type", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedAct,
                    onExpandedChange = { expandedAct = it }
                ) {
                    OutlinedTextField(
                        value = selectedAct?.name ?: "Select Action",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action Types") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedAct) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAct,
                        onDismissRequest = { expandedAct = false }
                    ) {
                        uiState.availableActions.forEach { proto ->
                            val coroutineScope = rememberCoroutineScope()

                            DropdownMenuItem(
                                text = { Text(proto.name) },
                                onClick = {
                                    selectedAct = proto
                                    selectedMsg = null
                                    selectedSrv = null
                                    expandedAct = false
                                    coroutineScope.launch {
                                        viewModel.loadProtocolFields(
                                            context,
                                            selectedProtocol = proto
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                if (activeProtocol != null && protocolFields.isNotEmpty()) {

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Configure Fields for ${activeProtocol!!.name}", style = MaterialTheme.typography.titleMedium)

                    val editableFields = remember(protocolFields) { protocolFields.filter { it.section == "Goal" && !it.isConstant } }
                    val fixedFields = remember(protocolFields) { protocolFields.filter { it.isConstant || it.section != "Goal" } }

                    val focusRequesters = remember(editableFields.size + 1) { List(editableFields.size + 1) { FocusRequester() } }

                    Column {
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
                        viewModel.triggerProtocol()
                    }) {
                        Text("Trigger Protocol")
                    }
                    Button(onClick = {
                        val id = UUID.randomUUID().toString()
                        val selectedProtocol = selectedMsg ?: selectedSrv ?: selectedAct
                        val fullType = if (selectedProtocol != null) {
                            when (selectedProtocol.type) {
                                ProtocolUiState.ProtocolType.MSG -> "${selectedProtocol.packageName}/${selectedProtocol.name}"
                                else -> "${selectedProtocol.packageName}/${selectedProtocol.type.name.lowercase()}/${selectedProtocol.name}"
                            }
                        } else {
                            activeProtocol!!.type.name
                        }
                        val msgJson = viewModel.buildMsgArgsJson()
                        viewModel.saveCustomAppAction(
                            context,
                            AppAction(
                                id = id,
                                displayName = protocolFieldValues["__topic__"] ?: activeProtocol!!.name,
                                topic = protocolFieldValues["__topic__"] ?: "",
                                type = fullType,
                                source = activeProtocol!!.packageName,
                                msg = msgJson,
                                rosMessageType = when {
                                    selectedSrv != null -> RosProtocolType.SERVICE_CLIENT.name
                                    selectedAct != null -> RosProtocolType.ACTION_CLIENT.name
                                    else -> RosProtocolType.PUBLISHER.name
                                }
                            )
                        )
                        keyboardController?.hide()
                    }) {
                        Text("Save as App Action")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (editingAction == null) "Create Custom App Action" else "Edit Custom App Action",
                    style = MaterialTheme.typography.titleMedium
                )
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
                    visualTransformation = VisualTransformation.None,
                    interactionSource = remember {
                        val source = MutableInteractionSource()
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
                        val id = editingAction?.id ?: UUID.randomUUID().toString()
                        val typeText = actionType.text
                        val protocolType = when {
                            typeText.contains("/srv/") -> RosProtocolType.SERVICE_CLIENT
                            typeText.contains("/action/") -> RosProtocolType.ACTION_CLIENT
                            else -> RosProtocolType.PUBLISHER // or SUBSCRIBER, default to PUBLISHER
                        }
                        val finalType = if (protocolType == RosProtocolType.PUBLISHER) {
                            typeText.replace("/msg/", "/")
                        } else {
                            typeText
                        }

                        viewModel.saveCustomAppAction(
                            context,
                            AppAction(
                                id = id,
                                displayName = actionDisplayName.text,
                                topic = actionTopic.text,
                                type = finalType,
                                source = actionSource.text,
                                msg = actionMsg.text,
                                rosMessageType = protocolType.name
                            )
                        )
                        viewModel.setEditingAppAction(null)
                    }) {
                        Text(if (editingAction == null) "Save App Action" else "Update App Action")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
            
            item {
                // Publisher List Section
                Spacer(modifier = Modifier.height(16.dp))
                Text("Saved App Actions", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                if (appActions.isEmpty()) {
                    Text("No app actions found.")
                } else {
                    appActions.forEach { action ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.setEditingAppAction(action) }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(action.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text("Topic: ${action.topic}", style = MaterialTheme.typography.bodySmall)
                                    Text("Type: ${action.type}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Message JSON:", style = MaterialTheme.typography.bodySmall)
                                    Text(action.msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                                }
                                Row {
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
            }
        }
    }
}
