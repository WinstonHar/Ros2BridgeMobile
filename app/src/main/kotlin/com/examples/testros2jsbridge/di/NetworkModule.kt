package com.examples.testros2jsbridge.di

/*
Removes hardcoded OkHttpClient creation in RosbridgeConnectionManager:64
 */

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeWebSocketListener

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {


    @Provides
    @Singleton
    fun provideConnectionManager(
        connectionRepository: com.examples.testros2jsbridge.domain.repository.RosConnectionRepository,
        connectToRosUseCase: com.examples.testros2jsbridge.domain.usecase.connection.ConnectToRosUseCase,
        disconnectFromRosUseCase: com.examples.testros2jsbridge.domain.usecase.connection.DisconnectFromRosUseCase,
        rosbridgeClient: RosbridgeClient
    ): com.examples.testros2jsbridge.core.network.ConnectionManager =
        com.examples.testros2jsbridge.core.network.ConnectionManager(
            connectionRepository,
            connectToRosUseCase,
            disconnectFromRosUseCase,
            rosbridgeClient
        )


    @Provides
    @Singleton
    fun provideRosbridgeClient(
        @ApplicationContext app: Context,
        rosbridgeWebSocketListener: RosbridgeWebSocketListener
    ): RosbridgeClient {
        val prefs = app.getSharedPreferences("app_config", Context.MODE_PRIVATE)
        val ip = prefs.getString("rosbridge_ip", "192.168.1.100") ?: "192.168.1.100"
        val port = prefs.getInt("rosbridge_port", 9090)
        val protocol = prefs.getString("rosbridge_protocol", "ws") ?: "ws"
        val url = "$protocol://$ip:$port"
        return RosbridgeClient(
            url,
            listener = rosbridgeWebSocketListener
        )
    }

    @Provides
    fun provideRosbridgeWebSocketListener(): RosbridgeWebSocketListener {
        return RosbridgeWebSocketListener()
    }
}