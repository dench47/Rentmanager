package com.rentmanager.app.data.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.rentmanager.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

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

private const val TAG = "UpdateManager"

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApi,
    private val okHttpClient: OkHttpClient
) {
    /**
     * Returns true if the app was installed from a store (Play Store, RuStore, etc.)
     * In that case the store handles updates and we skip our own update check.
     */
    fun isInstalledFromStore(): Boolean {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        val result = when (installer) {
            "com.android.vending" -> true   // Google Play Store
            "com.rustore.sdk" -> true       // RuStore
            "com.huawei.appmarket" -> true  // AppGallery
            else -> false                   // Sideloaded APK
        }
        Log.d(TAG, "isInstalledFromStore: installer=$installer -> $result")
        return result
    }

    suspend fun checkForUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        // Skip self-update if installed from a store — the store handles updates
        if (isInstalledFromStore()) {
            Log.d(TAG, "checkForUpdate: installed from store, skip")
            return@withContext UpdateResult.UpToDate
        }
        try {
            val response = authApi.getVersion()
            if (response.isSuccessful) {
                val info = response.body()!!
                Log.d(
                    TAG,
                    "checkForUpdate: server=${info.versionCode}, app=${BuildConfig.VERSION_CODE}, " +
                        "min=${info.minClientVersion}, force=${info.forceUpdate}"
                )
                if (info.versionCode > BuildConfig.VERSION_CODE) {
                    UpdateResult.Available(info)
                } else {
                    UpdateResult.UpToDate
                }
            } else {
                Log.w(TAG, "checkForUpdate: HTTP ${response.code()}")
                UpdateResult.Error("Ошибка сервера")
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkForUpdate: network error: ${e.message}")
            UpdateResult.Error("Нет связи с сервером")
        }
    }

    fun isForced(info: VersionResponse): Boolean =
        info.forceUpdate || BuildConfig.VERSION_CODE < info.minClientVersion

    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Downloads APK file via OkHttp with progress callback.
     * Returns the downloaded file.
     */
    suspend fun downloadApk(
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(apkUrl).build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful || response.body == null) {
            throw Exception("HTTP ${response.code}")
        }

        val body = response.body!!
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()

        val dir = context.cacheDir
        val outFile = File(dir, "app-update.apk")
        if (outFile.exists()) outFile.delete()

        var downloaded = 0L
        FileOutputStream(outFile).use { outputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                if (contentLength > 0) {
                    val progress = downloaded.toFloat() / contentLength.toFloat()
                    withContext(Dispatchers.Main) { onProgress(progress) }
                }
            }
        }

        inputStream.close()
        response.close()
        outFile
    }

    /**
     * Opens system APK installer for the given file.
     */
    fun installApk(apkFile: File) {
        if (!apkFile.exists()) return

        val intent = Intent(Intent.ACTION_VIEW)
        val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}
