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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.coroutineContext
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
    val attemptsLeft: Int? = null,
    val lockSeconds: Long = 0L,
    val isErrorAnimating: Boolean = false,
    val errorId: Int = 0
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
            step = if (!tokenManager.pinRecovery && tokenManager.hasPassword && tokenManager.localPinEnabled) {
                PinSetupStep.VERIFY_CURRENT
            } else PinSetupStep.ENTER,
            localPinEnabled = tokenManager.localPinEnabled,
            useBiometric = tokenManager.useBiometric,
            isLoading = !tokenManager.pinRecovery,
            lockSeconds = remainingLockSeconds(tokenManager.pinLockedUntil, System.currentTimeMillis())
        )
    )
    val uiState: StateFlow<PinSetupUiState> = _uiState.asStateFlow()
    private var feedbackJob: Job? = null

    init {
        if (!tokenManager.pinRecovery) checkPasswordStatus()
        fetchAttempts()
        loadTrustedDevices()
        // Мгновенное обновление списка устройств: событие приходит локально
        // (после «Подтвердить» в диалоге входа) или по push type=devices_changed.
        viewModelScope.launch {
            loginApprovalEvents.devicesChanged.collect { loadTrustedDevices() }
        }
        viewModelScope.launch {
            while (true) {
                val seconds = remainingLockSeconds(tokenManager.pinLockedUntil, System.currentTimeMillis())
                _uiState.update { it.copy(lockSeconds = seconds) }
                delay(250)
            }
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
                    tokenManager.hasPassword = hasPassword
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPasswordSet = hasPassword,
                            localPinEnabled = tokenManager.localPinEnabled,
                            step = if (hasPassword && tokenManager.localPinEnabled) PinSetupStep.VERIFY_CURRENT else PinSetupStep.ENTER
                        )
                    }
                }
            } catch (_: Exception) { }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // Ввод текущего кода (для смены)
    fun onCurrentPinDigit(digit: String) {
        val state = _uiState.value
        if (!canEnter() || state.lockSeconds > 0 || digit.length != 1 || digit[0] !in '0'..'9') return
        feedbackJob?.cancel()
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
        if (!canEnter() || state.lockSeconds > 0) return
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
                tokenManager.pinLockedUntil = 0L
                // Регистрируем FCM-токен
                tokenManager.fcmToken?.let { fcm ->
                    launch { try { authApi.registerDevice(RegisterDeviceRequest(fcm, deviceIdManager.deviceId)) } catch (_: Exception) {} }
                }
                _uiState.update { it.copy(isLoading = false, step = PinSetupStep.ENTER, pin = "", currentPin = "", errorMessage = null) }
                } else {
                    val parsed = parsePinError(resp.errorBody()?.string())
                    if (resp.code() == 429) {
                        val seconds = parsed?.retryAfterSeconds ?: 1L
                        tokenManager.pinLockedUntil = System.currentTimeMillis() + seconds * 1000L
                        _uiState.update { it.copy(lockSeconds = seconds, attemptsLeft = 0) }
                        showError(null, animate = true)
                    } else if (resp.code() == 401) {
                        _uiState.update { it.copy(attemptsLeft = parsed?.attemptsLeft) }
                        showError(pinErrorText(parsed?.attemptsLeft), animate = true)
                    } else {
                        showError("Не удалось проверить код", animate = false)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                showError("Нет связи с сервером", animate = false)
            }
        }
    }

    // Ввод нового кода (шаги ENTER и CONFIRM)
    fun onDigitEntered(digit: String) {
        val state = _uiState.value
        if (!canEnter() || digit.length != 1 || digit[0] !in '0'..'9') return
        feedbackJob?.cancel()
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
                            _uiState.update { it.copy(step = PinSetupStep.ENTER, pin = "", confirmPin = "") }
                            showError("Коды не совпадают. Попробуйте ещё раз", animate = true)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    fun onDeleteDigit() {
        val state = _uiState.value
        if (!canEnter()) return
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
                coroutineContext.ensureActive()
                if (resp.isSuccessful) {
                    tokenManager.hasPassword = true
                    tokenManager.localPinEnabled = true
                    // Сохраняем PIN локально сразу — чтобы биометрия работала
                    // без повторного входа по коду.
                    cryptoManager.savePin(password)
                    tokenManager.pinLockedUntil = 0L
                    tokenManager.finishPinRecovery()
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, isPasswordSet = true, localPinEnabled = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, step = PinSetupStep.ENTER, pin = "", confirmPin = "", errorMessage = "Ошибка сохранения") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, step = PinSetupStep.ENTER, pin = "", confirmPin = "", errorMessage = "Нет связи с сервером") }
            }
        }
    }

    private fun canEnter() = !_uiState.value.isLoading && !_uiState.value.isErrorAnimating && !_uiState.value.isSuccess &&
        (!tokenManager.pinRecovery || (tokenManager.recoveryPasswordReset && tokenManager.accessToken != null))

    private fun showError(message: String?, animate: Boolean) {
        feedbackJob?.cancel()
        _uiState.update { it.copy(isLoading = false, errorMessage = message,
            isErrorAnimating = animate, errorId = it.errorId + 1) }
        feedbackJob = viewModelScope.launch {
            if (animate) delay(PIN_SHAKE_MS)
            _uiState.update { it.copy(currentPin = "", confirmPin = "", isErrorAnimating = false) }
            delay(PIN_ERROR_MS - if (animate) PIN_SHAKE_MS else 0L)
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun beginRecovery() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val otherDevice = withTimeoutOrNull(3000) {
                try { authApi.listDevices(deviceIdManager.deviceId).body()?.any { !it.currentDevice } == true }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { false }
            } ?: false
            tokenManager.beginPinRecovery(otherDevice)
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