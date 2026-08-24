package com.rentmanager.app.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.RegisterDeviceRequest
import com.rentmanager.app.data.api.SetPasswordRequest
import com.rentmanager.app.data.api.TrustedDeviceDto
import com.rentmanager.app.data.api.VerifyPasswordRequest
import com.rentmanager.app.data.local.CryptoManager
import com.rentmanager.app.data.local.DeviceIdManager
import com.rentmanager.app.data.local.LoginApprovalEvents
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
    // Локальный тумблер «Требовать PIN на этом устройстве».
    // Выключение НЕ удаляет серверный PIN — он нужен для входа с новых устройств.
    val localPinEnabled: Boolean = true,
    // Вход по отпечатку (локальная настройка устройства, работает при установленном PIN)
    val useBiometric: Boolean = false,
    // Доверенные устройства (Device Trust)
    val trustedDevices: List<TrustedDeviceDto> = emptyList(),
    val attemptsLeft: Int? = null  // null = ещё грузим с сервера
)

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val cryptoManager: CryptoManager,
    private val deviceIdManager: DeviceIdManager,
    private val loginApprovalEvents: LoginApprovalEvents
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PinSetupUiState(
            localPinEnabled = tokenManager.localPinEnabled,
            useBiometric = tokenManager.useBiometric
        )
    )
    val uiState: StateFlow<PinSetupUiState> = _uiState.asStateFlow()

    init {
        checkPasswordStatus()
        fetchAttempts()
        loadTrustedDevices()
        // Мгновенное обновление списка устройств: событие приходит локально
        // (после «Подтвердить» в диалоге входа) или по push type=devices_changed.
        viewModelScope.launch {
            loginApprovalEvents.devicesChanged.collect { loadTrustedDevices() }
        }
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
                    _uiState.update {
                        it.copy(
                            isPasswordSet = hasPassword,
                            localPinEnabled = tokenManager.localPinEnabled,
                            step = if (hasPassword && tokenManager.localPinEnabled) PinSetupStep.VERIFY_CURRENT else PinSetupStep.ENTER
                        )
                    }
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
                _uiState.update { it.copy(isLoading = false, step = PinSetupStep.ENTER, pin = "", currentPin = "", errorMessage = null) }
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
                    tokenManager.localPinEnabled = true
                    // Сохраняем PIN локально сразу — чтобы биометрия работала
                    // без повторного входа по коду.
                    cryptoManager.savePin(password)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, isPasswordSet = true, localPinEnabled = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, step = PinSetupStep.ENTER, pin = "", confirmPin = "", errorMessage = "Ошибка сохранения") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, step = PinSetupStep.ENTER, pin = "", confirmPin = "", errorMessage = "Нет связи с сервером") }
            }
        }
    }

    // Тумблер «Требовать PIN на этом устройстве».
    // Выключение: локальный флаг + очистка сохранённого PIN для биометрии.
    // Серверный PIN НЕ удаляется — он нужен для подтверждения входа с новых устройств.
    // Включение: если серверный PIN есть — просто включаем флаг; если нет — задаём новый код.
    fun onTogglePinWithoutPin(disable: Boolean) {
        if (disable) {
            tokenManager.localPinEnabled = false
            cryptoManager.clearPin()
            _uiState.update { it.copy(isLoading = false, localPinEnabled = false, useBiometric = false) }
        } else {
            if (tokenManager.hasPassword) {
                tokenManager.localPinEnabled = true
                _uiState.update { it.copy(localPinEnabled = true) }
            } else {
                // Серверного PIN нет — предлагаем придумать
                _uiState.update {
                    it.copy(step = PinSetupStep.ENTER, pin = "", confirmPin = "", errorMessage = null)
                }
            }
        }
    }

    /** Тумблер «Вход по отпечатку» — локальная настройка устройства. */
    fun toggleBiometric(enabled: Boolean) {
        tokenManager.useBiometric = enabled
        _uiState.update { it.copy(useBiometric = enabled) }
    }

    // ===== Доверенные устройства (Device Trust) =====

    fun loadTrustedDevices() {
        viewModelScope.launch {
            try {
                val resp = authApi.listDevices(deviceIdManager.deviceId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(trustedDevices = resp.body() ?: emptyList()) }
                }
            } catch (_: Exception) { }
        }
    }

    /** Отзыв доверенного устройства — при следующем входе оно потребует подтверждения. */
    fun revokeTrustedDevice(deviceRowId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = try {
                authApi.revokeDevice(deviceRowId).isSuccessful
            } catch (_: Exception) { false }
            if (ok) loadTrustedDevices()
            onDone(ok)
        }
    }
}