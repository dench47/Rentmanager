package com.rentmanager.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

    private val callPhonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied — no action needed */ }

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

                // Проверка обновлений при запуске
                LaunchedEffect(Unit) {
                    val result = updateManager.checkForUpdate()
                    if (result is UpdateResult.Available) {
                        val info = result.info
                        // Показываем только если версия новее чем последняя показанная
                        if (info.versionCode > tokenManager.lastUpdatePromptVersion) {
                            tokenManager.lastUpdatePromptVersion = info.versionCode
                            updateInfo = info
                        }
                    }
                }

                // Показываем диалог если есть обновление
                if (updateInfo != null) {
                    UpdateDialog(
                        info = updateInfo!!,
                        onDownload = {
                            updateManager.downloadAndInstall(updateInfo!!.apkUrl)
                            updateInfo = null
                        }
                    )
                }

                RentManagerNavGraph(tokenManager = tokenManager)
            }
        }
    }
}
