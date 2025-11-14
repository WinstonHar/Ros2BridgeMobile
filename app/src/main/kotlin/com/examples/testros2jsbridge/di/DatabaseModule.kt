package com.examples.testros2jsbridge.di

import android.content.Context
import androidx.room.Room
import com.examples.testros2jsbridge.data.local.database.RosDatabase
import com.examples.testros2jsbridge.data.local.database.dao.AppActionDao
import com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
import com.examples.testros2jsbridge.data.local.database.dao.ControllerDao
import com.examples.testros2jsbridge.data.local.database.dao.GeometryMessageDao
import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
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

    @Provides
    fun provideAppActionDao(database: RosDatabase): AppActionDao {
        return database.appActionDao()
    }

    @Provides
    fun provideControllerDao(database: RosDatabase): ControllerDao {
        return database.controllerDao()
    }
}
