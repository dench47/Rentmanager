package com.rentmanager.app.data.api

import com.rentmanager.app.data.model.PaymentDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FinanceApi {

    @GET("finance/report")
    suspend fun getReport(
        @Query("property_id") propertyId: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<PaymentDto>>
}