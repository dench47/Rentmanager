package com.rentmanager.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    var userName: String?
        get() = prefs.getString("user_name", null)
        set(value) = prefs.edit().putString("user_name", value).apply()

    var phone: String?
        get() = prefs.getString("phone", null)
        set(value) = prefs.edit().putString("phone", value).apply()

    var defaultStartScreen: String
        get() = prefs.getString("default_start_screen", "verify") ?: "verify"
        set(value) = prefs.edit().putString("default_start_screen", value).apply()

    var selectedRole: String?
        get() = prefs.getString("selected_role", null)
        set(value) {
            if (value != null) prefs.edit().putString("selected_role", value).apply()
            else prefs.edit().remove("selected_role").apply()
        }

    var avatarUrl: String?
        get() = prefs.getString("avatar_url", null)
        set(value) = prefs.edit().putString("avatar_url", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}