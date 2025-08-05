package com.examples.testros2jsbridge.core.extension

object StringExtension {

    fun formatString(format: String, args: List<Any>): String {
        return String.format(format, *args.toTypedArray())
    }

    fun toLowerCase(str: String): String {
        return str.lowercase()
    }
}