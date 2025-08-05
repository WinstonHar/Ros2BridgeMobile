package com.examples.testros2jsbridge.data.remote.rosbridge

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeWebSocketListener

class RosbridgeClient(
    private val url: String,
    private val listener: RosbridgeWebSocketListener
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect() {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, listener)
    }

    fun send(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}