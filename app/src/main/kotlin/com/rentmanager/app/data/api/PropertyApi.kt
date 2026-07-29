package com.rentmanager.app.data.api

import com.rentmanager.app.data.model.PropertyDto
import retrofit2.Response
import retrofit2.http.*

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
}