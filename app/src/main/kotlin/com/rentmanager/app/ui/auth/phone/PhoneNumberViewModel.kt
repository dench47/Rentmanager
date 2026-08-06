package com.rentmanager.app.ui.auth.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.SendCodeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhoneNumberUiState(
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PhoneNumberViewModel @Inject constructor(
    private val authApi: AuthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneNumberUiState())
    val uiState: StateFlow<PhoneNumberUiState> = _uiState.asStateFlow()

    fun onPhoneNumberChange(number: String) {
        _uiState.update { it.copy(phoneNumber = number, errorMessage = null) }
    }

    fun onContinue(onCodeSent: () -> Unit) {
        val phone = _uiState.value.phoneNumber.trim()
        if (phone.length < 10) {
            _uiState.update { it.copy(errorMessage = "Введите корректный номер") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val response = authApi.sendCode(SendCodeRequest(phone))
                if (response.isSuccessful) {
                    onCodeSent()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка отправки кода") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun resetLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }
}
