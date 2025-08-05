
package com.examples.testros2jsbridge.data.remote.rosbridge

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * Handles WebSocket events for rosbridge connection.
 * Delegates business logic to higher layers.
 */
class RosbridgeWebSocketListener(
    private val onOpen: (() -> Unit)? = null,
    private val onMessage: ((String) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null,
    private val onClosing: (() -> Unit)? = null,
    private val onClosed: (() -> Unit)? = null
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        onOpen?.invoke()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        onMessage?.invoke(text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        //no op
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onFailure?.invoke(t)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        onClosing?.invoke()
        webSocket.close(code, reason)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        onClosed?.invoke()
    }
}