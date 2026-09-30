package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class TenantDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("passport_data") val passportData: String? = null,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("service_info") val serviceInfo: String? = null,
    @SerializedName("active") val active: Boolean = true,
    /** Аватарка живьём с аккаунта арендатора (если телефон совпал с юзером) */
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    /** Вторая строка списка: объект аренды и срок (канвас 13, канвас «Арендаторы») */
    @SerializedName("property_title") val propertyTitle: String? = null,
    /** Дата окончания аренды, ISO (YYYY-MM-DD) → «Арендует до 23.12.2027» */
    @SerializedName("rent_end_date") val rentEndDate: String? = null,
    /** Дата начала аренды по брони, ISO → «Аренда с 25.09.2026» */
    @SerializedName("rent_start_date") val rentStartDate: String? = null,
    /** Состояние аренды: active / booking / finished / none — подпись и фильтр */
    @SerializedName("rent_status") val rentStatus: String? = null,
    /** Брони с объектами — приходит из GET /tenants/{id}/card (один запрос на весь экран) */
    @SerializedName("bookings") val bookings: List<TenantBookingDto>? = null,
    /** Документы карточки — тем же запросом /card (канвас «14», 2983:42232) */
    @SerializedName("documents") val documents: List<TenantDocumentDto>? = null
)

/** Документ карточки арендатора: имя, тип (JPG/PDF…), ссылка в хранилище, размер. */
data class TenantDocumentDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("file_type") val fileType: String? = null,
    @SerializedName("url") val url: String,
    @SerializedName("size") val size: Long = 0,
    /** ISO-дата добавления (CreatedAt на сервере) → «добавлен DD.MM.YYYY» */
    @SerializedName("created_at") val createdAt: String? = null
)

data class TenantBookingDto(
    @SerializedName("property_id") val propertyId: String? = null,
    @SerializedName("property_name") val propertyName: String? = null,
    @SerializedName("property_address") val propertyAddress: String? = null,
    @SerializedName("property_photo") val propertyPhoto: String? = null,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String
)