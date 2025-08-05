package com.examples.testros2jsbridge.di

/*
Removes hardcoded OkHttpClient creation in RosbridgeConnectionManager:64
 */

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeWebSocketListener

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideRosbridgeClient(okHttpClient: OkHttpClient): RosbridgeClient {
        return RosbridgeClient(okHttpClient)
    }

    @Provides
    fun provideRosbridgeWebSocketListener(): RosbridgeWebSocketListener {
        return RosbridgeWebSocketListener()
    }
}