package com.rentmanager.app.ui.auth.code

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SmsCodeUiState(
    val code: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SmsCodeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SmsCodeUiState())
    val uiState: StateFlow<SmsCodeUiState> = _uiState.asStateFlow()

    fun setPhoneNumber(number: String) {
        _uiState.update { it.copy(phoneNumber = number) }
    }

    fun onCodeChange(newCode: String) {
        if (newCode.length <= 6) {
            _uiState.update { it.copy(code = newCode, errorMessage = null) }
        }
    }

    fun onCodeComplete() {
        if (_uiState.value.code.length < 6) {
            _uiState.update { it.copy(errorMessage = "Введите 6 цифр") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }
}