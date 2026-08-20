package com.rentmanager.app.data.repository

import com.rentmanager.app.data.api.GeoApi
import com.rentmanager.app.data.api.PhotonFeature
import javax.inject.Inject
import javax.inject.Singleton

/** Подсказка адреса для UI. */
data class AddressSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

@Singleton
class GeoRepository @Inject constructor(
    private val geoApi: GeoApi
) {
    suspend fun suggest(query: String): List<AddressSuggestion> {
        return try {
            val resp = geoApi.suggest(query)
            if (resp.isSuccessful) {
                resp.body()?.features.orEmpty().mapNotNull { it.toAddressSuggestion() }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

private fun PhotonFeature.toAddressSuggestion(): AddressSuggestion? {
    val lon = geometry?.coordinates?.getOrNull(0) ?: return null
    val lat = geometry?.coordinates?.getOrNull(1) ?: return null
    val p = properties ?: return null

    val streetFull = listOfNotNull(p.street, p.housenumber)
        .joinToString(" ")
        .trim()
    val locality = p.city ?: p.state ?: ""
    val parts = listOfNotNull(
        streetFull.ifBlank { null },
        locality.ifBlank { null },
        p.country
    )
    val display = if (parts.isEmpty()) (p.name ?: "Адрес") else parts.joinToString(", ")
    return AddressSuggestion(displayName = display, latitude = lat, longitude = lon)
}
