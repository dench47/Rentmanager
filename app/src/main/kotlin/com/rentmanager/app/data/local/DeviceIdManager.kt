package com.rentmanager.app.data.local

import android.os.Build
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Стабильный идентификатор устройства для механизма доверенных устройств.
 *
 * - Генерируется один раз при первом запуске приложения.
 * - Хранится в отдельном SharedPreferences-файле, который НЕ очищается при logout
 *   (в отличие от TokenManager.clear()) — иначе сервер не узнает устройство повторно.
 * - Отправляется на сервер при каждом login/callcheck для проверки доверия.
 */
@Singleton
class DeviceIdManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("device_identity", Context.MODE_PRIVATE)

    val deviceId: String by lazy {
        prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also { id ->
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
    }

    /** Человекочитаемое имя устройства для списка доверенных устройств и push'ей. */
    val deviceName: String by lazy {
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: ""
        val model = Build.MODEL ?: ""
        listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .ifBlank { "Android-устройство" }
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}
