package com.examples.testros2jsbridge.presentation.mapper

import com.examples.testros2jsbridge.domain.model.RosMessage

import com.examples.testros2jsbridge.presentation.state.GeometryUiState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

object MessageUiMapper {
    private val jsonFormatter = Json { prettyPrint = true; isLenient = true }

    fun toUiState(messages: List<RosMessage>): GeometryUiState =
        GeometryUiState(messages = messages)

    fun fromUiState(uiState: GeometryUiState): List<RosMessage> =
        uiState.messages

    fun formatMessageContent(message: RosMessage): String {
        return try {
            val jsonElement = jsonFormatter.parseToJsonElement(message.content)
            jsonFormatter.encodeToString(JsonObject.serializer(), jsonElement.jsonObject)
        } catch (_: Exception) {
            message.content
        }
    }

    fun formatTimestamp(message: RosMessage): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(message.timestamp))
    }

    fun extractKeyFields(message: RosMessage): Map<String, String> {
        return try {
            val jsonElement = jsonFormatter.parseToJsonElement(message.content)
            jsonElement.jsonObject.mapValues { (_, v) ->
                when (v) {
                    is JsonPrimitive -> v.content
                    else -> v.toString()
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}