package com.examples.testros2jsbridge.data.remote.rosbridge

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class RosbridgeClient(
    private val url: String,
    private val listener: RosbridgeWebSocketListener
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun unsubscribe(topic: String, type: String? = null) {
        topicCallbacks.remove(topic)
        val unsubscribeMsg = mutableMapOf(
            "op" to "unsubscribe",
            "topic" to topic
        )
        if (type != null) {
            unsubscribeMsg["type"] = type
        }
        val jsonString = kotlinx.serialization.json.Json.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            unsubscribeMsg
        )
        send(jsonString)
    }

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

    private val topicCallbacks = mutableMapOf<String, (String) -> Unit>()

    fun subscribe(topic: String, type: String, onMessage: (String) -> Unit) {
        topicCallbacks[topic] = onMessage
        val subscribeMsg = mapOf(
            "op" to "subscribe",
            "topic" to topic,
            "type" to type
        )
        val jsonString = kotlinx.serialization.json.Json.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            subscribeMsg
        )
        send(jsonString)
    }

    fun handleIncomingMessage(topic: String, message: String) {
        topicCallbacks[topic]?.invoke(message)
    }

}