package com.examples.testros2jsbridge.data.remote.protocol

import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryo.Kryo
import java.io.IOException

import com.examples.testros2jsbridge.domain.model.RosMessage

class RosProtocolHandler(
    private val listener: RosMessageListener? = null
) : Listener() {

    private val client = Client()
    private val kryo: Kryo = client.kryo

    init {
        // Register RosMessage with custom serializer
        kryo.register(RosMessage::class.java, MessageSerializer())
        client.addListener(this)
    }

    fun connect(ipAddress: String, port: Int, timeoutMs: Int = 5000) {
        client.start()
        try {
            client.connect(timeoutMs, ipAddress, port)
            listener?.onConnecting()
        } catch (e: IOException) {
            listener?.onError(e)
        }
    }

    fun sendMessage(message: RosMessage) {
        client.sendTCP(message)
    }

    override fun received(connection: Connection, obj: Any) {
        if (obj is RosMessage) {
            listener?.onMessageReceived(obj)
        }
    }

    override fun connected(connection: Connection) {
        listener?.onConnected()
    }

    override fun disconnected(connection: Connection) {
        listener?.onDisconnected()
    }

    fun disconnect() {
        client.stop()
        listener?.onDisconnected()
    }

    interface RosMessageListener {
        fun onMessageReceived(message: RosMessage)
        fun onConnected()
        fun onDisconnected()
        fun onConnecting() {}
        fun onError(e: Exception)
    }
}