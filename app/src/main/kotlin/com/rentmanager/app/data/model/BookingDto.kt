package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class BookingDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("property_id") val propertyId: String? = null,
    @SerializedName("tenant_id") val tenantId: String? = null,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("source") val source: String = "manual",
    @SerializedName("created_by") val createdBy: String? = null
)
