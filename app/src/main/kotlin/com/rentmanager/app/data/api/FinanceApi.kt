package com.rentmanager.app.data.api

import com.rentmanager.app.data.model.PaymentDto
import com.rentmanager.app.data.model.PaymentScheduleDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class CreatePaymentRequest(val amount: Double)

interface FinanceApi {

    @GET("finance/report")
    suspend fun getReport(
        @Query("property_id") propertyId: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<PaymentDto>>

    @GET("payments/schedule")
    suspend fun getSchedules(): Response<List<PaymentScheduleDto>>

    @POST("payments/schedule")
    suspend fun createSchedule(@Body request: PaymentScheduleDto): Response<PaymentScheduleDto>

    @GET("properties/{id}/payments")
    suspend fun listPayments(@Path("id") propertyId: String): Response<List<PaymentDto>>

    @POST("properties/{id}/payments")
    suspend fun createPayment(@Path("id") propertyId: String, @Body request: CreatePaymentRequest): Response<PaymentDto>
}