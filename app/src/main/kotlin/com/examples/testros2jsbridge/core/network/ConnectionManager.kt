
package com.examples.testros2jsbridge.core.network

import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.repository.RosConnectionRepository
import com.examples.testros2jsbridge.domain.usecase.connection.ConnectToRosUseCase
import com.examples.testros2jsbridge.domain.usecase.connection.DisconnectFromRosUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ConnectionManager manages the WebSocket connection to rosbridge, including connect/disconnect, message sending, and listener notification.
 * Provides a singleton interface for all ROS2 network communication in the app.
 */
class ConnectionManager(
    private val connectionRepository: RosConnectionRepository,
    private val connectToRosUseCase: ConnectToRosUseCase,
    private val disconnectFromRosUseCase: DisconnectFromRosUseCase
) {
    private val listeners = mutableListOf<Listener>()

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
        CoroutineScope(Dispatchers.Main).launch {
            try {
                connectToRosUseCase.connect(ip, port)
                listeners.forEach { it.onConnected() }
            } catch (e: Exception) {
                Logger.e("ConnectionManager", "Connection failed: ${e.message}")
                notifyError(e.message ?: "Unknown error")
            }
        }
    }

    fun disconnect(connectionId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                disconnectFromRosUseCase.disconnect(connectionId)
                listeners.forEach { it.onDisconnected() }
            } catch (e: Exception) {
                Logger.e("ConnectionManager", "Disconnection failed: ${e.message}")
                notifyError(e.message ?: "Unknown error")
            }
        }
    }

    fun notifyError(error: String) {
        listeners.forEach { it.onError(error) }
    }
}