package com.examples.testros2jsbridge.presentation.ui.screens.geometry

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.presentation.mapper.MessageUiMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeometryMessageScreen(
    viewModel: GeometryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                Text(text = "Geometry Messages", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                // Name input
                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = viewModel::updateNameInput,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Topic input
                OutlinedTextField(
                    value = uiState.topicInput,
                    onValueChange = viewModel::updateTopicInput,
                    label = { Text("Topic") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Type dropdown
                var expanded by remember { mutableStateOf(false) }
                val types = com.examples.testros2jsbridge.domain.geometry.geometryTypes
                var selectedType by remember { mutableStateOf(uiState.typeInput.takeIf { it in types } ?: types.first()) }
                LaunchedEffect(uiState.typeInput) {
                    if (uiState.typeInput in types && uiState.typeInput != selectedType) {
                        selectedType = uiState.typeInput
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    types.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedType = type
                                viewModel.updateTypeInput(type)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic fields for selected geometry type
            val fieldSpecs = com.examples.testros2jsbridge.domain.geometry.geometryTypeFields[selectedType] ?: emptyList()
            // Build a list of all field tags for the selected type
            val fieldTags = remember(selectedType) {
                fieldSpecs.flatMap { spec ->
                    when (spec) {
                        is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.Vector3 ->
                            listOf(
                                spec.prefix.let { if (it.isEmpty()) "x" else "${it}_x" },
                                spec.prefix.let { if (it.isEmpty()) "y" else "${it}_y" },
                                spec.prefix.let { if (it.isEmpty()) "z" else "${it}_z" }
                            )
                        is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.Quaternion ->
                            listOf(
                                spec.prefix.let { if (it.isEmpty()) "x" else "${it}_x" },
                                spec.prefix.let { if (it.isEmpty()) "y" else "${it}_y" },
                                spec.prefix.let { if (it.isEmpty()) "z" else "${it}_z" },
                                spec.prefix.let { if (it.isEmpty()) "w" else "${it}_w" }
                            )
                        is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.Covariance ->
                            (0 until 36).map { "covariance$it" }
                        is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.Point32Array ->
                            (0 until 3).flatMap { idx -> listOf("points${idx}_x", "points${idx}_y", "points${idx}_z") }
                        is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.PoseArray ->
                            (0 until 2).flatMap { idx -> listOf("poses${idx}_position_x", "poses${idx}_position_y", "poses${idx}_position_z", "poses${idx}_orientation_x", "poses${idx}_orientation_y", "poses${idx}_orientation_z", "poses${idx}_orientation_w") }
                        is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.FloatField -> listOf(spec.tag)
                        is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.IntField -> listOf(spec.tag)
                        is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.StringField -> listOf(spec.tag)
                    }
                }
            }
            // Use a map of tag to mutableStateOf for field values
            val fieldStates = remember(selectedType) {
                fieldTags.associateWith { mutableStateOf("") }
            }

            fieldSpecs.forEach { spec ->
                when (spec) {
                    is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.Vector3 -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("x", "y", "z").forEach { axis ->
                                val tag = if (spec.prefix.isEmpty()) axis else "${spec.prefix}_$axis"
                                OutlinedTextField(
                                    value = fieldStates[tag]?.value ?: "",
                                    onValueChange = { fieldStates[tag]?.value = it },
                                    label = { Text(tag) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.Quaternion -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("x", "y", "z", "w").forEach { axis ->
                                val tag = if (spec.prefix.isEmpty()) axis else "${spec.prefix}_$axis"
                                OutlinedTextField(
                                    value = fieldStates[tag]?.value ?: "",
                                    onValueChange = { fieldStates[tag]?.value = it },
                                    label = { Text(tag) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.Covariance -> {
                        Text("Covariance (36 floats)", style = MaterialTheme.typography.bodyMedium)
                        val scrollState = rememberScrollState()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (0 until 36).forEach { idx ->
                                OutlinedTextField(
                                    value = fieldStates["covariance$idx"]?.value ?: "",
                                    onValueChange = { fieldStates["covariance$idx"]?.value = it },
                                    label = { Text("$idx") },
                                    modifier = Modifier.width(60.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.Point32Array -> {
                        Text("Point32 Array (3 points)", style = MaterialTheme.typography.bodyMedium)
                        (0 until 3).forEach { idx ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("x", "y", "z").forEach { axis ->
                                    val tag = "points${idx}_$axis"
                                    OutlinedTextField(
                                        value = fieldStates[tag]?.value ?: "",
                                        onValueChange = { fieldStates[tag]?.value = it },
                                        label = { Text("$axis$idx") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.PoseArray -> {
                        Text("Pose Array (2 poses)", style = MaterialTheme.typography.bodyMedium)
                        (0 until 2).forEach { idx ->
                            Text("Pose $idx", style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("x", "y", "z").forEach { axis ->
                                    val tag = "poses${idx}_position_$axis"
                                    OutlinedTextField(
                                        value = fieldStates[tag]?.value ?: "",
                                        onValueChange = { fieldStates[tag]?.value = it },
                                        label = { Text("pos $axis") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("x", "y", "z", "w").forEach { axis ->
                                    val tag = "poses${idx}_orientation_$axis"
                                    OutlinedTextField(
                                        value = fieldStates[tag]?.value ?: "",
                                        onValueChange = { fieldStates[tag]?.value = it },
                                        label = { Text("ori $axis") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.FloatField -> {
                        OutlinedTextField(
                            value = fieldStates[spec.tag]?.value ?: "",
                            onValueChange = { fieldStates[spec.tag]?.value = it },
                            label = { Text(spec.hint) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.IntField -> {
                        OutlinedTextField(
                            value = fieldStates[spec.tag]?.value ?: "",
                            onValueChange = { fieldStates[spec.tag]?.value = it },
                            label = { Text(spec.hint) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    is com.examples.testros2jsbridge.domain.geometry.GeometryFieldSpec.StringField -> {
                        OutlinedTextField(
                            value = fieldStates[spec.tag]?.value ?: "",
                            onValueChange = { fieldStates[spec.tag]?.value = it },
                            label = { Text(spec.hint) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { viewModel.saveMessage() }, modifier = Modifier.weight(1f)) {
                    Text("Save Message")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.publishMessage() }, modifier = Modifier.weight(1f)) {
                    Text("Publish Message")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.showSavedButtons) {
                Text(text = "Saved Geometry Messages", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.messages.forEach { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { viewModel.selectMessage(msg) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = msg.label ?: "Unnamed", style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Topic: ${msg.topic}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Type: ${msg.type}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Saved: ${MessageUiMapper.formatTimestamp(msg)}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Content:", style = MaterialTheme.typography.bodySmall)
                                Text(text = MessageUiMapper.formatMessageContent(msg), style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.deleteMessage(msg) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Message",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
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
}
}