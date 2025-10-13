package com.examples.testros2jsbridge.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.examples.testros2jsbridge.data.local.database.dao.AppActionDao
import com.examples.testros2jsbridge.data.local.database.entities.AppAction

@Database(
    entities = [AppAction::class], // Only includes the AppAction entity
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class) // For the RosProtocolType enum
abstract class AppActionDatabase : RoomDatabase() {

    abstract fun appActionDao(): AppActionDao

    companion object {
        @Volatile
        private var INSTANCE: AppActionDatabase? = null

        fun getDatabase(context: Context): AppActionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppActionDatabase::class.java,
                    "app_action_database" // Name of the database file
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}