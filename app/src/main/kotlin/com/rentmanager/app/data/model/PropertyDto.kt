package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class PropertyDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("area") val area: Double? = null,
    @SerializedName("photos") val photos: List<String> = emptyList(),
    @SerializedName("tenant_info") val tenantInfo: String? = null,
    @SerializedName("service_info") val serviceInfo: String? = null,
    @SerializedName("status") val status: String? = null, // "free" / "occupied"
    @SerializedName("rent_amount") val rentAmount: Double? = null,
    @SerializedName("rent_end_date") val rentEndDate: String? = null,
    @SerializedName("contract_number") val contractNumber: String? = null,
    @SerializedName("contract_date") val contractDate: String? = null,
    @SerializedName("tenant_id") val tenantId: String? = null
)