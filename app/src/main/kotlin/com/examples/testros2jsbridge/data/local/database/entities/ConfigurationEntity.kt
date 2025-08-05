package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuration")
data class ConfigurationEntity(
    @PrimaryKey val id: String,
    val rosServerUrl: String,
    val defaultPublishTopic: String,
    val defaultSubscribeTopic: String,
    val userName: String,
    val enableLogging: Boolean,
    val reconnectOnFailure: Boolean,
    val theme: String,
    val connectionTimeoutMs: Int,
    val maxRetryCount: Int,
    val lastUsedProtocolType: String,
    val messageHistorySize: Int,
    val exportProfilePath: String?,
    val importProfilePath: String?,
    val joystickAddressingMode: String,
    val language: String,
    val notificationsEnabled: Boolean
)