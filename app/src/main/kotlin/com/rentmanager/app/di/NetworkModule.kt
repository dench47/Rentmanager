package com.rentmanager.app.di

import com.rentmanager.app.BuildConfig
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.BookingApi
import com.rentmanager.app.data.api.ChatApi
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.api.GeoApi
import com.rentmanager.app.data.api.PropertyApi
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.api.TokenRefresher
import com.rentmanager.app.data.local.TokenManager
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenManager: TokenManager,
        tokenRefresher: TokenRefresher
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BASIC
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

            // Single-flight refresh. Сетевые ошибки/таймауты НЕ разлогинивают,
            // сессия стирается только при явном 401/400 от refresh.
            val refreshed = tokenRefresher.refreshBlocking(oldRefreshToken)
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
            // Большие таймауты: приложение должно работать и с плохой связью.
            // 30с — соединение, 60с — между пакетами ответа, 120с — между пакетами тела
            // запроса (загрузка фото), 300с — общий потолок на один вызов.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // serializeNulls: null-поля уходят в JSON явно. Сервер обновляет объект
        // по присутствию ключей (map в Update) — без этого очистка необязательных
        // полей (телефон, WiFi, ставка и т.п.) не сохранялась бы: Gson по умолчанию
        // просто выбрасывает null-поля из тела запроса
        val gson = GsonBuilder().serializeNulls().create()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
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

    @Provides
    @Singleton
    fun provideBookingApi(retrofit: Retrofit): BookingApi = retrofit.create(BookingApi::class.java)

    @Provides
    @Singleton
    fun provideGeoApi(): GeoApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "RentManagerApp/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeoApi::class.java)
    }
}