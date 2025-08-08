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
    // --- Add missing repository and service providers for Hilt ---
    @Provides
    @Singleton
    fun provideRosTopicRepository(
        rosbridgeClient: com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient,
        subscriberDao: com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
    ): com.examples.testros2jsbridge.domain.repository.RosTopicRepository =
        com.examples.testros2jsbridge.data.repository.RosTopicRepositoryImpl(rosbridgeClient, subscriberDao)

    @Provides
    @Singleton
    fun provideRosServiceRepository(
        rosbridgeClient: com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
    ): com.examples.testros2jsbridge.domain.repository.RosServiceRepository =
        com.examples.testros2jsbridge.data.repository.RosServiceRepositoryImpl(rosbridgeClient)

    @Provides
    @Singleton
    fun provideRosActionRepository(
        rosbridgeClient: com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
    ): com.examples.testros2jsbridge.domain.repository.RosActionRepository =
        com.examples.testros2jsbridge.data.repository.RosActionRepositoryImpl(
            rosbridgeClient,
            actions = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        )

    @Provides
    @Singleton
    fun provideProtocolRepositoryImpl(): com.examples.testros2jsbridge.data.repository.ProtocolRepositoryImpl =
        com.examples.testros2jsbridge.data.repository.ProtocolRepositoryImpl()

    @Provides
    @Singleton
    fun provideRosDatabase(context: Context): RosDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            RosDatabase::class.java,
            "ros_database"
        )
            .fallbackToDestructiveMigration()
            .build()
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

    @Provides
    fun provideConnectionDao(database: RosDatabase): com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao {
        return database.connectionDao()
    }

    @Provides
    fun provideGeometryMessageDao(database: RosDatabase): com.examples.testros2jsbridge.data.local.database.dao.GeometryMessageDao {
        return database.geometryMessageDao()
    }
}