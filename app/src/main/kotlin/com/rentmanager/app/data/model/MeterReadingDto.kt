package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

/** Внесённое показание счётчика (запись истории). */
data class MeterReadingDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("meter_id") val meterId: String = "",
    @SerializedName("property_id") val propertyId: String = "",
    @SerializedName("value") val value: Double = 0.0,
    @SerializedName("unit") val unit: String = "",
    @SerializedName("date") val date: String = "" // YYYY-MM-DD
)

/** Тело запроса «внести показание». */
data class SubmitReadingRequest(
    @SerializedName("value") val value: Double
)
