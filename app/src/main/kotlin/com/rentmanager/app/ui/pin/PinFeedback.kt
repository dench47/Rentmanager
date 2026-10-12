package com.rentmanager.app.ui.pin

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

internal const val PIN_SHAKE_MS = 400L
internal const val PIN_ERROR_MS = 5000L

data class VerifyPasswordErrorResponse(
    val error: String? = null,
    @SerializedName("attempts_left") val attemptsLeft: Int? = null,
    @SerializedName("retry_after_seconds") val retryAfterSeconds: Long? = null
)

internal fun parsePinError(body: String?): VerifyPasswordErrorResponse? =
    runCatching { Gson().fromJson(body, VerifyPasswordErrorResponse::class.java) }.getOrNull()

internal fun pinErrorText(attemptsLeft: Int?): String = when (attemptsLeft) {
    1 -> "Неверный код. Осталась 1 попытка"
    2, 3 -> "Неверный код. Осталось $attemptsLeft попытки"
    else -> "Неверный код. Попробуйте ещё раз"
}

internal fun remainingLockSeconds(until: Long, now: Long): Long =
    ((until - now).coerceAtLeast(0L) + 999L) / 1000L

/** Плюрализация: 1 секунда / 2 секунды / 5 секунд (и так же минуты, часы). */
private fun plural(value: Long, one: String, few: String, many: String): String = when {
    value % 100 in 11L..14L -> many
    value % 10 == 1L -> one
    value % 10 in 2L..4L -> few
    else -> many
}

/**
 * Текст заголовка на время блокировки. Сервер отдаёт секунды, клиент форматирует
 * по-русски: «Повторите через 30 секунд», «…15 минут», «…1 час».
 */
internal fun pinLockText(seconds: Long): String {
    val tail = when {
        seconds < 60L -> "$seconds " + plural(seconds, "секунду", "секунды", "секунд")
        seconds < 3600L -> {
            val minutes = seconds / 60L
            "$minutes " + plural(minutes, "минуту", "минуты", "минут")
        }
        else -> {
            val hours = seconds / 3600L
            "$hours " + plural(hours, "час", "часа", "часов")
        }
    }
    return "Слишком много попыток. Повторите через $tail"
}