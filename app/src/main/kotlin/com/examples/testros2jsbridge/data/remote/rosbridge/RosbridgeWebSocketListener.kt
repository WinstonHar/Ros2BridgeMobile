package com.examples.testros2jsbridge.data.remote.rosbridge

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * WebSocket listener that forwards events to provided callbacks
 */
class RosbridgeWebSocketListener(
    private val onOpen: (WebSocket, Response) -> Unit = { _, _ -> },
    private val onMessage: (String) -> Unit = {},
    private val onFailure: (Throwable) -> Unit = {},
    private val onClosing: () -> Unit = {},
    private val onClosed: () -> Unit = {}
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        onOpen(webSocket, response)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        onMessage(text)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onFailure(t)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        onClosing()
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        onClosed()
    }
}
