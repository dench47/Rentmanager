package com.rentmanager.app.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import javax.inject.Inject

data class PinUiState(
    val pin: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val attemptsLeft: Int? = null,
    val lockSeconds: Long = 0L,
    val isErrorAnimating: Boolean = false,
    val errorId: Int = 0
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val cryptoManager: CryptoManager,
    private val deviceIdManager: com.rentmanager.app.data.local.DeviceIdManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState(
        lockSeconds = remainingLockSeconds(tokenManager.pinLockedUntil, System.currentTimeMillis())
    ))
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()
    private var feedbackJob: Job? = null
    private var verifyJob: Job? = null

    init {
        fetchAttempts()
        viewModelScope.launch {
            while (true) {
                refreshLockout()
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

    fun refreshLockout() {
        val until = tokenManager.pinLockedUntil
        val seconds = remainingLockSeconds(until, System.currentTimeMillis())
        if (seconds == 0L) {
            if (until != 0L) tokenManager.pinLockedUntil = 0L
            _uiState.update { it.copy(lockSeconds = 0L, attemptsLeft = if (it.lockSeconds > 0) 5 else it.attemptsLeft) }
        } else {
            _uiState.update { it.copy(lockSeconds = seconds) }
        }
    }

    fun onDigitEntered(digit: String) {
        if (!canEnter() || digit.length != 1 || digit[0] !in '0'..'9') return
        val current = _uiState.value.pin
        if (current.length < 4) {
            feedbackJob?.cancel()
            _uiState.update { it.copy(pin = current + digit, errorMessage = null) }
            if (current.length == 3) {
                verifyPin(current + digit)
            }
        }
    }

    fun onDeleteDigit() {
        if (!canEnter()) return
        val current = _uiState.value.pin
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(pin = current.dropLast(1), errorMessage = null) }
        }
    }

    private fun verifyPin(pin: String) {
        val phone = tokenManager.phone ?: run {
            _uiState.update { it.copy(pin = "", isLoading = false, errorMessage = "Номер телефона не найден") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        verifyJob = viewModelScope.launch {
            try {
                val resp = authApi.verifyPassword(VerifyPasswordRequest(phone, pin, tokenManager.fcmToken))
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    tokenManager.accessToken = body.accessToken
                    tokenManager.refreshToken = body.refreshToken
                    tokenManager.pinLockedUntil = 0L
                    // Сохраняем PIN в зашифрованное хранилище для входа по отпечатку
                    cryptoManager.savePin(pin)
                    // Регистрируем FCM-токен, если сохранён ранее
                    tokenManager.fcmToken?.let { fcm ->
                        launch { try { authApi.registerDevice(RegisterDeviceRequest(fcm, deviceIdManager.deviceId)) } catch (_: Exception) {} }
                    }
                    _uiState.update { it.copy(isLoading = false, isVerified = true, attemptsLeft = 5, errorMessage = null) }
                } else {
                    val parsed = parsePinError(resp.errorBody()?.string())
                    if (resp.code() == 429 || parsed?.retryAfterSeconds != null) {
                        val seconds = parsed?.retryAfterSeconds ?: 1L
                        tokenManager.pinLockedUntil = System.currentTimeMillis() + seconds * 1000L
                        refreshLockout()
                        showError(message = null, attemptsLeft = 0, animate = true)
                    } else if (resp.code() == 401) {
                        val remaining = parsed?.attemptsLeft
                        showError(pinErrorText(remaining), remaining, animate = true)
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

    fun logout(onLoggedOut: () -> Unit) {
        val fcm = tokenManager.fcmToken
        viewModelScope.launch {
            if (fcm != null) {
                // Отвязываем FCM-токен на сервере, пока access-токен ещё валиден
                withTimeoutOrNull(3000) {
                    try {
                        val resp = authApi.unregisterDevice(RegisterDeviceRequest(fcm))
                        if (!resp.isSuccessful) android.util.Log.e("FCM", "unregister failed: ${resp.code()}")
                    } catch (e: Exception) {
                        android.util.Log.e("FCM", "unregister error: ${e.message}")
                    }
                }
            }
            tokenManager.clear()
            cryptoManager.clearPin()
            tokenManager.useBiometric = false
            onLoggedOut()
        }
    }

    fun onBiometricSuccess(pin: String) {
        if (!canEnter() || tokenManager.pinRecovery) return
        // Биометрия подтверждена — имитируем ввод PIN через клавиатуру
        _uiState.update { it.copy(pin = pin, isLoading = true, errorMessage = null) }
        verifyPin(pin)
    }

    fun onBiometricError(message: String) = showError(message, animate = false)

    private fun canEnter(): Boolean = !_uiState.value.isLoading && !_uiState.value.isErrorAnimating &&
        !_uiState.value.isVerified && !tokenManager.pinRecovery &&
        tokenManager.pinLockedUntil <= System.currentTimeMillis()

    private fun showError(message: String?, attemptsLeft: Int? = _uiState.value.attemptsLeft, animate: Boolean) {
        feedbackJob?.cancel()
        _uiState.update { it.copy(
            isLoading = false, errorMessage = message, attemptsLeft = attemptsLeft,
            isErrorAnimating = animate, errorId = it.errorId + 1,
            pin = if (animate) it.pin else ""
        ) }
        feedbackJob = viewModelScope.launch {
            // Текст ошибки и «осталось N попыток» держим до следующего ввода,
            // успешного входа или разлогина (раньше сообщение гасло через 5 секунд)
            if (animate) {
                delay(PIN_SHAKE_MS)
                _uiState.update { it.copy(pin = "", isErrorAnimating = false) }
            }
        }
    }

    fun beginRecovery() {
        if (_uiState.value.isLoading) return
        verifyJob?.cancel()
        feedbackJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val otherDevice = withTimeoutOrNull(3000) {
                try {
                    authApi.listDevices(deviceIdManager.deviceId).body()?.any { !it.currentDevice } == true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { false }
            } ?: false
            tokenManager.beginPinRecovery(otherDevice)
        }
    }
}
