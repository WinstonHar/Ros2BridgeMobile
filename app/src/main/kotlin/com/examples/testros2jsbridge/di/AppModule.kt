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
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeWebSocketListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(app: Context): RosDatabase =
        Room.databaseBuilder(app, RosDatabase::class.java, "ros_database").build()

    @Provides
    fun provideSubscriberDao(db: RosDatabase): SubscriberDao = db.subscriberDao()

    @Provides
    fun providePublisherDao(db: RosDatabase): PublisherDao = db.publisherDao()
    
    // Simple event dispatcher for rosbridge events
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
    fun provideRosbridgeClient(app: Context): RosbridgeClient {
        val prefs = app.getSharedPreferences("app_config", Context.MODE_PRIVATE)
        val ip = prefs.getString("rosbridge_ip", "192.168.1.100") ?: "192.168.1.100"
        val port = prefs.getInt("rosbridge_port", 9090)
        val protocol = prefs.getString("rosbridge_protocol", "ws") ?: "ws"
        val url = "$protocol://$ip:$port"

        val listener = RosbridgeWebSocketListener(
            onOpen = {
                Logger.i("Rosbridge", "WebSocket connected: $url")
                RosbridgeEventDispatcher.notifyConnection(true)
            },
            onMessage = { msg ->
                Logger.d("Rosbridge", "Received message: $msg")
                RosbridgeEventDispatcher.notifyMessage(msg)
            },
            onFailure = { t ->
                Logger.e("Rosbridge", "WebSocket error: ${t.message}", t)
                RosbridgeEventDispatcher.notifyError(t)
                RosbridgeEventDispatcher.notifyConnection(false)
            },
            onClosing = {
                Logger.i("Rosbridge", "WebSocket closing")
                RosbridgeEventDispatcher.notifyConnection(false)
            },
            onClosed = {
                Logger.i("Rosbridge", "WebSocket closed")
                RosbridgeEventDispatcher.notifyConnection(false)
            }
        )
        return RosbridgeClient(url, listener)
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
    fun provideControllerRepository(app: Context): ControllerRepository =
        ControllerRepositoryImpl(app, app.getSharedPreferences("ros2_prefs", Context.MODE_PRIVATE))

    @Provides
    @Singleton
    fun providePublisherRepository(publisherDao: PublisherDao): PublisherRepository =
        PublisherRepositoryImpl(publisherDao)

    @Provides
    @Singleton
    fun provideRosConnectionRepository(rosbridgeClient: RosbridgeClient): RosConnectionRepository =
        RosConnectionRepositoryImpl(rosbridgeClient)

    @Provides
    @Singleton
    fun provideRosMessageRepository(rosbridgeClient: RosbridgeClient): RosMessageRepository =
        RosMessageRepositoryImpl(rosbridgeClient)

    @Provides
    @Singleton
    fun provideRosServiceRepository(rosbridgeClient: RosbridgeClient): RosServiceRepository =
        RosServiceRepositoryImpl(rosbridgeClient)

    @Provides
    @Singleton
    fun provideRosActionRepository(rosbridgeClient: RosbridgeClient): RosActionRepository =
        RosActionRepositoryImpl(
            rosbridgeClient,
            actions = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        )

    @Provides
    @Singleton
    fun provideRosTopicRepository(rosbridgeClient: RosbridgeClient): RosTopicRepository =
        RosTopicRepositoryImpl(rosbridgeClient)
}
