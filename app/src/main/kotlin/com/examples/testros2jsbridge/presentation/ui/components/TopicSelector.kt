package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSelector(
    topics: List<com.examples.testros2jsbridge.domain.model.AppAction>,
    selectedTopic: com.examples.testros2jsbridge.domain.model.AppAction?,
    onTopicSelected: (com.examples.testros2jsbridge.domain.model.AppAction?) -> Unit,
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