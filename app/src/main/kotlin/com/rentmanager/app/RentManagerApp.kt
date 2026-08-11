package com.rentmanager.app

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RentManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Принудительно запрашиваем FCM-токен при старте приложения
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                getSharedPreferences("auth", MODE_PRIVATE)
                    .edit().putString("fcm_token", token).apply()
            }
        }
    }
}