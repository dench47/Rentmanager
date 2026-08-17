package com.rentmanager.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.rentmanager.app.data.model.PropertyDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кэш детальной карточки объекта (по propertyId).
 * Заполняется при создании объекта и при загрузке списка,
 * чтобы карточка открывалась мгновенно без спиннера.
 */
@Singleton
class PropertyDetailCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("property_detail_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    data class Entry(
        val property: PropertyDto,
        val tenantName: String = "",
        val tenantPhone: String = "",
        val tenantCompany: String = ""
    )

    fun load(propertyId: String): Entry? {
        val json = prefs.getString(propertyId, null) ?: return null
        return try { gson.fromJson(json, Entry::class.java) } catch (_: Exception) { null }
    }

    /** Сохраняет объект (без арендатора) — используется при создании/загрузке списка. */
    fun saveProperty(dto: PropertyDto) {
        val existing = load(dto.id)
        save(
            Entry(
                property = dto,
                tenantName = existing?.tenantName ?: "",
                tenantPhone = existing?.tenantPhone ?: "",
                tenantCompany = existing?.tenantCompany ?: ""
            )
        )
    }

    fun save(entry: Entry) {
        prefs.edit().putString(entry.property.id, gson.toJson(entry)).apply()
    }
}
