package com.rentmanager.app.ui.auth.phone

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PhoneNumberUiState(
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PhoneNumberViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneNumberUiState())
    val uiState: StateFlow<PhoneNumberUiState> = _uiState.asStateFlow()

    fun onPhoneNumberChange(number: String) {
        _uiState.update { it.copy(phoneNumber = number, errorMessage = null) }
    }

    fun onContinue() {
        val phone = _uiState.value.phoneNumber.trim()
        if (phone.length < 10) {
            _uiState.update { it.copy(errorMessage = "Введите корректный номер") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        // Navigation handled by callback
    }

    fun resetLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }
}