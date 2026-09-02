package com.rentmanager.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.rentmanager.app.data.api.AuthApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Общая логика загрузки фотографий в S3 (папка "photos").
 * Используется при создании объекта и при добавлении фото в детальной карточке.
 *
 * Перед отправкой фото сжимается: длинная сторона до 1600px, JPEG 85%.
 * Кадры современной камеры весят 3–8 МБ и при загрузке на мобильном интернете
 * рвутся целиком (инцидент 02.09: «ошибка загрузки фото», сервер при этом был
 * здоров и мелкие запросы проходили).
 */
@Singleton
class PhotoUploader @Inject constructor(
    private val authApi: AuthApi,
    @ApplicationContext private val context: Context
) {
    suspend fun upload(uri: Uri): String = withContext(Dispatchers.IO) {
        val (bytes, mime) = readUploadBytes(uri)
        val part = MultipartBody.Part.createFormData(
            "file",
            "photo_${System.currentTimeMillis()}.jpg",
            bytes.toRequestBody(mime.toMediaTypeOrNull())
        )

        // Ретрай при сетевых сбоях/таймаутах — загрузка идемпотентна (новый ключ каждый раз)
        val resp = retryOnNetworkError { authApi.uploadPhoto(part) }
        if (!resp.isSuccessful) throw Exception("Ошибка загрузки фото")
        resp.body()!!.url
    }

    /** Байты для отправки: сжатый JPEG либо оригинал (мелкие/нечитаемые файлы). */
    private fun readUploadBytes(uri: Uri): Pair<ByteArray, String> {
        val resolver = context.contentResolver

        // Габариты без декодирования пикселей.
        // ВАЖНО: null-проверка потока — отдельно от результата decodeStream:
        // в inJustDecodeBounds decodeStream ВСЕГДА возвращает null-Bitmap
        fun openStream() = resolver.openInputStream(uri)
            ?: throw Exception("Не удалось открыть фото")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream().use { BitmapFactory.decodeStream(it, null, bounds) }
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight

        // Не картинка или формат не декодируется на этом устройстве — как есть
        if (srcW <= 0 || srcH <= 0) {
            return readOriginal(uri) to (resolver.getType(uri) ?: "image/jpeg")
        }

        // Уже маленькое лёгкое фото не пережимаем (качество и EXIF сохраняются)
        val srcSize = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (maxOf(srcW, srcH) <= MAX_SIDE && srcSize in 1..PASS_THROUGH_BYTES) {
            return readOriginal(uri) to (resolver.getType(uri) ?: "image/jpeg")
        }

        // inSampleSize: декодируем сразу уменьшенным (защита от OOM на 50 Мп кадрах)
        var sample = 1
        var w = srcW
        var h = srcH
        while (w / 2 >= MAX_SIDE && h / 2 >= MAX_SIDE) {
            w /= 2
            h /= 2
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = openStream().use { BitmapFactory.decodeStream(it, null, opts) }
        // Декодировать не удалось — отдаём оригинал как есть
            ?: return readOriginal(uri) to (resolver.getType(uri) ?: "image/jpeg")

        // Поворот по EXIF + точный даунскейл до MAX_SIDE — одной матрицей
        val scale = if (maxOf(decoded.width, decoded.height) > MAX_SIDE) {
            MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height)
        } else 1f
        val matrix = Matrix().apply {
            postRotate(exifRotationDegrees(uri))
            postScale(scale, scale)
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (result != decoded) decoded.recycle()

        val out = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        result.recycle()
        return out.toByteArray() to "image/jpeg"
    }

    private fun readOriginal(uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Не удалось открыть фото")

    /** Угол поворота из EXIF (портретные кадры камеры хранятся лежащими + флаг). */
    private fun exifRotationDegrees(uri: Uri): Float {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    private companion object {
        const val MAX_SIDE = 1600                // предел длинной стороны, px
        const val JPEG_QUALITY = 85
        const val PASS_THROUGH_BYTES = 300_000L  // меньшие оригиналы грузим как есть
    }
}
