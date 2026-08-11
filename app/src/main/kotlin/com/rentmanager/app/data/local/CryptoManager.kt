package com.rentmanager.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "secure_pin_store",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Ключ в Keystore сломан (очистка данных, переустановка) — удаляем и пересоздаём
            context.deleteSharedPreferences("secure_pin_store")
            EncryptedSharedPreferences.create(
                context,
                "secure_pin_store",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun savePin(pin: String) {
        prefs.edit().putString("user_pin", pin).apply()
    }

    fun getPin(): String? {
        return prefs.getString("user_pin", null)
    }

    fun clearPin() {
        prefs.edit().remove("user_pin").apply()
    }
}