package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("name") val name: String,
    @SerializedName("is_landlord") val isLandlord: Boolean = false,
    @SerializedName("is_tenant") val isTenant: Boolean = false,
    @SerializedName("legal_name") val legalName: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("token_version") val tokenVersion: Int = 0,
    @SerializedName("default_start_screen") val defaultStartScreen: String = ""
)
