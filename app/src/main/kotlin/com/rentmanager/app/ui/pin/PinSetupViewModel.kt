package com.rentmanager.app.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.RegisterDeviceRequest
import com.rentmanager.app.data.api.SetPasswordRequest
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

data class PinSetupUiState(
    val step: PinSetupStep = PinSetupStep.ENTER,
    val pin: String = "",
    val confirmPin: String = "",
    val currentPin: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isPasswordSet: Boolean = false,
    val isDisabling: Boolean = false,
    val attemptsLeft: Int? = null  // null = ещё грузим с сервера
)

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinSetupUiState())
    val uiState: StateFlow<PinSetupUiState> = _uiState.asStateFlow()

    init {
        checkPasswordStatus()
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

    private fun checkPasswordStatus() {
        viewModelScope.launch {
            try {
                val resp = authApi.getMe()
                if (resp.isSuccessful) {
                    val hasPassword = resp.body()?.hasPassword ?: false
                    _uiState.update { it.copy(isPasswordSet = hasPassword, step = if (hasPassword) PinSetupStep.VERIFY_CURRENT else PinSetupStep.ENTER) }
                }
            } catch (_: Exception) { }
        }
    }

    // Ввод текущего кода (для смены)
    fun onCurrentPinDigit(digit: String) {
        val state = _uiState.value
        val newPin = state.currentPin + digit
        if (newPin.length <= 4) {
            _uiState.update { it.copy(currentPin = newPin, errorMessage = null) }
            if (newPin.length == 4) {
                verifyCurrentPassword(newPin)
            }
        }
    }

    fun onCurrentPinDelete() {
        val state = _uiState.value
        if (state.currentPin.isNotEmpty()) {
            _uiState.update { it.copy(currentPin = state.currentPin.dropLast(1), errorMessage = null) }
        }
    }

    private fun verifyCurrentPassword(password: String) {
        val phone = tokenManager.phone ?: run {
            _uiState.update { it.copy(currentPin = "", errorMessage = "Ошибка: номер не найден") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val resp = authApi.verifyPassword(VerifyPasswordRequest(phone, password, tokenManager.fcmToken))
            if (resp.isSuccessful) {
                val body = resp.body()!!
                tokenManager.accessToken = body.accessToken
                tokenManager.refreshToken = body.refreshToken
                // Регистрируем FCM-токен
                tokenManager.fcmToken?.let { fcm ->
                    launch { try { authApi.registerDevice(RegisterDeviceRequest(fcm)) } catch (_: Exception) {} }
                }
                if (_uiState.value.isDisabling) {
                    // Подтвердили текущий код — отключаем PIN
                    clearPassword()
                } else {
                    _uiState.update { it.copy(isLoading = false, step = PinSetupStep.ENTER, pin = "", currentPin = "", errorMessage = null) }
                }
                } else {
                    val remaining = (_uiState.value.attemptsLeft ?: 5) - 1
                    if (remaining <= 0) {
                        tokenManager.clear()
                        _uiState.update { it.copy(currentPin = "", isLoading = false, errorMessage = "Превышен лимит попыток. Выход из аккаунта.", attemptsLeft = remaining) }
                    } else {
                        _uiState.update { it.copy(currentPin = "", isLoading = false, errorMessage = "Неверный код. Осталось попыток: $remaining", attemptsLeft = remaining) }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(currentPin = "", isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    // Ввод нового кода (шаги ENTER и CONFIRM)
    fun onDigitEntered(digit: String) {
        val state = _uiState.value
        when (state.step) {
            PinSetupStep.ENTER -> {
                val newPin = state.pin + digit
                if (newPin.length <= 4) {
                    _uiState.update { it.copy(pin = newPin, errorMessage = null) }
                    if (newPin.length == 4) {
                        _uiState.update { it.copy(step = PinSetupStep.CONFIRM, errorMessage = null) }
                    }
                }
            }
            PinSetupStep.CONFIRM -> {
                val newConfirm = state.confirmPin + digit
                if (newConfirm.length <= 4) {
                    _uiState.update { it.copy(confirmPin = newConfirm, errorMessage = null) }
                    if (newConfirm.length == 4) {
                        if (newConfirm == state.pin) {
                            savePassword(newConfirm)
                        } else {
                            _uiState.update { it.copy(step = PinSetupStep.ENTER, pin = "", confirmPin = "", errorMessage = "Коды не совпадают. Попробуйте снова.") }
                        }
                    }
                }
            }
            else -> {}
        }
    }

    fun onDeleteDigit() {
        val state = _uiState.value
        when (state.step) {
            PinSetupStep.ENTER -> {
                if (state.pin.isNotEmpty()) {
                    _uiState.update { it.copy(pin = state.pin.dropLast(1), errorMessage = null) }
                }
            }
            PinSetupStep.CONFIRM -> {
                if (state.confirmPin.isNotEmpty()) {
                    _uiState.update { it.copy(confirmPin = state.confirmPin.dropLast(1), errorMessage = null) }
                } else {
                    _uiState.update { it.copy(step = PinSetupStep.ENTER, pin = "", errorMessage = null) }
                }
            }
            else -> {}
        }
    }

    private fun savePassword(password: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val resp = authApi.setPassword(SetPasswordRequest(password))
                if (resp.isSuccessful) {
                    tokenManager.hasPassword = true
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, step = PinSetupStep.ENTER, pin = "", confirmPin = "", errorMessage = "Ошибка сохранения") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, step = PinSetupStep.ENTER, pin = "", confirmPin = "", errorMessage = "Нет связи с сервером") }
            }
        }
    }

    // Тумблер «Вход без PIN»
    fun onTogglePinWithoutPin(disable: Boolean) {
        if (disable) {
            // Отключаем PIN: сначала подтверждаем текущий код
            if (_uiState.value.isPasswordSet) {
                _uiState.update {
                    it.copy(step = PinSetupStep.VERIFY_CURRENT, isDisabling = true, currentPin = "", errorMessage = null)
                }
            }
        } else {
            // Включаем PIN: задаём новый код
            _uiState.update {
                it.copy(step = PinSetupStep.ENTER, isDisabling = false, pin = "", confirmPin = "", errorMessage = null)
            }
        }
    }

    private fun clearPassword() {
        viewModelScope.launch {
            try {
                val resp = authApi.setPassword(SetPasswordRequest(""))
                if (resp.isSuccessful) {
                    tokenManager.hasPassword = false
                    cryptoManager.clearPin()
                    _uiState.update { it.copy(isLoading = false, isPasswordSet = false, isDisabling = false, isSuccess = true) }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, isDisabling = false, step = PinSetupStep.VERIFY_CURRENT, currentPin = "", errorMessage = "Ошибка отключения PIN")
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isDisabling = false, step = PinSetupStep.VERIFY_CURRENT, currentPin = "", errorMessage = "Нет связи с сервером")
                }
            }
        }
    }
}