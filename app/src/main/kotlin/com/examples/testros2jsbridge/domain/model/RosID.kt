package com.examples.testros2jsbridge.domain.model

import kotlinx.serialization.Serializable

/*
A standard ID type used across the domain for consistency.
*/
@Serializable
data class RosId(val value: String) {
    init {
        require(!value.isBlank()) { "RosId value cannot be empty" }
    }

    val formattedValue: String
        get() = if (value.startsWith("/")) value else "/$value"
}
