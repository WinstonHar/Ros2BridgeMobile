package com.examples.testros2jsbridge.core.error

import com.examples.testros2jsbridge.domain.model.RosId
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.launch

class ErrorHandler {

    private val errorChannel = BroadcastChannel<String>(ChannelName) // ChannelName defined elsewhere

    fun handle(exception: Throwable) {
        // Broadcast the exception to all interested consumers
        errorChannel.send(exception.message ?: "Unknown error")
    }
}