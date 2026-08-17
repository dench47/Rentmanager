package com.rentmanager.app.ui.landlord.myproperties.propertydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AttachTenantRequest
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.PropertyApi
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.api.UserSearchResult
import com.rentmanager.app.data.model.TenantDto
import com.rentmanager.app.util.PhoneUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AttachTenantUiState(
    val searchResults: List<UserSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isAttaching: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AttachTenantViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val propertyApi: PropertyApi,
    private val tenantApi: TenantApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttachTenantUiState())
    val uiState: StateFlow<AttachTenantUiState> = _uiState.asStateFlow()

    fun searchUsers(phone: String) {
        val error = PhoneUtils.validate(phone)
        if (error != null) {
            _uiState.update { it.copy(isSearching = false, errorMessage = error, searchResults = emptyList()) }
            return
        }
        val normalized = PhoneUtils.normalize(phone) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            try {
                val resp = authApi.searchUsers(normalized)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(isSearching = false, searchResults = resp.body() ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isSearching = false, errorMessage = "Ошибка поиска") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearching = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun attachUser(propertyId: String, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAttaching = true, errorMessage = null) }
            try {
                val resp = propertyApi.attachTenant(propertyId, AttachTenantRequest(userId = userId))
                if (resp.isSuccessful) onSuccess()
                else _uiState.update { it.copy(isAttaching = false, errorMessage = "Ошибка прикрепления") }
            } catch (_: Exception) {
                _uiState.update { it.copy(isAttaching = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun attachManual(propertyId: String, name: String, phone: String, onSuccess: () -> Unit) {
        val error = PhoneUtils.validate(phone)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        val normalizedPhone = PhoneUtils.normalize(phone) ?: phone
        viewModelScope.launch {
            _uiState.update { it.copy(isAttaching = true, errorMessage = null) }
            try {
                val tenantResp = tenantApi.createTenant(TenantDto(id = "", fullName = name, phone = normalizedPhone))
                if (!tenantResp.isSuccessful) {
                    _uiState.update { it.copy(isAttaching = false, errorMessage = "Ошибка создания арендатора") }
                    return@launch
                }
                val tenantId = tenantResp.body()!!.id
                val attachResp = propertyApi.attachTenant(propertyId, AttachTenantRequest(tenantId = tenantId))
                if (attachResp.isSuccessful) onSuccess()
                else _uiState.update { it.copy(isAttaching = false, errorMessage = "Ошибка прикрепления") }
            } catch (_: Exception) {
                _uiState.update { it.copy(isAttaching = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }
}