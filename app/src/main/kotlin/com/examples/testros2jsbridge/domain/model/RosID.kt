package com.examples.testros2jsbridge.domain.model

import kotlinx.serialization.Serializable

/*
A standard ID type used across the domain for consistency.
*/
@Serializable
data class RosId(val value: String) {
    init {
        require(!value.isBlank()) { "RosId value cannot be empty" }
        // Add more validation rules here (e.g., character restrictions)
    }
}
