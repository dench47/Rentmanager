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
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var updateManager: UpdateManager

    private var pendingUpdateInfo: VersionResponse? = null

    private val callPhonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied — no action needed */ }

    private val installPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val info = pendingUpdateInfo
            if (info != null && updateManager.canInstallUnknownApps()) {
                pendingUpdateInfo = null
                // Retry download
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }

        setContent {
            RentManagerTheme {
                var updateInfo by remember { mutableStateOf<VersionResponse?>(null) }
                var isDownloading by remember { mutableStateOf(false) }
                var downloadProgress by remember { mutableFloatStateOf(0f) }
                var downloadedFile by remember { mutableStateOf<File?>(null) }
                val scope = rememberCoroutineScope()

                // Проверка обновлений при запуске — только если авторизован
                LaunchedEffect(tokenManager.accessToken) {
                    if (tokenManager.accessToken == null) return@LaunchedEffect
                    val result = updateManager.checkForUpdate()
                    if (result is UpdateResult.Available) {
                        val info = result.info
                        if (info.versionCode > tokenManager.lastUpdatePromptVersion) {
                            tokenManager.lastUpdatePromptVersion = info.versionCode
                            updateInfo = info
                        }
                    }
                }

                // Показываем диалог если есть обновление и пользователь авторизован
                if (updateInfo != null && tokenManager.accessToken != null) {
                    UpdateDialog(
                        info = updateInfo!!,
                        isDownloading = isDownloading,
                        progress = downloadProgress,
                        onDownload = {
                            val info = updateInfo!!

                            // Проверяем разрешение на установку
                            if (!updateManager.canInstallUnknownApps()) {
                                pendingUpdateInfo = info
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                        data = Uri.parse("package:$packageName")
                                    }
                                } else {
                                    Intent()
                                }
                                installPermissionLauncher.launch(intent)
                                return@UpdateDialog
                            }

                            // Загружаем APK с прогрессом
                            isDownloading = true
                            scope.launch {
                                try {
                                    val file = updateManager.downloadApk(info.apkUrl) { progress ->
                                        downloadProgress = progress
                                    }
                                    downloadedFile = file
                                    updateInfo = null
                                    isDownloading = false
                                    updateManager.installApk(file)
                                } catch (_: Exception) {
                                    isDownloading = false
                                    downloadProgress = 0f
                                    updateInfo = null
                                }
                            }
                        }
                    )
                }

                RentManagerNavGraph(tokenManager = tokenManager)
            }
        }
    }
}