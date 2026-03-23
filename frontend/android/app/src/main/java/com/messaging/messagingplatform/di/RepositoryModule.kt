package com.messaging.messagingplatform.di

import com.messaging.messagingplatform.data.repository.AuthRepositoryImpl
import com.messaging.messagingplatform.data.repository.ChannelRepositoryImpl
import com.messaging.messagingplatform.data.repository.MessageRepositoryImpl
import com.messaging.messagingplatform.domain.repository.AuthRepository
import com.messaging.messagingplatform.domain.repository.ChannelRepository
import com.messaging.messagingplatform.domain.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain interfaces to their data implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

}