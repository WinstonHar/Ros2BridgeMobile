package com.examples.testros2jsbridge.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.examples.testros2jsbridge.data.local.database.dao.AppActionDao
import com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
import com.examples.testros2jsbridge.data.local.database.dao.ControllerDao
import com.examples.testros2jsbridge.data.local.database.dao.GeometryMessageDao
import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
import com.examples.testros2jsbridge.data.local.database.entities.AppActionEntity
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMapEntity
import com.examples.testros2jsbridge.data.local.database.entities.ButtonPresetsEntity
import com.examples.testros2jsbridge.data.local.database.entities.ConnectionEntity
import com.examples.testros2jsbridge.data.local.database.entities.ControllerButtonFixedMapJunction
import com.examples.testros2jsbridge.data.local.database.entities.ControllerEntity
import com.examples.testros2jsbridge.data.local.database.entities.GeometryMessageEntity
import com.examples.testros2jsbridge.data.local.database.entities.PresetButtonMapJunction
import com.examples.testros2jsbridge.data.local.database.entities.PublisherEntity
import com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity

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
