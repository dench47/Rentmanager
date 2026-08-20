package com.rentmanager.app.data.repository

import com.rentmanager.app.data.api.GeoApi
import com.rentmanager.app.data.api.PhotonFeature
import javax.inject.Inject
import javax.inject.Singleton

/** Подсказка адреса для UI. */
data class AddressSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val extent: List<Double>? = null, // [minLon, minLat, maxLon, maxLat]
    val type: String? = null
)

@Singleton
class GeoRepository @Inject constructor(
    private val geoApi: GeoApi
) {
    suspend fun suggest(query: String, bbox: String? = null): List<AddressSuggestion> {
        return try {
            val resp = geoApi.suggest(query = query, bbox = bbox)
            if (resp.isSuccessful) {
                resp.body()?.features.orEmpty()
                    .mapNotNull { it.toAddressSuggestion() }
                    .distinctBy { it.displayName }
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

    // Собираем понятное название: сначала сам объект (name), затем иерархия
    val parts = mutableListOf<String>()
    if (streetFull.isNotBlank()) parts += streetFull
    if (!p.name.isNullOrBlank()) parts += p.name
    p.city?.let { if (it.isNotBlank()) parts += it }
    p.state?.let { if (it.isNotBlank()) parts += it }
    p.country?.let { if (it.isNotBlank()) parts += it }

    val unique = parts.distinct()
    val display = if (unique.isEmpty()) (p.name ?: "Адрес") else unique.joinToString(", ")
    return AddressSuggestion(
        displayName = display,
        latitude = lat,
        longitude = lon,
        extent = p.extent,
        type = p.type
    )
}
