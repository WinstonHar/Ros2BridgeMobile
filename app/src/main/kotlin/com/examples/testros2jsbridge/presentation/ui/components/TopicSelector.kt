package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.background
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


    Box(modifier = Modifier.padding(16.dp)) {
        Column {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
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
                        },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    topics.forEach { topic ->
                        val isSelected = topic.id == selectedTopic?.id
                        DropdownMenuItem(
                            text = { Text(topic.displayName) },
                            onClick = {
                                onTopicSelected(topic)
                                expanded = false
                            },
                            // Highlight selected item
                            modifier = if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else Modifier
                        )
                    }
                }
            }
        }
    }
}