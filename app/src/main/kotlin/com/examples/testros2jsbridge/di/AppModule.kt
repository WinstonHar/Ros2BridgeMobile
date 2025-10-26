package com.examples.testros2jsbridge.di

import android.content.Context
import com.examples.testros2jsbridge.core.error.ErrorHandler
import com.examples.testros2jsbridge.data.local.database.dao.AppActionDao
import com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
import com.examples.testros2jsbridge.data.local.database.dao.ControllerConfigDao
import com.examples.testros2jsbridge.data.local.database.dao.GeometryMessageDao
import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.data.local.database.dao.SelectedConfigDao
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.repository.AppActionRepositoryImpl
import com.examples.testros2jsbridge.data.repository.ConfigurationRepositoryImpl
import com.examples.testros2jsbridge.data.repository.ControllerRepositoryImpl
import com.examples.testros2jsbridge.data.repository.PublisherRepositoryImpl
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import com.examples.testros2jsbridge.domain.repository.PublisherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context = context

    // Event dispatcher for rosbridge events
    object RosbridgeEventDispatcher {
        private val connectionListeners = mutableListOf<(Boolean) -> Unit>()
        private val messageListeners = mutableListOf<(String) -> Unit>()
        private val errorListeners = mutableListOf<(Throwable) -> Unit>()

        fun addConnectionListener(listener: (Boolean) -> Unit) {
            connectionListeners.add(listener)
        }
        fun addMessageListener(listener: (String) -> Unit) {
            messageListeners.add(listener)
        }
        fun addErrorListener(listener: (Throwable) -> Unit) {
            errorListeners.add(listener)
        }
        fun notifyConnection(isConnected: Boolean) {
            connectionListeners.forEach { it(isConnected) }
        }
        fun notifyMessage(msg: String) {
            messageListeners.forEach { it(msg) }
        }
        fun notifyError(t: Throwable) {
            errorListeners.forEach { it(t) }
        }
    }

    @Provides
    @Singleton
    fun provideErrorHandler(): ErrorHandler = ErrorHandler()

    @Provides
    @Singleton
    fun provideConfigurationRepository(app: Context): ConfigurationRepository =
        ConfigurationRepositoryImpl(app, app.getSharedPreferences("ros2_prefs", Context.MODE_PRIVATE))

    @Provides
    @Singleton
    fun provideControllerRepository(
        appActionDao: AppActionDao,
        selectedConfigDao: SelectedConfigDao,
        controllerConfigDao: ControllerConfigDao
    ): ControllerRepository = 
        ControllerRepositoryImpl(appActionDao, selectedConfigDao, controllerConfigDao)

    @Provides
    @Singleton
    fun providePublisherRepository(publisherDao: PublisherDao): PublisherRepository =
        PublisherRepositoryImpl(publisherDao)

    @Provides
    @Singleton
    fun provideAppActionRepository(
        rosbridgeClient: RosbridgeClient,
        connectionDao: ConnectionDao,
        geometryMessageDao: GeometryMessageDao,
        appActionDao: AppActionDao, // Added this line
        @ApplicationContext context: Context
    ): AppActionRepository {
        return AppActionRepositoryImpl(
            rosbridgeClient = rosbridgeClient,
            connectionDao = connectionDao,
            geometryMessageDao = geometryMessageDao,
            appActionDao = appActionDao, // And this line
            context = context
        )
    }
}
