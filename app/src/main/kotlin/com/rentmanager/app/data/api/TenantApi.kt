package com.rentmanager.app.data.api

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
}