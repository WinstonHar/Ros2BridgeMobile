package com.examples.testros2jsbridge.core.error

class ErrorMapper {

    fun map(exception: Throwable): String {
        return when (exception) {
            is RosConnectionException -> "Connection error: ${exception.message}"
            is RosMessageException -> "Message error: ${exception.message}"
            else -> "Unknown exception: ${exception.message}"
        }
    }
}