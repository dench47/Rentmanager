package com.rentmanager.app.ui.landlord.createproperty

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val selectedLongitude: Double? = null
)

@HiltViewModel
class CreatePropertyViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val photoUploader: PhotoUploader,
    private val geoRepository: GeoRepository,
    private val detailCache: PropertyDetailCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePropertyUiState())
    val uiState: StateFlow<CreatePropertyUiState> = _uiState.asStateFlow()

    private var suggestJob: Job? = null

    fun suggestAddress(query: String) {
        suggestJob?.cancel()
        if (query.trim().length < 3) {
            _uiState.value = _uiState.value.copy(addressSuggestions = emptyList())
            return
        }
        suggestJob = viewModelScope.launch {
            delay(300)
            val suggestions = geoRepository.suggest(query.trim())
            _uiState.value = _uiState.value.copy(addressSuggestions = suggestions)
        }
    }

    fun selectAddress(suggestion: AddressSuggestion) {
        _uiState.value = _uiState.value.copy(
            addressSuggestions = emptyList(),
            selectedLatitude = suggestion.latitude,
            selectedLongitude = suggestion.longitude
        )
    }

    fun clearAddressSelection() {
        _uiState.value = _uiState.value.copy(
            selectedLatitude = null,
            selectedLongitude = null
        )
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
                    address = address,
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
