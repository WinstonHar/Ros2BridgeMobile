package com.examples.testros2jsbridge.di

/*
Centralizes dependency wiring, makes testing possible
 */

import com.examples.testros2jsbridge.data.repository.AppActionRepositoryImpl
import com.examples.testros2jsbridge.data.repository.ControllerRepositoryImpl
import com.examples.testros2jsbridge.data.repository.ProtocolRepositoryImpl
import com.examples.testros2jsbridge.data.repository.RosConnectionRepositoryImpl
import com.examples.testros2jsbridge.data.repository.RosMessageRepositoryImpl
import com.examples.testros2jsbridge.data.repository.RosServiceRepositoryImpl
import com.examples.testros2jsbridge.data.repository.RosTopicRepositoryImpl
import com.examples.testros2jsbridge.data.repository.SubscriberRepositoryImpl
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import com.examples.testros2jsbridge.domain.repository.ProtocolRepository
import com.examples.testros2jsbridge.domain.repository.RosConnectionRepository
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import com.examples.testros2jsbridge.domain.repository.RosServiceRepository
import com.examples.testros2jsbridge.domain.repository.RosTopicRepository
import com.examples.testros2jsbridge.domain.repository.SubscriberRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    @Binds
    @Singleton
    abstract fun bindSubscriberRepository(
        impl: SubscriberRepositoryImpl
    ): SubscriberRepository

    @Binds
    @Singleton
    abstract fun bindProtocolRepository(
        impl: ProtocolRepositoryImpl
    ): ProtocolRepository

    @Binds
    @Singleton
    abstract fun bindAppActionRepository(
        impl: AppActionRepositoryImpl
    ): AppActionRepository

    @Binds
    @Singleton
    abstract fun bindRosTopicRepository(
        impl: RosTopicRepositoryImpl
    ): RosTopicRepository

    @Binds
    @Singleton
    abstract fun bindRosServiceRepository(
        impl: RosServiceRepositoryImpl
    ): RosServiceRepository

    @Binds
    @Singleton
    abstract fun bindControllerRepository(
        impl: ControllerRepositoryImpl
    ): ControllerRepository
}