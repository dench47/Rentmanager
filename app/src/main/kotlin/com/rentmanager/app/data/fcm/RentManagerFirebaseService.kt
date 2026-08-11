package com.rentmanager.app.data.fcm

import android.util.Log
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
        // Принудительно запрашиваем токен — Firebase кеширует и не дёргает onNewToken при повторных запусках
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onNewToken(task.result)
            } else {
                Log.e("FCM", "Failed to get token: ${task.exception?.message}")
            }
        }
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
        }
    }
}

