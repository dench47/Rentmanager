package com.rentmanager.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.SendCodeRequest
import com.rentmanager.app.data.api.UpdateProfileRequest
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val phone: String = "",
    val email: String? = null,
    val legalName: String? = null,
    val avatarUrl: String? = null,
    val defaultStartScreen: String = "main",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Смена телефона
    val isChangingPhone: Boolean = false,
    val newPhone: String = "",
    val callPhone: String = "",
    val callPhonePretty: String = "",
    val showPhoneWarning: Boolean = false,
    // Выход
    val showLogoutDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    // Начальный экран
    val showStartScreenDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(
        phone = tokenManager.phone ?: "",
        defaultStartScreen = tokenManager.defaultStartScreen
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val resp = authApi.getMe()
                if (resp.isSuccessful) {
                    val user = resp.body()!!
                    _uiState.update {
                        it.copy(
                            userName = user.name,
                            phone = user.phone,
                            email = user.email,
                            legalName = user.legalName,
                            avatarUrl = user.avatarUrl
                        )
                    }
                    tokenManager.userName = user.name
                    tokenManager.phone = user.phone
                }
            } catch (_: Exception) { }
        }
    }

    fun updateProfile(
        name: String? = null,
        email: String? = null,
        legalName: String? = null,
        avatarUrl: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resp = authApi.updateProfile(UpdateProfileRequest(name, email, legalName, avatarUrl))
                if (resp.isSuccessful) {
                    val user = resp.body()!!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userName = user.name,
                            email = user.email,
                            legalName = user.legalName,
                            avatarUrl = user.avatarUrl
                        )
                    }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка сохранения") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun onPhoneChangeRequest(newPhone: String) {
        val clean = newPhone.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(showPhoneWarning = true, newPhone = "+7$clean") }
    }

    fun dismissPhoneWarning() {
        _uiState.update { it.copy(showPhoneWarning = false) }
    }

    fun startPhoneVerification(onNavigate: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(showPhoneWarning = false, isLoading = true) }
            try {
                val resp = authApi.changePhone(SendCodeRequest(_uiState.value.newPhone))
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    _uiState.update {
                        it.copy(isLoading = false, isChangingPhone = true, callPhone = body.callPhone, callPhonePretty = body.callPhonePretty)
                    }
                    onNavigate()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun showLogoutDialog() { _uiState.update { it.copy(showLogoutDialog = true) } }

    fun logoutCurrentDevice(onLoggedOut: () -> Unit) {
        tokenManager.clear()
        _uiState.update { it.copy(showLogoutDialog = false) }
        onLoggedOut()
    }

    fun logoutAllDevices(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try { authApi.logoutAll() } catch (_: Exception) { }
            tokenManager.clear()
            _uiState.update { it.copy(showLogoutDialog = false) }
            onLoggedOut()
        }
    }

    fun showDeleteDialog() { _uiState.update { it.copy(showDeleteDialog = true) } }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try { authApi.deleteAccount() } catch (_: Exception) { }
            tokenManager.clear()
            _uiState.update { it.copy(showDeleteDialog = false) }
            onDeleted()
        }
    }

    fun showStartScreenDialog() { _uiState.update { it.copy(showStartScreenDialog = true) } }

    fun setDefaultStartScreen(screen: String) {
        tokenManager.defaultStartScreen = screen
        _uiState.update { it.copy(defaultStartScreen = screen, showStartScreenDialog = false) }
    }

    fun dismissDialogs() {
        _uiState.update { it.copy(showLogoutDialog = false, showDeleteDialog = false, showStartScreenDialog = false, showPhoneWarning = false, errorMessage = null) }
    }
}