package com.rentmanager.app.data.api

import com.google.gson.annotations.SerializedName
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.model.PhotoDto
import com.rentmanager.app.data.model.PropertyDto
import retrofit2.Response
import retrofit2.http.*

data class AttachTenantRequest(
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("tenant_id") val tenantId: String? = null
)

data class AddPhotoRequest(
    @SerializedName("url") val url: String
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

    @POST("properties/{id}/publish")
    suspend fun publishProperty(@Path("id") id: String): Response<PropertyDto>

    @POST("properties/{id}/unpublish")
    suspend fun unpublishProperty(@Path("id") id: String): Response<PropertyDto>

    @GET("properties/{id}/meters")
    suspend fun getMeters(@Path("id") id: String): Response<List<MeterDto>>

    @POST("properties/{id}/meters")
    suspend fun createMeter(@Path("id") id: String, @Body meter: MeterDto): Response<MeterDto>

    @POST("properties/{id}/attach_tenant")
    suspend fun attachTenant(@Path("id") id: String, @Body request: AttachTenantRequest): Response<MessageResponse>

    @POST("properties/{id}/photos")
    suspend fun addPhoto(@Path("id") id: String, @Body request: AddPhotoRequest): Response<PhotoDto>

    @DELETE("photos/{photoId}")
    suspend fun deletePhoto(@Path("photoId") photoId: String): Response<Unit>

    @GET("tenant/properties")
    suspend fun getTenantProperties(): Response<List<PropertyDto>>
}