package com.examples.testros2jsbridge.domain.model

data class CustomProtocol(
    val name: String,
    val importPath: String,
    val type: Type,
    val packageName: String
) {
    enum class Type { MSG, SRV, ACTION }
}

val CustomProtocol.typeString: String
    get() = when (type) {
        CustomProtocol.Type.MSG -> "ryan_msgs/msg/$name"
        CustomProtocol.Type.SRV -> "ryan_msgs/srv/$name"
        CustomProtocol.Type.ACTION -> "ryan_msgs/action/$name"
    }

