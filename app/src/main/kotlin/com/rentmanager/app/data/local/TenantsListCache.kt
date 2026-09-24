package com.rentmanager.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rentmanager.app.data.model.TenantDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кэш последнего списка арендаторов (SharedPreferences + JSON).
 * Канон «отображение сразу»: после перезапуска приложения список
 * рендерится из кэша в первый кадр, сеть подтверждает молча —
 * как TenantCardCache у карточки, но с переживанием процесса.
 */
@Singleton
class TenantsListCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("tenants_list_cache", Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    fun get(): List<TenantDto>? = runCatching {
        val json = prefs.getString(KEY, null) ?: return null
        gson.fromJson<List<TenantDto>>(json, object : TypeToken<List<TenantDto>>() {}.type)
    }.getOrNull()

    fun put(tenants: List<TenantDto>) {
        runCatching {
            prefs.edit().putString(KEY, gson.toJson(tenants)).apply()
        }
    }

    private companion object {
        const val KEY = "tenants"
    }
}
