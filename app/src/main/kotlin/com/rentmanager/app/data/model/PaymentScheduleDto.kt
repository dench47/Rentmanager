package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class PaymentScheduleDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("property_id") val propertyId: String = "",
    @SerializedName("day_of_month") val dayOfMonth: Int? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("type") val type: String = "auto",
    @SerializedName("custom_dates") val customDates: String? = null
)