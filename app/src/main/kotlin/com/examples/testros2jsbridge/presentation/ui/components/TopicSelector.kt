package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopicSelector(
    topics: List<String>,
    selectedTopic: String?,
    onTopicSelected: (String) -> Unit,
    label: String = "Select Topic"
) {
    var expanded by remember { mutableStateOf(false) }
    var currentTopic by remember { mutableStateOf(selectedTopic ?: "") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentTopic,
                onValueChange = {
                    currentTopic = it
                    onTopicSelected(it)
                },
                label = { Text(label) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                topics.forEach { topic ->
                    DropdownMenuItem(
                        text = { Text(topic) },
                        onClick = {
                            currentTopic = topic
                            onTopicSelected(topic)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}