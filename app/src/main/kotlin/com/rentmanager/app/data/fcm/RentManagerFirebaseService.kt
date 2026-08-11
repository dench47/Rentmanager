package com.rentmanager.app.data.fcm

import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.RegisterDeviceRequest
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RentManagerFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var authApi: AuthApi

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Принудительно запрашиваем токен — Firebase кеширует и не дёргает onNewToken при повторных запусках
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onNewToken(task.result)
            } else {
                Log.e("FCM", "Failed to get token: ${task.exception?.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "new_login_v2",
                "Входы в аккаунт",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых входах в ваш аккаунт"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, body: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(this, "new_login_v2")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(1, notification)
    }

    @Deprecated("Deprecated in Java")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        tokenManager.fcmToken = token

        // Отправляем токен на сервер (если не залогинен — 401, токен останется в prefs и уйдёт при логине)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authApi.registerDevice(RegisterDeviceRequest(token))
            } catch (e: Exception) {
                Log.e("FCM", "Failed to register device: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"]
        if (type == "logout_all") {
            Log.d("FCM", "Received logout_all — clearing session")
            tokenManager.clear()
        } else if (type == "new_login") {
            val title = message.data["title"] ?: "Новый вход в аккаунт"
            val body = message.data["body"] ?: "Замечен вход на другом устройстве"
            showNotification(title, body)
        }
    }
}

