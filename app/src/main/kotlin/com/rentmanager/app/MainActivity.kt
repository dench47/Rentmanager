package com.rentmanager.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var updateManager: UpdateManager

    private var pendingUpdateInfo: VersionResponse? = null

    private val callPhonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied — no action needed */ }

    private val installPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // After returning from settings — try again if permission is now granted
            val info = pendingUpdateInfo
            if (info != null && updateManager.canInstallUnknownApps()) {
                pendingUpdateInfo = null
                updateManager.downloadAndInstall(info.apkUrl)
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
                        onDownload = {
                            val info = updateInfo!!
                            if (updateManager.canInstallUnknownApps()) {
                                updateManager.downloadAndInstall(info.apkUrl)
                            } else {
                                pendingUpdateInfo = info
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                        data = Uri.parse("package:$packageName")
                                    }
                                } else {
                                    Intent()
                                }
                                installPermissionLauncher.launch(intent)
                            }
                            updateInfo = null
                        }
                    )
                }

                RentManagerNavGraph(tokenManager = tokenManager)
            }
        }
    }
}