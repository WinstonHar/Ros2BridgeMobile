package com.examples.testros2jsbridge.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RosConnectionCard(
    ipAddress: String,
    port: String,
    isConnected: Boolean,
    connectionStatus: String,
    onIpAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "ROS2 Connection", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = ipAddress,
            onValueChange = onIpAddressChange,
            label = { Text("IP Address") },
            enabled = !isConnected,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            label = { Text("Port") },
            enabled = !isConnected,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(
                onClick = onConnect,
                enabled = !isConnected
            ) {
                Text("Connect")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onDisconnect,
                enabled = isConnected
            ) {
                Text("Disconnect")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClear,
                enabled = !isConnected && (ipAddress.isNotBlank() || port.isNotBlank())
            ) {
                Text("Clear")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Status: $connectionStatus", style = MaterialTheme.typography.bodyLarge)
    }
}