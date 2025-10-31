package com.examples.testros2jsbridge.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.examples.testros2jsbridge.data.local.database.dao.*
import com.examples.testros2jsbridge.data.local.database.entities.*

@Database(
    entities = [
        SubscriberEntity::class,
        PublisherEntity::class,
        ConnectionEntity::class,
        GeometryMessageEntity::class,
        AppActionEntity::class,
        ButtonMapEntity::class,
        ButtonPresetsEntity::class,
        ControllerEntity::class,
        PresetButtonMapJunction::class,
        ControllerButtonFixedMapJunction::class
    ],
    version = 5,
    exportSchema = false
)
abstract class RosDatabase : RoomDatabase() {

    abstract fun subscriberDao(): SubscriberDao
    abstract fun publisherDao(): PublisherDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun geometryMessageDao(): GeometryMessageDao
    abstract fun appActionDao(): AppActionDao
    abstract fun controllerDao(): ControllerDao

    companion object {
        @Volatile
        private var INSTANCE: RosDatabase? = null

        fun getDatabase(context: Context): RosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RosDatabase::class.java,
                    "ros_database"
                )
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
