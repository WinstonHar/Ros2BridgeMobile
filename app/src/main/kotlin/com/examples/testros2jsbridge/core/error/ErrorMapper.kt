package com.examples.testros2jsbridge.core.error

import com.examples.testros2jsbridge.domain.model.RosId

class ErrorMapper {

    fun map(exception: Throwable): String {
        return when (exception) {
            is RosConnectionException -> "Connection error: ${exception.message}"
            is RosMessageException -> "Message error: ${exception.message}"
            else -> "Unknown exception: ${exception.message}"
        }
    }
}