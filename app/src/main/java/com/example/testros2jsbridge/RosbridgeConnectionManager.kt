package com.example.testros2jsbridge

import android.os.Handler
import android.os.Looper
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/*
    RosbridgeConnectionManager manages the WebSocket connection to rosbridge, including connect/disconnect, message sending, and listener notification.
    Provides a singleton interface for all ROS2 network communication in the app.
*/

object RosbridgeConnectionManager {
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var url: String? = null
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val mainHandler = Handler(Looper.getMainLooper())
    var isConnected: Boolean = false
        private set

    interface Listener {
        fun onConnected()
        fun onDisconnected()
        fun onMessage(text: String)
        fun onError(error: String)
    }

    /*
        input:    listener - Listener
        output:   None
        remarks:  Adds a listener to receive connection and message events.
    */
    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    /*
        input:    listener - Listener
        output:   None
        remarks:  Removes a listener from receiving connection and message events.
    */
    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    /*
        input:    ip - String, port - Int
        output:   None
        remarks:  Connects to the rosbridge WebSocket server at the given IP and port.
    */
    fun connect(ip: String, port: Int) {
        val wsUrl = "ws://$ip:$port"
        android.util.Log.d("RosbridgeConnectionManager", "connect() called with $wsUrl")
        if (wsUrl == url && isConnected) {
            android.util.Log.d("RosbridgeConnectionManager", "Already connected to $wsUrl, skipping connect.")
            return
        }
        disconnect()
        url = wsUrl
        client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client!!.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                android.util.Log.d("RosbridgeConnectionManager", "WebSocket opened: $wsUrl")
                isConnected = true
                mainHandler.post { listeners.forEach { it.onConnected() } }
            }

            /*
                input:    webSocket - WebSocket, text - String
                output:   None
                remarks:  Called when a text message is received from rosbridge; notifies listeners.
            */
            override fun onMessage(webSocket: WebSocket, text: String) {
                mainHandler.post { listeners.forEach { it.onMessage(text) } }
            }

            /*
                input:    webSocket - WebSocket, bytes - ByteString
                output:   None
                remarks:  Called when a binary message is received from rosbridge; decodes and notifies listeners.
            */
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            /*
                input:    webSocket - WebSocket, t - Throwable, response - Response?
                output:   None
                remarks:  Called when the WebSocket connection fails; logs error and notifies listeners.
            */
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("RosbridgeConnectionManager", "WebSocket failure: ${t.message}", t)
                isConnected = false
                mainHandler.post { listeners.forEach { it.onError(t.message ?: "Unknown error") } }
            }
            
            /*
                input:    webSocket - WebSocket, code - Int, reason - String
                output:   None
                remarks:  Called when the WebSocket connection is closed; logs and notifies listeners.
            */
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.w("RosbridgeConnectionManager", "WebSocket closed: code=$code, reason=$reason")
                isConnected = false
                mainHandler.post { listeners.forEach { it.onDisconnected() } }
            }
        })
    }

    /*
        input:    None
        output:   None
        remarks:  Disconnects from the rosbridge WebSocket server and notifies listeners.
    */
    fun disconnect() {
        if (webSocket == null && !isConnected) return // Only log/close if actually connected
        android.util.Log.d("RosbridgeConnectionManager", "disconnect() called")
        webSocket?.close(1000, null)
        webSocket = null
        isConnected = false
        mainHandler.post { listeners.forEach { it.onDisconnected() } }
    }

    /*
        input:    json - JSONObject
        output:   None
        remarks:  Sends a JSON message to rosbridge over the WebSocket connection.
    */
    fun send(json: JSONObject) {
        webSocket?.send(json.toString())
    }

    /*
        input:    text - String
        output:   None
        remarks:  Sends a raw string message to rosbridge over the WebSocket connection.
    */
    fun sendRaw(text: String) {
        webSocket?.send(text)
    }
}
