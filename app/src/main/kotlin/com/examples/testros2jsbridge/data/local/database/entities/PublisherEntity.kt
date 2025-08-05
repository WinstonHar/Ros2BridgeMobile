package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "publisher")
data class PublisherEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val messageType: String,
    val message: String,
    val label: String?,
    val isEnabled: Boolean,
    val lastPublishedTimestamp: Long?,
    val presetGroup: String?
)