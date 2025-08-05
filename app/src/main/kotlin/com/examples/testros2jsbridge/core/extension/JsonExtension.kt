package com.examples.testros2jsbridge.core.extension

import kotlinx.serialization.*
import com.examples.testros2jsbridge.domain.model.Publisher

object JsonExtension {
    fun publisherToJson(publisher: Publisher): String {
        return Json.encodeToString(publisher)
    }

    fun jsonToPublisher(jsonString: String): Publisher? {
        return Json.decodeFromString<Publisher>(jsonString)
    }
}