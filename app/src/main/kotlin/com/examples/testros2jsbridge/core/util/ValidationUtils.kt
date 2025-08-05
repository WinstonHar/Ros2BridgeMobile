package com.examples.testros2jsbridge.core.util

object ValidationUtils {
    fun isValidIp(ip: String): Boolean = Regex("""^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$""").matches(ip)
    fun isValidPort(port: Int): Boolean = port in 1..65535
    fun isValidTopic(topic: String): Boolean = Regex("""^/[a-zA-Z0-9_/]+$""").matches(topic)
    // Add more as needed
}