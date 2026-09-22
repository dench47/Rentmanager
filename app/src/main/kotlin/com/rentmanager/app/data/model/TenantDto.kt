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
    /** Брони с объектами — приходит из GET /tenants/{id}/card (один запрос на весь экран) */
    @SerializedName("bookings") val bookings: List<TenantBookingDto>? = null
)

data class TenantBookingDto(
    @SerializedName("property_id") val propertyId: String? = null,
    @SerializedName("property_name") val propertyName: String? = null,
    @SerializedName("property_address") val propertyAddress: String? = null,
    @SerializedName("property_photo") val propertyPhoto: String? = null,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String
)