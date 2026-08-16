package com.rentmanager.app.data.api

import com.google.gson.annotations.SerializedName
import com.rentmanager.app.data.model.BookingDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class CreateBookingRequest(
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("source") val source: String = "manual"
)

interface BookingApi {

    @GET("properties/{id}/bookings")
    suspend fun getBookings(@Path("id") propertyId: String): Response<List<BookingDto>>

    @POST("properties/{id}/bookings")
    suspend fun createBooking(
        @Path("id") propertyId: String,
        @Body request: CreateBookingRequest
    ): Response<BookingDto>

    @PUT("properties/{id}/bookings/{bookingId}")
    suspend fun updateBooking(
        @Path("id") propertyId: String,
        @Path("bookingId") bookingId: String,
        @Body request: CreateBookingRequest
    ): Response<BookingDto>

    @DELETE("properties/{id}/bookings/{bookingId}")
    suspend fun deleteBooking(
        @Path("id") propertyId: String,
        @Path("bookingId") bookingId: String
    ): Response<Unit>
}
