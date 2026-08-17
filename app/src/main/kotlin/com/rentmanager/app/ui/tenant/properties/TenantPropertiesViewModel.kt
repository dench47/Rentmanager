package com.rentmanager.app.ui.tenant.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.PropertyApi
import com.rentmanager.app.data.model.PropertyDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TenantPropertiesUiState(
    val properties: List<PropertyDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class TenantPropertiesViewModel @Inject constructor(
    private val propertyApi: PropertyApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantPropertiesUiState())
    val uiState: StateFlow<TenantPropertiesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = propertyApi.getTenantProperties()
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, properties = resp.body() ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка загрузки") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }
}
