package com.rentmanager.app.ui.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AuthUiState(
    val phoneNumber: String = "",
    val smsCode: String = "",
    val userName: String = "",
    val isLoading: Boolean = false,
    val isCodeSent: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onPhoneNumberChanged(phone: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phone, error = null)
    }

    fun onSmsCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(smsCode = code, error = null)
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(userName = name, error = null)
    }

    fun sendSmsCode() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        // TODO: API-запрос на отправку SMS/PUSH
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isCodeSent = true,
            error = null
        )
    }

    fun confirmSmsCode() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        // TODO: API-запрос на подтверждение кода
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null
        )
    }

    fun saveUserName() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        // TODO: API-запрос на сохранение имени
        _uiState.value = _uiState.value.copy(isLoading = false)
    }
}