package com.rentmanager.app.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.VerifyPasswordRequest
import com.rentmanager.app.data.local.CryptoManager
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PinUiState(
    val pin: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val attemptsLeft: Int = 5
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    fun onDigitEntered(digit: String) {
        val current = _uiState.value.pin
        if (current.length < 4) {
            _uiState.update { it.copy(pin = current + digit, errorMessage = null) }
            if (current.length == 3) {
                verifyPin(current + digit)
            }
        }
    }

    fun onDeleteDigit() {
        val current = _uiState.value.pin
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(pin = current.dropLast(1), errorMessage = null) }
        }
    }

    private fun verifyPin(pin: String) {
        val phone = tokenManager.phone ?: run {
            tokenManager.clear()
            _uiState.update { it.copy(pin = "", isLoading = false, isVerified = true) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val resp = authApi.verifyPassword(VerifyPasswordRequest(phone, pin))
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    tokenManager.accessToken = body.accessToken
                    tokenManager.refreshToken = body.refreshToken
                    // Сохраняем PIN в зашифрованное хранилище для входа по отпечатку
                    cryptoManager.savePin(pin)
                    _uiState.update { it.copy(isLoading = false, isVerified = true) }
                } else {
                    val remaining = _uiState.value.attemptsLeft - 1
                    if (remaining <= 0) {
                        // Логаут после 5 неверных попыток
                        tokenManager.clear()
                        _uiState.update { it.copy(pin = "", isLoading = false, errorMessage = "Превышен лимит попыток", attemptsLeft = remaining) }
                    } else {
                        _uiState.update { it.copy(pin = "", isLoading = false, errorMessage = "Неверный код. Осталось попыток: $remaining", attemptsLeft = remaining) }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(pin = "", isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        tokenManager.clear()
        cryptoManager.clearPin()
        onLoggedOut()
    }

    fun onBiometricSuccess(pin: String) {
        // Биометрия подтверждена — имитируем ввод PIN через клавиатуру
        _uiState.update { it.copy(pin = pin, isLoading = true, errorMessage = null) }
        verifyPin(pin)
    }
}
