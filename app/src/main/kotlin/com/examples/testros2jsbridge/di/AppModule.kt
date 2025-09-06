package com.examples.testros2jsbridge.di

import android.content.Context
import androidx.room.Room
import com.examples.testros2jsbridge.data.local.database.RosDatabase
import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.data.repository.*
import com.examples.testros2jsbridge.domain.repository.*
import com.examples.testros2jsbridge.core.error.ErrorHandler
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
import com.examples.testros2jsbridge.data.local.database.dao.GeometryMessageDao
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeWebSocketListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppContext(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): android.content.Context = context

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
    // Removed duplicate RosDatabase and RosbridgeClient providers. These are now only in DatabaseModule and NetworkModule.

    @Provides
    @Singleton
    fun provideErrorHandler(): ErrorHandler = ErrorHandler()

    @Provides
    @Singleton
    fun provideConfigurationRepository(app: Context): ConfigurationRepository =
        ConfigurationRepositoryImpl(app, app.getSharedPreferences("ros2_prefs", Context.MODE_PRIVATE))

    @Provides
    @Singleton
    fun provideControllerRepository(app: Context): ControllerRepository =
        ControllerRepositoryImpl(app, app.getSharedPreferences("ros2_prefs", Context.MODE_PRIVATE))

    @Provides
    @Singleton
    fun providePublisherRepository(publisherDao: PublisherDao): PublisherRepository =
        PublisherRepositoryImpl(publisherDao)

    @Provides
    @Singleton
    fun provideAppActionRepository(
        rosbridgeClient: RosbridgeClient,
        connectionDao: ConnectionDao,
        geometryMessageDao: GeometryMessageDao
    ): AppActionRepository {
        return AppActionRepositoryImpl(
            rosbridgeClient = rosbridgeClient,
            connectionDao = connectionDao,
            geometryMessageDao = geometryMessageDao,
            messages = MutableStateFlow(emptyList())
        )
    }
}
