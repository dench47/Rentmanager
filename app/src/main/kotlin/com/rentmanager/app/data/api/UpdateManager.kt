package com.rentmanager.app.data.api

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.rentmanager.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

data class VersionResponse(
    @com.google.gson.annotations.SerializedName("version_code") val versionCode: Int,
    @com.google.gson.annotations.SerializedName("version_name") val versionName: String,
    @com.google.gson.annotations.SerializedName("min_client_version") val minClientVersion: Int,
    @com.google.gson.annotations.SerializedName("apk_url") val apkUrl: String,
    @com.google.gson.annotations.SerializedName("force_update") val forceUpdate: Boolean,
    @com.google.gson.annotations.SerializedName("release_notes") val releaseNotes: String
)

sealed class UpdateResult {
    data class Available(val info: VersionResponse) : UpdateResult()
    data object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApi
) {
    private var downloadId: Long = 0

    suspend fun checkForUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val response = authApi.getVersion()
            if (response.isSuccessful) {
                val info = response.body()!!
                if (info.versionCode > BuildConfig.VERSION_CODE) {
                    UpdateResult.Available(info)
                } else {
                    UpdateResult.UpToDate
                }
            } else {
                UpdateResult.Error("Ошибка сервера")
            }
        } catch (e: Exception) {
            UpdateResult.Error("Нет связи с сервером")
        }
    }

    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun downloadAndInstall(apkUrl: String) {
        val destination = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "app-update.apk"
        )

        // Delete old file if exists
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle("Обновление приложения")
            .setDescription("Загрузка новой версии...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id != downloadId) return

                val query = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex >= 0) {
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            installApk(destination)
                        }
                    }
                }
                cursor.close()
                context.unregisterReceiver(this)
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), flags)
    }

    private fun installApk(apkFile: File) {
        if (!apkFile.exists()) return

        val intent = Intent(Intent.ACTION_VIEW)
        val fileUri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

        intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}