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

data class TenantsListUiState(
    val tenants: List<TenantDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class TenantsListViewModel @Inject constructor(
    private val tenantApi: TenantApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantsListUiState())
    val uiState: StateFlow<TenantsListUiState> = _uiState.asStateFlow()

    /** Загрузка уже проходила — дальше только тихий рефреш, без «Загрузки…» и мигания */
    private var loadedOnce = false

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val firstLoad = !loadedOnce
            _uiState.update { it.copy(isLoading = firstLoad, errorMessage = null) }
            try {
                val resp = tenantApi.getTenants()
                loadedOnce = true
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, tenants = resp.body() ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка загрузки") }
                }
            } catch (_: Exception) {
                loadedOnce = true
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun deleteTenant(id: String) {
        viewModelScope.launch {
            try {
                val resp = tenantApi.deleteTenant(id)
                if (resp.isSuccessful) {
                    load()
                } else {
                    _uiState.update { it.copy(errorMessage = "Ошибка удаления") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Нет связи с сервером") }
            }
        }
    }
}
