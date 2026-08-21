package com.rentmanager.app.ui.landlord.createproperty

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.local.LocationProvider
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.model.PhotoDto
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.AddressSuggestion
import com.rentmanager.app.data.repository.GeoRepository
import com.rentmanager.app.data.repository.PhotoUploader
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePropertyUiState(
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    val addressSuggestions: List<AddressSuggestion> = emptyList(),
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val addressToSet: String? = null
)

@HiltViewModel
class CreatePropertyViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val photoUploader: PhotoUploader,
    private val geoRepository: GeoRepository,
    private val detailCache: PropertyDetailCache,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePropertyUiState())
    val uiState: StateFlow<CreatePropertyUiState> = _uiState.asStateFlow()

    private var suggestJob: Job? = null
    private var baseAddress: String = ""
    private var scopeBbox: String? = null
    private var refinePrefix: String? = null

    fun suggestAddress(fullText: String) {
        suggestJob?.cancel()

        // Если пользователь стёр всё — сбрасываем накопленную базу и область поиска
        if (fullText.trim().isEmpty()) {
            baseAddress = ""
            scopeBbox = null
            refinePrefix = null
            _uiState.value = _uiState.value.copy(addressSuggestions = emptyList())
            return
        }

        // «Активный сегмент» — то, что пользователь печатает после уже выбранной базы
        val segment = when {
            baseAddress.isNotEmpty() && fullText.endsWith(baseAddress) ->
                fullText.removeSuffix(baseAddress).trim(' ', ',', '-', '.', ';')
            baseAddress.isNotEmpty() && fullText.startsWith(baseAddress) ->
                fullText.removePrefix(baseAddress).trim(' ', ',', '-', '.', ';')
            else -> fullText.trim()
        }

        // Порог: 1 символ при уточнении номера дома, 3 — для общего поиска
        val minLength = if (refinePrefix != null) 1 else 3
        if (segment.length < minLength) {
            _uiState.value = _uiState.value.copy(addressSuggestions = emptyList())
            return
        }

        // Запрос: «улица номер» при уточнении дома, иначе сам сегмент
        val query = if (refinePrefix != null) "$refinePrefix $segment" else segment

        suggestJob = viewModelScope.launch {
            delay(300)
            var lat: Double? = null
            var lon: Double? = null
            if (scopeBbox == null && refinePrefix == null) {
                locationProvider.lastKnownLocation()?.let { pair ->
                    lat = pair.first
                    lon = pair.second
                }
            }
            val suggestions = geoRepository.suggest(query, scopeBbox, lat, lon)
            _uiState.value = _uiState.value.copy(addressSuggestions = suggestions)
        }
    }

    fun selectAddress(suggestion: AddressSuggestion) {
        baseAddress = suggestion.displayName
        val type = suggestion.type ?: ""

        if (type == "street") {
            // Улица: оставляем городской bbox, запоминаем имя для уточнения номера дома
            refinePrefix = suggestion.displayName.substringBefore(",").trim()
        } else {
            refinePrefix = null
            if (type in setOf("city", "town", "village", "hamlet", "state", "country", "district", "county", "municipality")) {
                scopeBbox = suggestion.extent?.let { ext ->
                    if (ext.size >= 4) ext.take(4).joinToString(",") else null
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            addressSuggestions = emptyList(),
            selectedLatitude = suggestion.latitude,
            selectedLongitude = suggestion.longitude
        )
    }

    fun clearAddressSelection() {
        baseAddress = ""
        scopeBbox = null
        refinePrefix = null
        _uiState.value = _uiState.value.copy(
            addressSuggestions = emptyList(),
            selectedLatitude = null,
            selectedLongitude = null
        )
    }

    fun clearAddressSuggestions() {
        _uiState.value = _uiState.value.copy(addressSuggestions = emptyList())
    }

    fun onMapTapped(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            selectedLatitude = lat,
            selectedLongitude = lon
        )
        viewModelScope.launch {
            val suggestion = geoRepository.reverse(lat, lon)
            if (suggestion != null) {
                baseAddress = suggestion.displayName
                scopeBbox = null
                refinePrefix = null
                _uiState.value = _uiState.value.copy(
                    addressToSet = suggestion.displayName,
                    addressSuggestions = emptyList()
                )
            }
        }
    }

    fun consumeAddressToSet() {
        _uiState.value = _uiState.value.copy(addressToSet = null)
    }

    fun createProperty(
        name: String,
        address: String,
        area: String?,
        rentAmount: String?,
        description: String?,
        photoUris: List<String>,
        serviceInfo: String?,
        phone: String?,
        wifiPassword: String?,
        houseRules: String?,
        latitude: Double?,
        longitude: Double?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            try {
                // 1. Загружаем фото в S3 (папка photos), по аналогии с аватаркой
                val photoUrls = photoUris.map { photoUploader.upload(Uri.parse(it)) }

                // 2. Создаём объект в БД
                val dto = PropertyDto(
                    name = name,
                    address = address.trim().trimEnd(',', ' '),
                    area = area?.toDoubleOrNull(),
                    rentAmount = rentAmount?.toDoubleOrNull(),
                    description = description,
                    photos = photoUrls.map { PhotoDto(url = it) },
                    serviceInfo = serviceInfo,
                    phone = phone,
                    wifiPassword = wifiPassword,
                    houseRules = houseRules,
                    latitude = latitude,
                    longitude = longitude
                )
                val resp = propertyRepository.createProperty(dto)
                if (resp.isSuccessful) {
                    resp.body()?.let { detailCache.saveProperty(it) }
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(isCreating = false, errorMessage = "Ошибка создания объекта")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isCreating = false, errorMessage = e.message ?: "Ошибка")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
