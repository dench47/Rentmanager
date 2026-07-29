package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class PaymentDto(
    @SerializedName("id") val id: String,
    @SerializedName("property_id") val propertyId: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("date") val date: String,
    @SerializedName("status") val status: String, // "paid" / "pending" / "overdue"
    @SerializedName("type") val type: String // "income" / "expense"
)