package com.rentmanager.app.data.fcm

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rentmanager.app.MainActivity
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.RegisterDeviceRequest
import com.rentmanager.app.data.local.DeviceIdManager
import com.rentmanager.app.data.local.LoginApprovalEvents
import com.rentmanager.app.data.local.TenantEvents
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class RentManagerFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var authApi: AuthApi

    @Inject
    lateinit var tenantEvents: TenantEvents

    @Inject
    lateinit var loginApprovalEvents: LoginApprovalEvents

    @Inject
    lateinit var deviceIdManager: DeviceIdManager

    @Suppress("DEPRECATION") // FCM token API: миграция на register()/onRegistered(FID) — отдельная задача
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

    private fun showNotification(title: String, body: String, extras: Map<String, String> = emptyMap()) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            extras.forEach { (k, v) -> putExtra(k, v) }
        }
        val notification = NotificationCompat.Builder(this, "new_login_v2")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        NotificationManagerCompat.from(this).notify(1, notification)
    }

    private fun isAppInForeground(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val procs = am.runningAppProcesses ?: return false
        return procs.any {
            it.processName == packageName &&
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    // Время входа приходит как UNIX-метка (timestamp) — форматируем в часовом поясе телефона.
    // Если метки нет (старый сервер) — используем готовую строку body как есть.
    private fun buildNewLoginBody(message: RemoteMessage): String {
        val time = message.data["timestamp"]?.toLongOrNull()?.let { ts ->
            try {
                DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(ts))
            } catch (e: Exception) {
                null
            }
        }
        return if (time != null) "Замечен вход в $time"
        else (message.data["body"] ?: "Замечен вход на другом устройстве")
    }

    @Suppress("DEPRECATION") // устаревший API FCM: миграция на onRegistered(FID) — отдельная задача
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        tokenManager.fcmToken = token

        // Отправляем токен на сервер только при активной сессии:
        // после logout/delete_account токена нет — регистрация уйдёт
        // вместе с логином (см. VerifyViewModel.registerFcm), иначе получаем 401-шум.
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
        when (type) {
            "logout_all" -> {
                Log.d("FCM", "Received logout_all — clearing session")
                tokenManager.clear()
            }
            "new_login" -> {
                val title = message.data["title"] ?: "Новый вход в аккаунт"
                val body = buildNewLoginBody(message)
                showNotification(title, body)
            }
            // Device Trust: кто-то пытается войти с нового устройства — показываем диалог
            "login_request" -> {
                Log.d("FCM", "Received login_request")
                val requestId = message.data["request_id"] ?: ""
                val deviceName = message.data["device_name"] ?: ""
                loginApprovalEvents.emit(requestId = requestId, deviceName = deviceName)
                // Нотификацию показываем только если приложение свёрнуто/убито —
                // в форграунде диалог появится и так через StateFlow.
                if (!isAppInForeground()) {
                    val title = message.data["title"] ?: "Подтвердите вход"
                    val body = message.data["body"] ?: "Новое устройство пытается войти"
                    showNotification(title, body, mapOf("request_id" to requestId, "device_name" to deviceName))
                }
            }
            // Device Trust: список доверенных устройств изменился (новое устройство
            // подтверждено/отозвано) — открытые экраны обновляют список мгновенно
            "devices_changed" -> {
                Log.d("FCM", "Received devices_changed")
                loginApprovalEvents.emitDevicesChanged()
            }
            // Device Trust: устройство отозвано. Если это текущее устройство — мгновенный разлогин.
            // Иначе — просто обновляем список устройств на открытых экранах.
            "device_revoked" -> {
                val revokedId = message.data["device_id"] ?: ""
                Log.d("FCM", "Received device_revoked: $revokedId, my device: ${deviceIdManager.deviceId}")
                if (revokedId == deviceIdManager.deviceId) {
                    Log.d("FCM", "This device was revoked — logging out")
                    tokenManager.clear()
                } else {
                    loginApprovalEvents.emitDevicesChanged()
                }
            }
            "tenant_attached", "tenant_detached" -> {
                val title = message.data["title"] ?: "Обновление доступа к объекту"
                val body = message.data["body"] ?: ""
                showNotification(title, body)
                // Сигнал на обновление списка объектов у арендатора
                tenantEvents.notifyChanged()
            }
        }
    }
}

