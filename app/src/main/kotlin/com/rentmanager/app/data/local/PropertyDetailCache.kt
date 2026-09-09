package com.rentmanager.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.rentmanager.app.data.model.PaymentScheduleDto
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
        val tenantCompany: String = "",
        /** График платежей: без него «Арендная плата» при входе в карточку
         *  мелькает ставкой объявления, пока график не придёт с сервера */
        val schedule: PaymentScheduleDto? = null,
        /** Статистика «Аренда и платежи» (плашка задолженности, срок аренды):
         *  кэшируется, чтобы карточка не мигала дефолтами при входе */
        val hasDebt: Boolean = false,
        val debtAmount: Double = 0.0,
        val lastBookingEnd: String? = null
    )

    fun load(propertyId: String): Entry? {
        val json = prefs.getString(propertyId, null) ?: return null
        return try { gson.fromJson(json, Entry::class.java) } catch (_: Exception) { null }
    }

    /** Сохраняет объект (без арендатора) — используется при создании/загрузке списка.
     *  График и статистику не затирает: они обновляются через savePaymentStats. */
    fun saveProperty(dto: PropertyDto) {
        try {
            val existing = load(dto.id)
            save(
                Entry(
                    property = dto,
                    tenantName = existing?.tenantName ?: "",
                    tenantPhone = existing?.tenantPhone ?: "",
                    tenantCompany = existing?.tenantCompany ?: "",
                    schedule = existing?.schedule,
                    hasDebt = existing?.hasDebt ?: false,
                    debtAmount = existing?.debtAmount ?: 0.0,
                    lastBookingEnd = existing?.lastBookingEnd
                )
            )
        } catch (_: Exception) { }
    }

    /** Обновляет график и статистику платежей в кэше (после загрузки с сервера). */
    fun savePaymentStats(
        propertyId: String,
        schedule: PaymentScheduleDto?,
        hasDebt: Boolean,
        debtAmount: Double,
        lastBookingEnd: String?
    ) {
        try {
            val existing = load(propertyId) ?: return
            save(
                existing.copy(
                    schedule = schedule,
                    hasDebt = hasDebt,
                    debtAmount = debtAmount,
                    lastBookingEnd = lastBookingEnd
                )
            )
        } catch (_: Exception) { }
    }

    fun save(entry: Entry) {
        try {
            prefs.edit().putString(entry.property.id, gson.toJson(entry)).apply()
        } catch (_: Exception) { }
    }

    /** Удаляет закэшированную карточку объекта (после удаления объекта). */
    fun remove(propertyId: String) {
        try {
            prefs.edit().remove(propertyId).apply()
        } catch (_: Exception) { }
    }
}
