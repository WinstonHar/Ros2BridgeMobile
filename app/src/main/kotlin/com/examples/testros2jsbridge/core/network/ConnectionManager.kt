
package com.examples.testros2jsbridge.core.network

import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.repository.RosConnectionRepository
import com.examples.testros2jsbridge.domain.usecase.connection.ConnectToRosUseCase
import com.examples.testros2jsbridge.domain.usecase.connection.DisconnectFromRosUseCase

/**
 * ConnectionManager manages the WebSocket connection to rosbridge, including connect/disconnect, message sending, and listener notification.
 * Provides a singleton interface for all ROS2 network communication in the app.
 */
class ConnectionManager(
    private val connectionRepository: RosConnectionRepository,
    private val connectToRosUseCase: ConnectToRosUseCase,
    private val disconnectFromRosUseCase: DisconnectFromRosUseCase,
    private val rosbridgeClient: com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
) {
    private val listeners = mutableListOf<Listener>()

    // Setup WebSocket event forwarding
    private val wsListener = com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeWebSocketListener(
        _onOpen = { _, _ -> listeners.forEach { it.onConnected() } },
        _onMessage = { msg: String -> listeners.forEach { it.onMessage(msg) } },
        _onFailure = { t: Throwable -> listeners.forEach { it.onError(t.message ?: "Unknown error") } },
        _onClosing = { listeners.forEach { it.onDisconnected() } },
        _onClosed = { listeners.forEach { it.onDisconnected() } }
    )

    interface Listener {
        fun onConnected()
        fun onDisconnected()
        fun onMessage(text: String)
        fun onError(error: String)
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun connect(ip: String, port: Int) {
        // Build the ws/wss URL
        val protocol = "ws" // TODO: Make configurable if needed
        val url = "$protocol://$ip:$port"
        try {
            rosbridgeClient.setUrl(url)
            rosbridgeClient.setListener(wsListener)
            rosbridgeClient.connect()
        } catch (e: Exception) {
            Logger.e("ConnectionManager", "Connection failed: ${e.message}")
            notifyError(e.message ?: "Unknown error")
        }
    }

    fun disconnect(connectionId: String) {
        try {
            rosbridgeClient.disconnect()
        } catch (e: Exception) {
            Logger.e("ConnectionManager", "Disconnection failed: ${e.message}")
            notifyError(e.message ?: "Unknown error")
        }
    }

    fun notifyError(error: String) {
        listeners.forEach { it.onError(error) }
    }
}