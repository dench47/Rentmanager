package com.rentmanager.app.data.api

import com.google.gson.annotations.SerializedName
import com.rentmanager.app.data.model.TenantDocumentDto
import com.rentmanager.app.data.model.TenantDto
import retrofit2.Response
import retrofit2.http.*

interface TenantApi {

    @GET("tenants")
    suspend fun getTenants(): Response<List<TenantDto>>

    @GET("tenants/{id}")
    suspend fun getTenant(@Path("id") id: String): Response<TenantDto>

    /** Карточка арендатора целиком (арендатор + брони с объектами) — один запрос */
    @GET("tenants/{id}/card")
    suspend fun getTenantCard(@Path("id") id: String): Response<TenantDto>

    @POST("tenants")
    suspend fun createTenant(@Body tenant: TenantDto): Response<TenantDto>

    /** Редактирование карточки арендатора (канвас «14») */
    @PUT("tenants/{id}")
    suspend fun updateTenant(@Path("id") id: String, @Body tenant: TenantDto): Response<TenantDto>

    @DELETE("tenants/{id}")
    suspend fun deleteTenant(@Path("id") id: String): Response<Unit>

    /** «Отменить удаление» (Figma 3014:22433) */
    @POST("tenants/{id}/restore")
    suspend fun restoreTenant(@Path("id") id: String): Response<TenantDto>

    @GET("tenant/landlords")
    suspend fun getLandlords(): Response<List<UserSearchResult>>

    // ---- Документы карточки (канвас «14», 2983:42232 / шит 2983:44896) ----

    /** Метаданные после загрузки файла в хранилище (POST /upload?folder=documents) */
    @POST("tenants/{id}/documents")
    suspend fun addTenantDocument(
        @Path("id") id: String,
        @Body body: AddTenantDocumentRequest
    ): Response<TenantDocumentDto>

    @DELETE("tenants/{id}/documents/{docId}")
    suspend fun deleteTenantDocument(
        @Path("id") id: String,
        @Path("docId") docId: String
    ): Response<Unit>
}

/** Тело POST /tenants/{id}/documents */
data class AddTenantDocumentRequest(
    @SerializedName("name") val name: String,
    @SerializedName("file_type") val fileType: String?,
    @SerializedName("url") val url: String,
    @SerializedName("size") val size: Long
)