package com.rentmanager.app.ui.landlord.otherproperties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.api.UserSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LandlordsListUiState(
    val landlords: List<UserSearchResult> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class LandlordsListViewModel @Inject constructor(
    private val tenantApi: TenantApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandlordsListUiState())
    val uiState: StateFlow<LandlordsListUiState> = _uiState.asStateFlow()

    /** Загрузка уже проходила — дальше только тихий рефреш, без «Загрузки…» и мигания */
    private var loadedOnce = false

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !loadedOnce, errorMessage = null) }
            try {
                val resp = tenantApi.getLandlords()
                loadedOnce = true
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, landlords = resp.body() ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка загрузки") }
                }
            } catch (_: Exception) {
                loadedOnce = true
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }
}
