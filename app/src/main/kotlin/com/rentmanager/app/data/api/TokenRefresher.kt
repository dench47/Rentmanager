package com.rentmanager.app.data.api

import android.util.Base64
import com.rentmanager.app.BuildConfig
import com.rentmanager.app.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefresher @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val mutex = Mutex()

    // Отдельный клиент без Authenticator, чтобы refresh не рекурсировал сам в себя.
    private val refreshApi: AuthApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApi::class.java)

    /**
     * Обновляет access/refresh токены. Сетевые ошибки и 5xx не разлогинивают —
     * сессия стирается только когда сервер явно отверг refresh-токен (401/400).
     */
    suspend fun refresh(expectedOldRefreshToken: String? = null): Boolean = mutex.withLock {
        if (expectedOldRefreshToken != null &&
            tokenManager.refreshToken != expectedOldRefreshToken
        ) {
            // Токен уже обновлён другим потоком
            return@withLock true
        }

        val current = tokenManager.refreshToken
        if (current == null) {
            tokenManager.clear()
            return@withLock false
        }

        val resp = try {
            refreshApi.refreshToken(RefreshTokenRequest(current))
        } catch (e: Exception) {
            return@withLock false
        }

        when {
            resp.isSuccessful -> {
                val body = resp.body()
                if (body != null) {
                    tokenManager.accessToken = body.accessToken
                    tokenManager.refreshToken = body.refreshToken
                    true
                } else {
                    false
                }
            }
            resp.code() == 401 || resp.code() == 400 -> {
                tokenManager.clear()
                false
            }
            else -> false
        }
    }

    /** Синхронная обёртка для OkHttp Authenticator (вызывается из его потока). */
    fun refreshBlocking(expectedOldRefreshToken: String? = null): Boolean =
        runBlocking { refresh(expectedOldRefreshToken) }

    /** true, если access-токен отсутствует или истекает в ближайшие [thresholdMs]. */
    fun shouldRefresh(thresholdMs: Long = 60_000L): Boolean {
        val token = tokenManager.accessToken ?: return false
        val exp = accessTokenExp(token) ?: return false
        return exp - System.currentTimeMillis() <= thresholdMs
    }

    private fun accessTokenExp(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
                Charsets.UTF_8
            )
            val exp = JSONObject(payload).optLong("exp", 0L)
            if (exp > 0) exp * 1000L else null
        } catch (e: Exception) {
            null
        }
    }
}