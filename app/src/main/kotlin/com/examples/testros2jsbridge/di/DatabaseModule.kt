package com.examples.testros2jsbridge.di

/*
Eliminates manual ViewModel creation in MainActivity:103-108
 */

import android.content.Context
import androidx.room.Room
import com.examples.testros2jsbridge.data.local.database.AppActionDatabase
import com.examples.testros2jsbridge.data.local.database.ControllerDatabase
import com.examples.testros2jsbridge.data.local.database.dao.AppActionDao
import com.examples.testros2jsbridge.data.local.database.dao.ConfigurationDao
import com.examples.testros2jsbridge.data.local.database.dao.ControllerDao
import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideControllerDatabase(context: Context): ControllerDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ControllerDatabase::class.java,
            "controller_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAppActionDatabase(context: Context): AppActionDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppActionDatabase::class.java,
            "app_action_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideControllerDao(database: ControllerDatabase): ControllerDao {
        return database.controllerDao()
    }

    @Provides
    fun provideAppActionDao(database: AppActionDatabase): AppActionDao {
        return database.appActionDao()
    }
}