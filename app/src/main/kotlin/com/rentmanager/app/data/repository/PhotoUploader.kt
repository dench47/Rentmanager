package com.rentmanager.app.data.repository

import android.content.Context
import android.net.Uri
import com.rentmanager.app.data.api.AuthApi
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Общая логика загрузки фотографий в S3 (папка "photos").
 * Используется при создании объекта и при добавлении фото в детальной карточке.
 */
@Singleton
class PhotoUploader @Inject constructor(
    private val authApi: AuthApi,
    @ApplicationContext private val context: Context
) {
    suspend fun upload(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file")
        val bytes = inputStream.readBytes()
        inputStream.close()

        val fileName = "photo_${System.currentTimeMillis()}.jpg"
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

        val resp = authApi.uploadPhoto(part)
        if (!resp.isSuccessful) throw Exception("Ошибка загрузки фото")
        return resp.body()!!.url
    }
}
