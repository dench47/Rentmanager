package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class TenantDto(
    @SerializedName("id") val id: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("passport_data") val passportData: String? = null,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("active") val active: Boolean = true
)