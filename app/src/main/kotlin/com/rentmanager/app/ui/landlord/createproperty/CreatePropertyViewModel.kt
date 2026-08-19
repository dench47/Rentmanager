package com.rentmanager.app.ui.landlord.createproperty

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.model.PhotoDto
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.PhotoUploader
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePropertyUiState(
    val isCreating: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CreatePropertyViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val photoUploader: PhotoUploader,
    private val detailCache: PropertyDetailCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePropertyUiState())
    val uiState: StateFlow<CreatePropertyUiState> = _uiState.asStateFlow()

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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = CreatePropertyUiState(isCreating = true)
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
                    houseRules = houseRules
                )
                val resp = propertyRepository.createProperty(dto)
                if (resp.isSuccessful) {
                    resp.body()?.let { detailCache.saveProperty(it) }
                    onSuccess()
                } else {
                    _uiState.value = CreatePropertyUiState(isCreating = false, errorMessage = "Ошибка создания объекта")
                }
            } catch (e: Exception) {
                _uiState.value = CreatePropertyUiState(isCreating = false, errorMessage = e.message ?: "Ошибка")
            }
        }
    }

    fun clearError() {
        _uiState.value = CreatePropertyUiState()
    }
}
