package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String? = null, // "landlord" / "tenant"
    @SerializedName("legal_name") val legalName: String? = null
)