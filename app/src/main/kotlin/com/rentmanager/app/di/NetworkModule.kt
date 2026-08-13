package com.rentmanager.app.di

import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.ChatApi
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.api.PropertyApi
import com.rentmanager.app.BuildConfig
import com.rentmanager.app.data.api.RefreshTokenRequest
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.local.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL = BuildConfig.API_BASE_URL
    private val refreshMutex = Mutex()

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.rentmanager.app.BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
        val authInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
            tokenManager.accessToken?.let { token ->
                request.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(request.build())
        }

        val authenticator = Authenticator { _, response ->
            // Не пытаемся обновить токен для verify_password:
            // 401 там означает неверный пароль, а не протухший токен.
            val path = response.request.url.encodedPath
            if (path.contains("/auth/verify_password")) {
                return@Authenticator null
            }

            val oldRefreshToken = tokenManager.refreshToken
            if (oldRefreshToken == null) {
                tokenManager.clear()
                return@Authenticator null
            }

            // Single-flight refresh: обновляет только один поток,
            // остальные ждут и переиспользуют уже обновлённый токен.
            val refreshed = runBlocking {
                refreshMutex.withLock {
                    // Повторно читаем — возможно, токен уже обновил другой поток
                    val currentRefreshToken = tokenManager.refreshToken
                    if (currentRefreshToken != oldRefreshToken) {
                        true // токен уже обновлён
                    } else {
                        // Отдельный клиент для refresh, не зависит от основного графа
                        val refreshApi = Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(OkHttpClient.Builder().build())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(AuthApi::class.java)

                        val refreshResp = try {
                            refreshApi.refreshToken(RefreshTokenRequest(currentRefreshToken))
                        } catch (e: Exception) {
                            tokenManager.clear()
                            return@withLock false
                        }

                        if (refreshResp.isSuccessful) {
                            val body = refreshResp.body()!!
                            tokenManager.accessToken = body.accessToken
                            tokenManager.refreshToken = body.refreshToken
                            true
                        } else {
                            tokenManager.clear()
                            false
                        }
                    }
                }
            }

            if (refreshed) {
                val newRequest = response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokenManager.accessToken}")
                    .build()
                response.close()
                newRequest
            } else {
                null
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun providePropertyApi(retrofit: Retrofit): PropertyApi = retrofit.create(PropertyApi::class.java)

    @Provides
    @Singleton
    fun provideTenantApi(retrofit: Retrofit): TenantApi = retrofit.create(TenantApi::class.java)

    @Provides
    @Singleton
    fun provideFinanceApi(retrofit: Retrofit): FinanceApi = retrofit.create(FinanceApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)
}