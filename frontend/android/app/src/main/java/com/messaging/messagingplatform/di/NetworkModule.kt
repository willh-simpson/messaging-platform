package com.messaging.messagingplatform.di

import android.content.Context
import com.google.gson.Gson
import com.messaging.messagingplatform.BuildConfig
import com.messaging.messagingplatform.data.api.AuthApi
import com.messaging.messagingplatform.data.api.ChannelApi
import com.messaging.messagingplatform.data.api.MessageApi
import com.messaging.messagingplatform.data.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    /**
     * OkHttpClient with 2 interceptors:
     *
     * Auth interceptor: reads JWT from DataStore and attaches it to requests as Bearer token.
     * Logging interceptor: logs req/res bodies in DEBUG builds only.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        authRepositoryImpl: AuthRepositoryImpl,
    ): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking {
                authRepositoryImpl.getToken()
            }

            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/vnd.messaging.v1+json")
                .apply {
                    if (token != null) addHeader("Authorization", "Bearer $token")
                }
                .build()

            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideAuthRepositoryImpl(
        @ApplicationContext context: Context,
        authApi: AuthApi,
    ): AuthRepositoryImpl =
        AuthRepositoryImpl(authApi, context)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideChannelApi(retrofit: Retrofit): ChannelApi =
        retrofit.create(ChannelApi::class.java)

    @Provides
    @Singleton
    fun provideMessageApi(retrofit: Retrofit): MessageApi =
        retrofit.create(MessageApi::class.java)
}