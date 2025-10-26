package com.examples.testros2jsbridge.domain.model
enum class RosProtocolType {
    PUBLISHER,
    SERVICE_CLIENT,
    ACTION_CLIENT;

    companion object {
        fun fromString(type: String): RosProtocolType? = when (type.lowercase()) {
            "msgs" -> PUBLISHER
            "srv" -> SERVICE_CLIENT
            "action" -> ACTION_CLIENT
            else -> null
        }
    }
}