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
    private val tenantApi: TenantApi,
    private val listCache: com.rentmanager.app.data.local.TenantsListCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantsListUiState())
    val uiState: StateFlow<TenantsListUiState> = _uiState.asStateFlow()

    /** Загрузка уже проходила — дальше только тихий рефреш, без «Загрузки…» и мигания */
    private var loadedOnce = false

    init {
        // Канон «отображение сразу»: после перезапуска список рендерится из
        // кэша в первый кадр, сеть подтверждает молча (loadedOnce уже true)
        val cached = listCache.get()
        if (cached != null) {
            loadedOnce = true
            _uiState.value = TenantsListUiState(isLoading = false, tenants = cached)
        }
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
                    val tenants = resp.body() ?: emptyList()
                    listCache.put(tenants)
                    _uiState.update { it.copy(isLoading = false, tenants = tenants) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка загрузки") }
                }
            } catch (_: Exception) {
                loadedOnce = true
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    /** «Отменить удаление» из диалога на фоне списка (3014:22433) */
    fun restoreTenant(id: String) {
        viewModelScope.launch {
            runCatching { tenantApi.restoreTenant(id) }
            load()
        }
    }

    /** Закрытие окна «Нет связи» тапом вне (канон остальных экранов) */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
