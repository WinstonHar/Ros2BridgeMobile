package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.model.AppAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSelector(
    topics: List<AppAction>,
    selectedTopic: AppAction?,
    onTopicSelected: (AppAction?) -> Unit,
    label: String = "Select Topic"
) {
    var expanded by remember { mutableStateOf(false) }

    Logger.d("TopicSelector","TopicSelector - Topics: ${topics.map { it.displayName }}")

    Box(modifier = Modifier.padding(16.dp)) {
        Column {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                    Logger.d("TopicSelector", "Dropdown expanded state toggled to: $expanded")
                }
            ) {
                OutlinedTextField(
                    value = selectedTopic?.displayName ?: "",
                    onValueChange = {},
                    label = { Text(label) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .clickable {
                            expanded = !expanded
                            Logger.d("TopicSelector", "Dropdown clicked, expanded state toggled to: $expanded")
                        },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                        Logger.d("TopicSelector", "Dropdown dismissed")
                    }
                ) {
                    Logger.d("TopicSelector", "Rendering dropdown items: ${topics.map { it.displayName }}")
                    topics.forEach { topic ->
                        DropdownMenuItem(
                            text = { Text(topic.displayName) },
                            onClick = {
                                Logger.d("TopicSelector", "Selected topic: ${topic.displayName}")
                                onTopicSelected(topic)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}