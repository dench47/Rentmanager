package com.rentmanager.app.ui.landlord.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.model.TenantDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TenantDetailUiState(
    val tenant: TenantDto? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class TenantDetailViewModel @Inject constructor(
    private val tenantApi: TenantApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantDetailUiState())
    val uiState: StateFlow<TenantDetailUiState> = _uiState.asStateFlow()

    fun load(tenantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = tenantApi.getTenant(tenantId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(tenant = resp.body(), isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка загрузки") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }
}