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
    suspend fun suggest(query: String, bbox: String? = null, lat: Double? = null, lon: Double? = null): List<AddressSuggestion> {
        return try {
            val resp = geoApi.suggest(query = query, bbox = bbox, lat = lat, lon = lon)
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

    /**
     * Обратный геокодинг по тапу карты. Photon по умолчанию отдаёт ближайший
     * POI (магазин, фирма, ЖК) — выбираем из кандидатов именно дом: сначала
     * фичу с улицей и номером дома, затем любое здание, и лишь потом остальное.
     */
    suspend fun reverse(lat: Double, lon: Double): AddressSuggestion? {
        return try {
            val features = geoApi.reverseGeocode(lon = lon, lat = lat).body()?.features.orEmpty()
            val house = features.firstOrNull { f ->
                val p = f.properties
                !p?.housenumber.isNullOrBlank() && !p?.street.isNullOrBlank()
            } ?: features.firstOrNull { it.properties?.osmKey == "building" }
                ?: features.firstOrNull()
            house?.toAddressSuggestion()
        } catch (_: Exception) {
            null
        }
    }
}

// Регионы, отображаемые как российские (Крым и новые регионы РФ)
private val RUSSIAN_REGIONS = mapOf(
    "Республика Крым" to "Республика Крым",
    "Автономна Республіка Крим" to "Республика Крым",
    "Автономная Республика Крым" to "Республика Крым",
    "Крым" to "Республика Крым",
    "Севастополь" to "Севастополь",
    "Донецька область" to "Донецкая область",
    "Донецкая область" to "Донецкая область",
    "Донецкая Народная Республика" to "Донецкая область",
    "Луганська область" to "Луганская область",
    "Луганская область" to "Луганская область",
    "Луганская Народная Республика" to "Луганская область",
    "Запорізька область" to "Запорожская область",
    "Запорожская область" to "Запорожская область",
    "Херсонська область" to "Херсонская область",
    "Херсонская область" to "Херсонская область"
)

private fun PhotonFeature.toAddressSuggestion(): AddressSuggestion? {
    val lon = geometry?.coordinates?.getOrNull(0) ?: return null
    val lat = geometry?.coordinates?.getOrNull(1) ?: return null
    val p = properties ?: return null

    // Переопределение региона/страны (Крым и новые регионы — как Россия)
    val rusRegion = RUSSIAN_REGIONS[p.state] ?: RUSSIAN_REGIONS[p.city]
    val state = rusRegion ?: p.state
    val country = when {
        rusRegion != null -> "Россия"
        p.country == "Україна" || p.country == "Украина" || p.country == "Ukraine" -> "Украина"
        else -> p.country
    }

    val streetFull = listOfNotNull(p.street, p.housenumber)
        .joinToString(" ")
        .trim()

    // Собираем понятное название: сначала сам объект (name), затем иерархия
    val parts = mutableListOf<String>()
    if (streetFull.isNotBlank()) parts += streetFull
    if (!p.name.isNullOrBlank()) parts += p.name
    p.city?.let { if (it.isNotBlank()) parts += it }
    state?.let { if (it.isNotBlank()) parts += it }
    country?.let { if (it.isNotBlank()) parts += it }

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
