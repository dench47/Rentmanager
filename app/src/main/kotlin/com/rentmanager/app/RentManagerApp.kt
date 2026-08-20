package com.rentmanager.app

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class RentManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // User-Agent для тайлов OpenStreetMap (требование политики OSM)
        Configuration.getInstance().userAgentValue = packageName
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