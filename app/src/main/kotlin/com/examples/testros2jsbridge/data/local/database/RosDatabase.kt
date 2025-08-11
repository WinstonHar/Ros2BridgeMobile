package com.examples.testros2jsbridge.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.examples.testros2jsbridge.data.local.database.dao.ConfigurationDao
import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
import com.examples.testros2jsbridge.data.local.database.entities.ConfigurationEntity
import com.examples.testros2jsbridge.data.local.database.entities.PublisherEntity
import com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity

@Database(
    entities = [
        ConfigurationEntity::class,
    // PublisherEntity removed
        SubscriberEntity::class,
        com.examples.testros2jsbridge.data.local.database.entities.ConnectionEntity::class,
        com.examples.testros2jsbridge.data.local.database.entities.GeometryMessageEntity::class
    ],
    version = 4
)
abstract class RosDatabase : RoomDatabase() {
    abstract fun configurationDao(): ConfigurationDao
    // publisherDao removed
    abstract fun subscriberDao(): SubscriberDao
    abstract fun connectionDao(): com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
    abstract fun geometryMessageDao(): com.examples.testros2jsbridge.data.local.database.dao.GeometryMessageDao
}