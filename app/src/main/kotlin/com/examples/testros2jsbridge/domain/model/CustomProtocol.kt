package com.examples.testros2jsbridge.domain.model

data class CustomProtocol(
    val name: String,
    val importPath: String,
    val type: Type
) {
    enum class Type { MSG, SRV, ACTION }
}

