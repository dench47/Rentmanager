package com.rentmanager.app.data.api

import com.google.gson.annotations.SerializedName
import com.rentmanager.app.data.model.PropertyDto
import retrofit2.Response
import retrofit2.http.*

data class AttachTenantRequest(
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("tenant_id") val tenantId: String? = null
)

interface PropertyApi {

    @GET("properties")
    suspend fun getProperties(): Response<List<PropertyDto>>

    @GET("properties/{id}")
    suspend fun getProperty(@Path("id") id: String): Response<PropertyDto>

    @POST("properties")
    suspend fun createProperty(@Body property: PropertyDto): Response<PropertyDto>

    @PUT("properties/{id}")
    suspend fun updateProperty(@Path("id") id: String, @Body property: PropertyDto): Response<PropertyDto>

    @DELETE("properties/{id}")
    suspend fun deleteProperty(@Path("id") id: String): Response<Unit>

    @POST("properties/{id}/attach_tenant")
    suspend fun attachTenant(@Path("id") id: String, @Body request: AttachTenantRequest): Response<MessageResponse>
}