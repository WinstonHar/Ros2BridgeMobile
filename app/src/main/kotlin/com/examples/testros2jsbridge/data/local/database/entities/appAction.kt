package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_action")
data class AppAction(
    @PrimaryKey(autoGenerate = true)
    val appActionId: Long = 0,
    val displayName: String,
    val rosTopic: String,
    val rosMessageType: String,
    val messageJsonTemplate: String,
    val rosProtocolType: RosProtocolType,
    val protocolPackageName: String?,
    val protocolName: String?
)