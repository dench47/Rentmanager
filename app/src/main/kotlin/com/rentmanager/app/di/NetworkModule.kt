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

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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
            // Для остальных /auth/ запросов (set_password, updateProfile и др.)
            // 401 может означать протухший токен — разрешаем refresh.
            val path = response.request.url.encodedPath
            if (path.contains("/auth/verify_password")) {
                return@Authenticator null
            }

            val refreshToken = tokenManager.refreshToken
            if (refreshToken == null) {
                tokenManager.clear()
                return@Authenticator null
            }

            // Отдельный клиент для refresh, не зависит от основного графа
            val refreshApi = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApi::class.java)

            val refreshResp = try {
                runBlocking { refreshApi.refreshToken(RefreshTokenRequest(refreshToken)) }
            } catch (e: Exception) {
                tokenManager.clear()
                return@Authenticator null
            }

            if (refreshResp.isSuccessful) {
                val body = refreshResp.body()!!
                tokenManager.accessToken = body.accessToken
                tokenManager.refreshToken = body.refreshToken

                val newRequest = response.request.newBuilder()
                    .header("Authorization", "Bearer ${body.accessToken}")
                    .build()
                response.close()
                return@Authenticator newRequest
            }

            tokenManager.clear()
            null
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