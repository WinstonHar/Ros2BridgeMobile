package com.examples.testros2jsbridge.core.util

import java.util.UUID

object UuidUtils {
    fun uuidToByteArrayString(uuid: String): String {
        return try {
            val u = UUID.fromString(uuid)
            val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
            bb.putLong(u.mostSignificantBits)
            bb.putLong(u.leastSignificantBits)
            bb.array().joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
        } catch (e: Exception) {
            Logger.w("UuidUtils", "Failed to parse UUID '$uuid'. Defaulting to zero array.", e)
            (0..15).joinToString(",", prefix = "[", postfix = "]") { "0" }
        }
    }
}