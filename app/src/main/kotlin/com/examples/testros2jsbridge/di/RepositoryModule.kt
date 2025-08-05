package com.examples.testros2jsbridge.di

/*
Centralizes dependency wiring, makes testing possible
 */

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.examples.testros2jsbridge.data.repository.RosConnectionRepository
import com.examples.testros2jsbridge.data.repository.RosConnectionRepositoryImpl
import com.examples.testros2jsbridge.data.repository.RosMessageRepository
import com.examples.testros2jsbridge.data.repository.RosMessageRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRosConnectionRepository(
        impl: RosConnectionRepositoryImpl
    ): RosConnectionRepository

    @Binds
    @Singleton
    abstract fun bindRosMessageRepository(
        impl: RosMessageRepositoryImpl
    ): RosMessageRepository
}