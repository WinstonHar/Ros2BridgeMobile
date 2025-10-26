package com.examples.testros2jsbridge.di

/*
Eliminates manual ViewModel creation in MainActivity:103-108
 */

import android.content.Context
import androidx.room.Room
import com.examples.testros2jsbridge.data.local.database.AppActionDatabase
import com.examples.testros2jsbridge.data.local.database.ControllerDatabase
import com.examples.testros2jsbridge.data.local.database.RosDatabase
import com.examples.testros2jsbridge.data.local.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideControllerDatabase(@ApplicationContext context: Context): ControllerDatabase {
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
    fun provideAppActionDatabase(@ApplicationContext context: Context): AppActionDatabase {
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
    fun provideSelectedConfigDao(database: ControllerDatabase): SelectedConfigDao {
        return database.selectedConfigDao()
    }

    @Provides
    fun provideControllerConfigDao(database: ControllerDatabase): ControllerConfigDao {
        return database.controllerConfigDao()
    }

    @Provides
    fun provideAppActionDao(database: AppActionDatabase): AppActionDao {
        return database.appActionDao()
    }

    @Provides
    @Singleton
    fun provideRosDatabase(@ApplicationContext context: Context): RosDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            RosDatabase::class.java,
            "ros_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSubscriberDao(database: RosDatabase): SubscriberDao {
        return database.subscriberDao()
    }

    @Provides
    fun providePublisherDao(database: RosDatabase): PublisherDao {
        return database.publisherDao()
    }

    @Provides
    fun provideConnectionDao(database: RosDatabase): ConnectionDao {
        return database.connectionDao()
    }

    @Provides
    fun provideGeometryMessageDao(database: RosDatabase): GeometryMessageDao {
        return database.geometryMessageDao()
    }
}
