package com.examples.testros2jsbridge.di

/*
Eliminates manual ViewModel creation in MainActivity:103-108
 */

import android.content.Context
import androidx.room.Room
import com.examples.testros2jsbridge.data.local.database.RosDatabase
import com.examples.testros2jsbridge.data.local.database.dao.ConfigurationDao
import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
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
    fun provideRosDatabase(context: Context): RosDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            RosDatabase::class.java,
            "ros_database"
        ).build()
    }

    @Provides
    fun providePublisherDao(database: RosDatabase): PublisherDao {
        return database.publisherDao()
    }

    @Provides
    fun provideSubscriberDao(database: RosDatabase): SubscriberDao {
        return database.subscriberDao()
    }

    @Provides
    fun provideConfigurationDao(database: RosDatabase): ConfigurationDao {
        return database.configurationDao()
    }
}