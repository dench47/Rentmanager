package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class MeterDto(
    @SerializedName("id") val id: String,
    @SerializedName("property_id") val propertyId: String,
    @SerializedName("type") val type: String, // "hot_water", "cold_water", "electricity", "heat"
    @SerializedName("factory_number") val factoryNumber: String,
    @SerializedName("next_verification_date") val nextVerificationDate: String,
    @SerializedName("current_value") val currentValue: Double,
    @SerializedName("unit") val unit: String, // "м³", "кВт·ч", "Гкал"
    @SerializedName("submit_readings_by") val submitReadingsBy: String,
    @SerializedName("last_updated") val lastUpdated: String? = null,
    @SerializedName("remind_verification") val remindVerification: Boolean = false,
    @SerializedName("remind_readings") val remindReadings: Boolean = false
)
