package com.rentmanager.app.data.local

import android.content.Context
import android.content.SharedPreferences
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
            if (value != null) prefs.edit().putString("access_token", value).commit()
            else prefs.edit().remove("access_token").commit()
        }

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) {
            prefs.edit().putString("refresh_token", value).commit()
        }

    var userName: String?
        get() = prefs.getString("user_name", null)
        set(value) = prefs.edit().putString("user_name", value).apply()

    var phone: String?
        get() = prefs.getString("phone", null)
        set(value) = prefs.edit().putString("phone", value).apply()

    var defaultStartScreen: String
        get() = prefs.getString("default_start_screen", "") ?: ""
        set(value) = prefs.edit().putString("default_start_screen", value).apply()

    var hasPassword: Boolean
        get() = prefs.getBoolean("has_password", false)
        set(value) = prefs.edit().putBoolean("has_password", value).apply()

    /**
     * Локальный тумблер «Требовать PIN на этом устройстве».
     * НЕ удаляет серверный PIN — тот остаётся для входа с новых устройств.
     * При выключенном флаге приложение не спрашивает PIN при открытии на этом устройстве.
     */
    var localPinEnabled: Boolean
        get() = prefs.getBoolean("local_pin_enabled", true)
        set(value) = prefs.edit().putBoolean("local_pin_enabled", value).apply()

    var selectedRole: String?
        get() = prefs.getString("selected_role", null)
        set(value) {
            if (value != null) prefs.edit().putString("selected_role", value).apply()
            else prefs.edit().remove("selected_role").apply()
        }

    var avatarUrl: String?
        get() = prefs.getString("avatar_url", null)
        set(value) = prefs.edit().putString("avatar_url", value).apply()

    // В памяти (не в SharedPreferences): сбрасывается при смерти процесса,
    // поэтому «Позже» забывается после свайпа и снова показывается на холодном старте.
    var lastUpdatePromptVersion: Int = 0

    var useBiometric: Boolean
        get() = prefs.getBoolean("use_biometric", false)
        set(value) = prefs.edit().putBoolean("use_biometric", value).apply()

    var lastPauseTimestamp: Long
        get() = prefs.getLong("last_pause_ts", 0L)
        set(value) = prefs.edit().putLong("last_pause_ts", value).apply()

    private val _requirePin = MutableStateFlow(prefs.getBoolean("require_pin", false))
    val requirePinFlow: StateFlow<Boolean> = _requirePin.asStateFlow()

    var requirePin: Boolean
        get() = _requirePin.value
        set(value) {
            _requirePin.value = value
            prefs.edit().putBoolean("require_pin", value).apply()
        }

    var lastRoute: String?
        get() = prefs.getString("last_route", null)
        set(value) = prefs.edit().putString("last_route", value).apply()

    var fcmToken: String?
        get() = prefs.getString("fcm_token", null)
        set(value) = prefs.edit().putString("fcm_token", value).apply()

    fun clear() {
        val savedFcmToken = fcmToken // сохраняем FCM-токен, чтобы не потерять при логауте
        _accessToken.value = null
        _requirePin.value = false
        prefs.edit().clear().apply()
        if (savedFcmToken != null) {
            prefs.edit().putString("fcm_token", savedFcmToken).apply()
        }
    }
}