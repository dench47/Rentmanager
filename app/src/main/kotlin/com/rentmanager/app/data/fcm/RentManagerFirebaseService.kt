package com.rentmanager.app.data.fcm

import android.util.Log
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

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        tokenManager.fcmToken = token

        // Отправляем токен на сервер
        if (tokenManager.accessToken != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    authApi.registerDevice(RegisterDeviceRequest(token))
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to register device: ${e.message}")
                }
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

