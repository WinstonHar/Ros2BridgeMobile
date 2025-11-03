package com.examples.testros2jsbridge.data.remote.rosbridge

import com.examples.testros2jsbridge.core.util.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosbridgeClient @Inject constructor() {
    private var webSocket: WebSocket? = null
    private var url: String = ""
    private var baseListener: WebSocketListener? = null
    private val messageQueue = ConcurrentLinkedQueue<String>()

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun setUrl(newUrl: String) {
        this.url = newUrl
    }

    fun setListener(newListener: WebSocketListener) {
        this.baseListener = newListener
    }

    fun connect() {
        if (url.isEmpty()) {
            Logger.e("RosbridgeClient", "Cannot connect: URL not set")
            return
        }
        if (baseListener == null) {
            Logger.e("RosbridgeClient", "Cannot connect: Listener not set")
            return
        }

        disconnect()

        val request = Request.Builder()
            .url(url)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@RosbridgeClient.webSocket = webSocket
                baseListener?.onOpen(webSocket, response)
                Logger.d("RosbridgeClient", "Connection opened")
                while(messageQueue.isNotEmpty()) {
                    messageQueue.poll()?.let { send(it) }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                baseListener?.onMessage(webSocket, text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                baseListener?.onClosing(webSocket, code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@RosbridgeClient.webSocket = null
                baseListener?.onClosed(webSocket, code, reason)
                Logger.d("RosbridgeClient", "Connection closed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@RosbridgeClient.webSocket = null
                baseListener?.onFailure(webSocket, t, response)
                Logger.e("RosbridgeClient", "Connection failure", t)
            }
        }

        client.newWebSocket(request, listener)
        Logger.d("RosbridgeClient", "Connecting to $url")
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        messageQueue.clear()
        Logger.d("RosbridgeClient", "Disconnected")
    }

    fun send(message: String) {
        if (webSocket != null) {
            webSocket?.send(message)
        } else {
            messageQueue.add(message)
        }
    }

    fun isConnected(): Boolean = webSocket != null
}
