package com.rentmanager.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.rentmanager.app.data.api.UpdateManager
import com.rentmanager.app.data.api.UpdateResult
import com.rentmanager.app.data.api.VersionResponse
import com.rentmanager.app.data.local.TokenManager
import com.rentmanager.app.ui.components.UpdateDialog
import com.rentmanager.app.ui.navigation.RentManagerNavGraph
import com.rentmanager.app.ui.theme.RentManagerTheme
import dagger.hilt.android.AndroidEntryPoint
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
    @Inject lateinit var updateManager: UpdateManager

    private val retryDownloadSignal = mutableIntStateOf(0)

    private val callPhonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied — no action needed */ }

    private val notificationPermissionLauncher =
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
        if (tokenManager.hasPassword && tokenManager.accessToken != null) {
            val now = System.currentTimeMillis()
            val elapsed = now - tokenManager.lastPauseTimestamp
            if (tokenManager.lastPauseTimestamp > 0L && elapsed > BACKGROUND_TIMEOUT_MS) {
                tokenManager.requirePin = true
            }
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

                RentManagerNavGraph(tokenManager = tokenManager)
            }
        }
    }
}