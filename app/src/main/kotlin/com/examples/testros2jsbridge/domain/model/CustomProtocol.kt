package com.examples.testros2jsbridge.domain.model

data class CustomProtocol(
    val name: String,
    val importPath: String,
    val type: Type,
    val msgType: String? = null,
    val packageName: String,
    val content: String = ""
) {
    enum class Type { MSG, SRV, ACTION }
}

val CustomProtocol.typeString: String
    get() = when (type) {
        CustomProtocol.Type.MSG -> "$packageName/msg/$name"
        CustomProtocol.Type.SRV -> "$packageName/srv/$name"
        CustomProtocol.Type.ACTION -> "$packageName/action/$name"
    }
