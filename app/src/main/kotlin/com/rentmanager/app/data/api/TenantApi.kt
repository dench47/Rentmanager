package com.rentmanager.app.data.api

import com.rentmanager.app.data.model.TenantDto
import retrofit2.Response
import retrofit2.http.*

interface TenantApi {

    @GET("tenants")
    suspend fun getTenants(): Response<List<TenantDto>>

    @GET("tenants/{id}")
    suspend fun getTenant(@Path("id") id: String): Response<TenantDto>

    @POST("tenants")
    suspend fun createTenant(@Body tenant: TenantDto): Response<TenantDto>

    @DELETE("tenants/{id}")
    suspend fun deleteTenant(@Path("id") id: String): Response<Unit>
}