package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class PaymentScheduleDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("property_id") val propertyId: String = "",
    @SerializedName("day_of_month") val dayOfMonth: Int? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("type") val type: String = "auto",
    @SerializedName("custom_dates") val customDates: String? = null,
    @SerializedName("requisites") val requisites: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

/** Один график на объект; при дублях берём самую свежую запись по updated_at. */
fun List<PaymentScheduleDto>.forProperty(propertyId: String): PaymentScheduleDto? {
    val list = filter { it.propertyId == propertyId }
    if (list.isEmpty()) return null
    return list.maxByOrNull { it.updatedAt.orEmpty() }
}