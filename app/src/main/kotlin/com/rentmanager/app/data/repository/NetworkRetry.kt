package com.rentmanager.app.data.repository

import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Повторяет блок при сетевых сбоях (IOException, включая SocketTimeoutException)
 * с экспоненциальной паузой (1с → 2с → 4с) между попытками.
 *
 * Применять ТОЛЬКО к идемпотентным операциям: загрузка файлов (каждая попытка
 * создаёт новый ключ на сервере), GET-запросы и т.п. Для создания/обновления
 * объектов НЕ использовать, чтобы не плодить дубликаты.
 */
suspend fun <T> retryOnNetworkError(
    times: Int = 3,
    initialDelayMs: Long = 1000L,
    block: suspend () -> T
): T {
    require(times >= 0) { "times must be >= 0" }
    var attempt = 0
    var lastError: IOException? = null
    while (attempt <= times) {
        try {
            return block()
        } catch (e: IOException) {
            lastError = e
            attempt++
            if (attempt <= times) {
                delay(initialDelayMs * (1L shl (attempt - 1)))
            }
        }
    }
    throw lastError ?: IOException("Network error")
}
