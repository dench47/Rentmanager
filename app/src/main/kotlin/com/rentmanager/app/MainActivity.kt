package com.rentmanager.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import com.rentmanager.app.data.api.ApproveLoginRequest
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.RegisterDeviceRequest
import com.rentmanager.app.data.api.TokenRefresher
import com.rentmanager.app.data.api.UpdateManager
import com.rentmanager.app.data.api.UpdateResult
import com.rentmanager.app.data.api.VersionResponse
import com.rentmanager.app.data.local.DeviceIdManager
import com.rentmanager.app.data.local.LoginApprovalEvents
import com.rentmanager.app.data.local.TokenManager
import com.rentmanager.app.ui.components.UpdateDialog
import com.rentmanager.app.ui.navigation.RentManagerNavGraph
import com.rentmanager.app.ui.theme.RentManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rentmanager.app.ui.components.ForcedUpdateScreen

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var tokenRefresher: TokenRefresher
    @Inject lateinit var updateManager: UpdateManager
    @Inject lateinit var authApi: AuthApi
    @Inject lateinit var loginApprovalEvents: LoginApprovalEvents

    private val retryDownloadSignal = mutableIntStateOf(0)

    private val callPhonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied — no action needed */ }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied */ }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied */ }


    private val installPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (updateManager.canInstallUnknownApps()) {
                retryDownloadSignal.intValue += 1
            }
        }

    companion object {
        private const val BACKGROUND_TIMEOUT_MS = 60_000L
    }

    override fun onPause() {
        super.onPause()
        tokenManager.lastPauseTimestamp = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        // Фоновая блокировка PIN'ом — только если локально включён запрос PIN
        if (tokenManager.hasPassword && tokenManager.localPinEnabled && tokenManager.accessToken != null) {
            val now = System.currentTimeMillis()
            val elapsed = now - tokenManager.lastPauseTimestamp
            if (tokenManager.lastPauseTimestamp > 0L && elapsed > BACKGROUND_TIMEOUT_MS) {
                tokenManager.requirePin = true
            }
        }
        // Проактивно обновляем access-токен, если он близок к истечению,
        // чтобы следующий запрос не падал с 401.
        if (tokenRefresher.shouldRefresh()) {
            CoroutineScope(Dispatchers.IO).launch {
                tokenRefresher.refresh()
            }
        }

        // Перерегистрируем FCM-токен, чтобы он не пропал после рестарта сервера
        registerFcmTokenIfLoggedIn()
    }

    private fun registerFcmTokenIfLoggedIn() {
        if (tokenManager.accessToken == null) return
        val cached = tokenManager.fcmToken
        if (cached != null) {
            registerDeviceOnServer(cached)
            return
        }
        // Токен ещё не сохранён — запрашиваем напрямую у Firebase
        @Suppress("DEPRECATION")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful && tokenManager.accessToken != null) {
                val token = task.result
                tokenManager.fcmToken = token
                registerDeviceOnServer(token)
            }
        }
    }

    private fun registerDeviceOnServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authApi.registerDevice(RegisterDeviceRequest(token))
            } catch (e: Exception) {
                Log.e("FCM", "Failed to register device: ${e.message}")
            }
        }
    }

    private fun requestLocationPermissionOnce() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("location_permission_asked", false)) return
        prefs.edit().putBoolean("location_permission_asked", true).apply()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Холодный старт — сбрасываем состояние фона, чтобы не требовать PIN повторно после обновления
        if (savedInstanceState == null) {
            tokenManager.lastRoute = null
            tokenManager.requirePin = false
            tokenManager.lastPauseTimestamp = 0L
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }

        // Запрашиваем разрешение на уведомления (Android 13+), чтобы пользователю не пришлось лезть в настройки
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Запрашиваем геолокацию один раз при первом запуске (для приоритизации подсказок адреса)
        requestLocationPermissionOnce()

        setContent {
            RentManagerTheme {
                var updateInfo by remember { mutableStateOf<VersionResponse?>(null) }
                var updateForced by remember { mutableStateOf(false) }
                var isDownloading by remember { mutableStateOf(false) }
                var downloadProgress by remember { mutableFloatStateOf(0f) }
                var updateError by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                fun checkUpdate() {
                    if (isDownloading || updateInfo != null) return
                    scope.launch {
                        val result = updateManager.checkForUpdate()
                        if (result is UpdateResult.Available) {
                            val info = result.info
                            val forced = updateManager.isForced(info)
                            if (!forced && info.versionCode <= tokenManager.lastUpdatePromptVersion) {
                                return@launch
                            }
                            updateInfo = info
                            updateForced = forced
                            updateError = null
                        }
                    }
                }

                fun startDownload() {
                    val info = updateInfo ?: return
                    if (!updateManager.canInstallUnknownApps()) {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        } else {
                            Intent()
                        }
                        installPermissionLauncher.launch(intent)
                        return
                    }
                    isDownloading = true
                    updateError = null
                    scope.launch {
                        try {
                            val file = updateManager.downloadApk(info.apkUrl) { progress ->
                                downloadProgress = progress
                            }
                            updateInfo = null
                            updateForced = false
                            isDownloading = false
                            updateManager.installApk(file)
                        } catch (_: Exception) {
                            isDownloading = false
                            downloadProgress = 0f
                            updateError = "Не удалось загрузить обновление. Проверьте интернет и нажмите «Повторить»."
                        }
                    }
                }

                fun dismissRecommended() {
                    val info = updateInfo ?: return
                    tokenManager.lastUpdatePromptVersion = info.versionCode
                    updateInfo = null
                    updateError = null
                }

                // Проверка при холодном старте
                LaunchedEffect(Unit) { checkUpdate() }

                // Проверка при возврате из фона
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) checkUpdate()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // Ретрай загрузки после выдачи разрешения на установку
                LaunchedEffect(retryDownloadSignal.intValue) {
                    if (retryDownloadSignal.intValue > 0) startDownload()
                }

                RentManagerNavGraph(tokenManager = tokenManager)

                // ===== Device Trust: диалог подтверждения входа с нового устройства =====
                var pendingLoginRequest by remember { mutableStateOf<LoginApprovalEvents.LoginRequest?>(null) }
                LaunchedEffect(Unit) {
                    loginApprovalEvents.requests.collect { pendingLoginRequest = it }
                }
                // При devices_changed (например, вход через Telegram с другого устройства)
                // сбрасываем диалог — запрос больше не актуален.
                LaunchedEffect(Unit) {
                    loginApprovalEvents.devicesChanged.collect { pendingLoginRequest = null }
                }
                pendingLoginRequest?.let { req ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { },
                        title = { Text("Подтвердите вход") },
                        text = {
                            Text(
                                "Попытка входа с устройства " +
                                    (req.deviceName?.takeIf { it.isNotBlank() } ?: "неизвестного устройства") +
                                    ". Это вы?"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        if (authApi.approveLogin(ApproveLoginRequest(req.requestId)).isSuccessful) {
                                            // Мгновенно обновляем список устройств на открытых экранах
                                            loginApprovalEvents.emitDevicesChanged()
                                        }
                                    } catch (_: Exception) {}
                                }
                                pendingLoginRequest = null
                            }) { Text("Подтвердить", color = Color(0xFF007AFF)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    try { authApi.denyLogin(ApproveLoginRequest(req.requestId)) } catch (_: Exception) {}
                                    // Отклонённая попытка тоже меняет состояние — синхронизируем UI
                                    loginApprovalEvents.emitDevicesChanged()
                                }
                                pendingLoginRequest = null
                            }) { Text("Отклонить", color = Color(0xFFE53935)) }
                        }
                    )
                }

                if (updateInfo != null) {
                    if (updateForced) {
                        ForcedUpdateScreen(
                            info = updateInfo!!,
                            isDownloading = isDownloading,
                            progress = downloadProgress,
                            errorMessage = updateError,
                            onDownload = { startDownload() },
                            onRetry = { startDownload() }
                        )
                    } else {
                        UpdateDialog(
                            info = updateInfo!!,
                            isDownloading = isDownloading,
                            progress = downloadProgress,
                            errorMessage = updateError,
                            onDownload = { startDownload() },
                            onLater = { dismissRecommended() },
                            onRetry = { startDownload() }
                        )
                    }
                }
            }
        }
    }
}