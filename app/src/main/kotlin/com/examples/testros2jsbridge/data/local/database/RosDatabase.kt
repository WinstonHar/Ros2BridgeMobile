package com.examples.testros2jsbridge.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.examples.testros2jsbridge.data.local.database.entities.ConfigurationEntity
import com.examples.testros2jsbridge.data.local.database.entities.PublisherEntity
import com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity

@Database(
    entities = [
        ConfigurationEntity::class,
        PublisherEntity::class,
        SubscriberEntity::class
    ],
    version = 1
)
abstract class RosDatabase : RoomDatabase() {
    abstract fun configurationDao(): ConfigurationDao
    abstract fun publisherDao(): PublisherDao
    abstract fun subscriberDao(): SubscriberDao
}