package com.examples.testros2jsbridge.domain.model

data class ButtonPreset(
    val presetId: Int,
    val name: String,
    val buttonMaps: List<ButtonMap>
)
