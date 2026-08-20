package com.rentmanager.app.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Photon (OpenStreetMap) — бесплатный сервис автодополнения адресов

data class PhotonResponse(
    val features: List<PhotonFeature>? = null
)

data class PhotonFeature(
    val geometry: PhotonGeometry? = null,
    val properties: PhotonProperties? = null
)

data class PhotonGeometry(
    val coordinates: List<Double>? = null // [lon, lat]
)

data class PhotonProperties(
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val type: String? = null,
    val extent: List<Double>? = null // [minLon, minLat, maxLon, maxLat]
)

interface GeoApi {
    @GET("api/")
    suspend fun suggest(
        @Query("q") query: String,
        @Query("limit") limit: Int = 30,
        @Query("bbox") bbox: String? = null
    ): Response<PhotonResponse>
}
