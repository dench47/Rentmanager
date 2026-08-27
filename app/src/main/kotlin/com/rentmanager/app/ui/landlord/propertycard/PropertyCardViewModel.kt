package com.rentmanager.app.ui.landlord.propertycard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PropertyCardUiState(
    val isLoading: Boolean = true,
    val property: PropertyDto? = null
)

@HiltViewModel
class PropertyCardViewModel @Inject constructor(
    private val repository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyCardUiState())
    val uiState: StateFlow<PropertyCardUiState> = _uiState.asStateFlow()

    fun load(propertyId: String) {
        if (propertyId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = PropertyCardUiState(isLoading = true)
            try {
                val resp = repository.getProperty(propertyId)
                val body = if (resp.isSuccessful) resp.body() else null
                _uiState.value = PropertyCardUiState(isLoading = false, property = body)
            } catch (e: Exception) {
                _uiState.value = PropertyCardUiState(isLoading = false)
            }
        }
    }
}