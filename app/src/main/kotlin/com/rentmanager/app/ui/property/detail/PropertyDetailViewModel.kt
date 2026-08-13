package com.rentmanager.app.ui.property.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PropertyDetailUiState(
    val isLoading: Boolean = true,
    val propertyName: String = "",
    val address: String = "",
    val area: String = "",
    val photoUrl: String? = null,
    val serviceInfo: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class PropertyDetailViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyDetailUiState())
    val uiState: StateFlow<PropertyDetailUiState> = _uiState.asStateFlow()

    fun load(propertyId: String) {
        viewModelScope.launch {
            _uiState.value = PropertyDetailUiState(isLoading = true)
            try {
                val resp = propertyRepository.getProperty(propertyId)
                if (resp.isSuccessful) {
                    val p = resp.body()!!
                    _uiState.value = PropertyDetailUiState(
                        isLoading = false,
                        propertyName = p.name,
                        address = p.address,
                        area = formatArea(p.area),
                        photoUrl = p.photos.firstOrNull()?.url,
                        serviceInfo = p.serviceInfo ?: ""
                    )
                } else {
                    _uiState.value = PropertyDetailUiState(isLoading = false, errorMessage = "Объект не найден")
                }
            } catch (e: Exception) {
                _uiState.value = PropertyDetailUiState(isLoading = false, errorMessage = e.message ?: "Ошибка")
            }
        }
    }

    private fun formatArea(area: Double?): String {
        if (area == null) return ""
        return if (area == area.toLong().toDouble()) "${area.toLong()} м²" else "$area м²"
    }
}
