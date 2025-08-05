package com.examples.testros2jsbridge.core.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object JsonUtils {
    inline fun <reified T> toJson(obj: T): String = Json.encodeToString(obj)
    inline fun <reified T> fromJson(json: String): T = Json.decodeFromString(json)
}