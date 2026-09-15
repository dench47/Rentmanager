package com.rentmanager.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val _accessToken = MutableStateFlow(prefs.getString("access_token", null))
    val accessTokenFlow: StateFlow<String?> = _accessToken.asStateFlow()

    var accessToken: String?
        get() = _accessToken.value
        set(value) {
            _accessToken.value = value
            if (value != null) prefs.edit(commit = true) { putString("access_token", value) }
            else prefs.edit(commit = true) { remove("access_token") }
        }

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) {
            prefs.edit(commit = true) { putString("refresh_token", value) }
        }

    var userName: String?
        get() = prefs.getString("user_name", null)
        set(value) = prefs.edit { putString("user_name", value) }

    var phone: String?
        get() = prefs.getString("phone", null)
        set(value) = prefs.edit { putString("phone", value) }

    var defaultStartScreen: String
        get() = prefs.getString("default_start_screen", "") ?: ""
        set(value) = prefs.edit { putString("default_start_screen", value) }

    var hasPassword: Boolean
        get() = prefs.getBoolean("has_password", false)
        set(value) = prefs.edit { putBoolean("has_password", value) }

    /**
     * Локальный тумблер «Требовать PIN на этом устройстве».
     * НЕ удаляет серверный PIN — тот остаётся для входа с новых устройств.
     * При выключенном флаге приложение не спрашивает PIN при открытии на этом устройстве.
     */
    var localPinEnabled: Boolean
        get() = prefs.getBoolean("local_pin_enabled", true)
        set(value) = prefs.edit { putBoolean("local_pin_enabled", value) }

    var selectedRole: String?
        get() = prefs.getString("selected_role", null)
        set(value) {
            if (value != null) prefs.edit { putString("selected_role", value) }
            else prefs.edit { remove("selected_role") }
        }

    var avatarUrl: String?
        get() = prefs.getString("avatar_url", null)
        set(value) = prefs.edit { putString("avatar_url", value) }

    // В памяти (не в SharedPreferences): сбрасывается при смерти процесса,
    // поэтому «Позже» забывается после свайпа и снова показывается на холодном старте.
    var lastUpdatePromptVersion: Int = 0

    var useBiometric: Boolean
        get() = prefs.getBoolean("use_biometric", false)
        set(value) = prefs.edit { putBoolean("use_biometric", value) }

    var lastPauseTimestamp: Long
        get() = prefs.getLong("last_pause_ts", 0L)
        set(value) = prefs.edit { putLong("last_pause_ts", value) }

    /** После нажатия «Позже» на промпте Telegram — не показываем его повторно
     *  даже при пересоздании ViewModel (смена экрана и возврат). */
    var telegramPromptDismissed: Boolean
        get() = prefs.getBoolean("telegram_prompt_dismissed", false)
        set(value) = prefs.edit { putBoolean("telegram_prompt_dismissed", value) }

    private val _requirePin = MutableStateFlow(prefs.getBoolean("require_pin", false))
    val requirePinFlow: StateFlow<Boolean> = _requirePin.asStateFlow()

    var requirePin: Boolean
        get() = _requirePin.value
        set(value) {
            _requirePin.value = value
            prefs.edit { putBoolean("require_pin", value) }
        }

    var lastRoute: String?
        get() = prefs.getString("last_route", null)
        set(value) = prefs.edit { putString("last_route", value) }

    var fcmToken: String?
        get() = prefs.getString("fcm_token", null)
        set(value) = prefs.edit { putString("fcm_token", value) }

    fun clear() {
        val savedFcmToken = fcmToken // сохраняем FCM-токен, чтобы не потерять при логауте
        _accessToken.value = null
        _requirePin.value = false
        prefs.edit { clear() }
        if (savedFcmToken != null) {
            prefs.edit { putString("fcm_token", savedFcmToken) }
        }
    }
}