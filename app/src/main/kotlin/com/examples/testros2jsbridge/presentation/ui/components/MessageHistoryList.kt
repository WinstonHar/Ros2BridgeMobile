package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CollapsibleMessageHistoryList(messageHistory: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    val reversedHistory = remember(messageHistory) { messageHistory.asReversed() }

    Surface(
        color = Color.Transparent,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(colorResource(id = android.R.color.background_light))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Tab bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sent Message History...",
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.Remove else Icons.Filled.Add,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            // Expandable content
            if (expanded) {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp)
                        .padding(8.dp)
                ) {
                    items(reversedHistory) { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}