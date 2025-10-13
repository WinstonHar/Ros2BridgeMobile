package com.examples.testros2jsbridge.data.remote.rosbridge

import com.examples.testros2jsbridge.core.util.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RosbridgeClient manages WebSocket connection to ROS2 rosbridge server
 */
@Singleton
class RosbridgeClient @Inject constructor() {
    private var webSocket: WebSocket? = null
    private var url: String = ""
    private var listener: WebSocketListener? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun setUrl(newUrl: String) {
        this.url = newUrl
    }

    fun setListener(newListener: WebSocketListener) {
        this.listener = newListener
    }

    fun connect() {
        if (url.isEmpty()) {
            Logger.e("RosbridgeClient", "Cannot connect: URL not set")
            return
        }
        if (listener == null) {
            Logger.e("RosbridgeClient", "Cannot connect: Listener not set")
            return
        }

        disconnect()

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, listener!!)
        Logger.d("RosbridgeClient", "Connecting to $url")
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        Logger.d("RosbridgeClient", "Disconnected")
    }

    fun send(message: String) {
        val ws = webSocket
        if (ws == null) {
            Logger.e("RosbridgeClient", "Cannot send message: Not connected")
            return
        }
        ws.send(message)
    }

    fun isConnected(): Boolean = webSocket != null
}
