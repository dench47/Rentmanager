package com.rentmanager.app.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import android.util.Log
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.RegisterDeviceRequest
import com.rentmanager.app.data.api.VerifyPasswordRequest
import com.rentmanager.app.data.local.CryptoManager
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class PinUiState(
    val pin: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val attemptsLeft: Int? = null  // null = ещё загружаем с сервера
)

data class VerifyPasswordErrorResponse(
    val error: String? = null,
    @com.google.gson.annotations.SerializedName("attempts_left")
    val attemptsLeft: Int? = null
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    init {
        fetchAttempts()
    }

    private fun fetchAttempts() {
        val phone = tokenManager.phone ?: return
        viewModelScope.launch {
            try {
                val resp = authApi.getPinAttempts(phone)
                if (resp.isSuccessful) {
                    val left = resp.body()?.attemptsLeft ?: 5
                    _uiState.update { it.copy(attemptsLeft = left) }
                } else {
                    _uiState.update { it.copy(attemptsLeft = 5) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(attemptsLeft = 5) }
            }
        }
    }

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
                val resp = authApi.verifyPassword(VerifyPasswordRequest(phone, pin, tokenManager.fcmToken))
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    tokenManager.accessToken = body.accessToken
                    tokenManager.refreshToken = body.refreshToken
                    // Сохраняем PIN в зашифрованное хранилище для входа по отпечатку
                    cryptoManager.savePin(pin)
                    // Регистрируем FCM-токен, если сохранён ранее
                    tokenManager.fcmToken?.let { fcm ->
                        launch { try { authApi.registerDevice(RegisterDeviceRequest(fcm)) } catch (_: Exception) {} }
                    }
                    _uiState.update { it.copy(isLoading = false, isVerified = true, attemptsLeft = 5) }
                } else {
                    // Парсим attemptsLeft из тела ошибки
                    var serverAttemptsLeft: Int? = null
                    try {
                        val errorBody = resp.errorBody()?.string()
                        val errorResp = Gson().fromJson(errorBody, VerifyPasswordErrorResponse::class.java)
                        serverAttemptsLeft = errorResp.attemptsLeft
                    } catch (_: Exception) {}

                    val remaining = serverAttemptsLeft ?: ((_uiState.value.attemptsLeft ?: 5) - 1)
                    if (remaining <= 0) {
                        // Полный разлогин на всех устройствах + сброс PIN
                        try { authApi.logoutAll() } catch (_: Exception) {}
                        tokenManager.clear()
                        cryptoManager.clearPin()
                        tokenManager.hasPassword = false
                        _uiState.update { it.copy(pin = "", isLoading = false, isVerified = true, attemptsLeft = 0, errorMessage = null) }
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
        val fcm = tokenManager.fcmToken
        viewModelScope.launch {
            if (fcm != null) {
                // Отвязываем FCM-токен на сервере, пока access-токен ещё валиден
                withTimeoutOrNull(3000) {
                    try { authApi.unregisterDevice(RegisterDeviceRequest(fcm)) } catch (_: Exception) {}
                }
            }
            tokenManager.clear()
            cryptoManager.clearPin()
            onLoggedOut()
        }
    }

    fun onBiometricSuccess(pin: String) {
        // Биометрия подтверждена — имитируем ввод PIN через клавиатуру
        _uiState.update { it.copy(pin = pin, isLoading = true, errorMessage = null) }
        verifyPin(pin)
    }
}
