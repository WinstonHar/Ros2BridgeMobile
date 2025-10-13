package com.examples.testros2jsbridge.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.examples.testros2jsbridge.data.local.database.dao.ControllerDao
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMap
import com.examples.testros2jsbridge.data.local.database.entities.ButtonPresets
import com.examples.testros2jsbridge.data.local.database.entities.Controller
import com.examples.testros2jsbridge.data.local.database.entities.PresetButtonMapCrossRef

@Database(
    entities = [
        Controller::class,
        ButtonMap::class,
        ButtonPresets::class,
        PresetButtonMapCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class) // For the InputType enum
abstract class ControllerDatabase : RoomDatabase() {

    abstract fun controllerDao(): ControllerDao

    companion object {
        @Volatile
        private var INSTANCE: ControllerDatabase? = null

        fun getDatabase(context: Context): ControllerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ControllerDatabase::class.java,
                    "controller_database" // Name of the database file
                )
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}