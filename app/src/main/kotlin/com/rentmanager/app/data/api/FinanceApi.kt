package com.rentmanager.app.data.api

import com.rentmanager.app.data.model.PaymentDto
import com.rentmanager.app.data.model.PaymentRequisiteDto
import com.google.gson.annotations.SerializedName
import com.rentmanager.app.data.model.PaymentScheduleDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class CreatePaymentRequest(val amount: Double)

// Подписка: состояние, пополнение, промокоды
data class SubscriptionPromoDto(
    @SerializedName("code") val code: String,
    @SerializedName("rate") val rate: Double
)

data class SubscriptionStateDto(
    @SerializedName("balance") val balance: Double,
    @SerializedName("objects") val objects: Int,
    @SerializedName("base_rate") val baseRate: Double,
    @SerializedName("rate") val rate: Double,
    @SerializedName("daily_charge") val dailyCharge: Double,
    @SerializedName("promo") val promo: SubscriptionPromoDto? = null
)

data class TopUpDto(
    @SerializedName("added") val added: Double,
    @SerializedName("balance") val balance: Double
)

data class PromoApplyRequest(val code: String)

data class SubscriptionOperationDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("amount") val amount: Double,
    @SerializedName("status") val status: String = "done",
    @SerializedName("created_at") val createdAt: String? = null
)

interface FinanceApi {

    @GET("finance/report")
    suspend fun getReport(
        @Query("property_id") propertyId: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<PaymentDto>>

    @GET("payments/schedule")
    suspend fun getSchedules(): Response<List<PaymentScheduleDto>>

    // Подписка
    @GET("subscription")
    suspend fun subscriptionState(): Response<SubscriptionStateDto>

    @POST("subscription/topup")
    suspend fun topUp(): Response<TopUpDto>

    @POST("subscription/promo")
    suspend fun applyPromo(@Body request: PromoApplyRequest): Response<SubscriptionPromoDto>

    @GET("subscription/operations")
    suspend fun subscriptionOperations(): Response<List<SubscriptionOperationDto>>

    @GET("tenant/schedules")
    suspend fun getTenantSchedules(): Response<List<PaymentScheduleDto>>

    @POST("payments/schedule")
    suspend fun createSchedule(@Body request: PaymentScheduleDto): Response<PaymentScheduleDto>

    // Реквизиты арендодателя (общие на аккаунт)
    @GET("payments/requisites")
    suspend fun getRequisites(): Response<List<PaymentRequisiteDto>>

    @POST("payments/requisites")
    suspend fun createRequisite(@Body request: PaymentRequisiteDto): Response<PaymentRequisiteDto>

    @PUT("payments/requisites/{id}")
    suspend fun updateRequisite(@Path("id") id: String, @Body request: PaymentRequisiteDto): Response<PaymentRequisiteDto>

    @DELETE("payments/requisites/{id}")
    suspend fun deleteRequisite(@Path("id") id: String): Response<Unit>

    @GET("properties/{id}/payments")
    suspend fun listPayments(@Path("id") propertyId: String): Response<List<PaymentDto>>

    @POST("properties/{id}/payments")
    suspend fun createPayment(@Path("id") propertyId: String, @Body request: CreatePaymentRequest): Response<PaymentDto>
}