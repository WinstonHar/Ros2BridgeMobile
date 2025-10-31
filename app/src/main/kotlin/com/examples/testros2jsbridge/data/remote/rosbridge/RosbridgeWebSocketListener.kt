package com.examples.testros2jsbridge.data.remote.rosbridge

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * WebSocket listener that forwards events to provided callbacks
 */
class RosbridgeWebSocketListener(
    private val _onOpen: (WebSocket, Response) -> Unit = { _, _ -> },
    private val _onMessage: (String) -> Unit = {},
    private val _onFailure: (Throwable) -> Unit = {},
    private val _onClosing: () -> Unit = {},
    private val _onClosed: () -> Unit = {}
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        _onOpen(webSocket, response)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        _onMessage(text)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        _onFailure(t)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        _onClosing()
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        _onClosed()
    }
}
