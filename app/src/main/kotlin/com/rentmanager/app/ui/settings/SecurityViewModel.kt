package com.rentmanager.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.EmailToggle2FARequest
import com.rentmanager.app.data.api.EmailVerifyRequest
import com.rentmanager.app.data.api.TrustedDeviceDto
import com.rentmanager.app.data.api.UpdateProfileRequest
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

/** Способ подтверждения входа без звонка. */
enum class VerificationMethod(val label: String) {
    TELEGRAM("Telegram"),
    EMAIL("Email"),
    MAX("Макс (скоро)")
}

data class SecurityUiState(
    val localPinEnabled: Boolean = true,
    val useBiometric: Boolean = false,
    val trustedDevices: List<TrustedDeviceDto> = emptyList(),
    val telegramLinked: Boolean = false,
    val telegramUsername: String? = null,
    val email: String? = null,
    val emailVerified: Boolean = false,
    val email2faEnabled: Boolean = false,
    val maxAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val cryptoManager: CryptoManager,
    private val deviceIdManager: DeviceIdManager,
    private val loginApprovalEvents: LoginApprovalEvents
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SecurityUiState(
            localPinEnabled = tokenManager.localPinEnabled,
            useBiometric = tokenManager.useBiometric
        )
    )
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        viewModelScope.launch {
            loginApprovalEvents.devicesChanged.collect { loadTrustedDevices() }
        }
    }

    fun loadAll() {
        loadProfile()
        loadTrustedDevices()
        loadTelegramStatus()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val resp = authApi.getMe()
                if (resp.isSuccessful) {
                    val user = resp.body()!!
                    _uiState.update { it.copy(email = user.email, emailVerified = user.emailVerified == true, email2faEnabled = user.email2faEnabled == true) }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadTelegramStatus() {
        viewModelScope.launch {
            try {
                val resp = authApi.telegramStatus()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _uiState.update {
                        it.copy(
                            telegramLinked = body?.linked == true,
                            telegramUsername = body?.username
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadTrustedDevices() {
        viewModelScope.launch {
            try {
                val resp = authApi.listDevices(deviceIdManager.deviceId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(trustedDevices = resp.body() ?: emptyList()) }
                }
            } catch (_: Exception) {}
        }
    }

    fun revokeTrustedDevice(deviceRowId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = try { authApi.revokeDevice(deviceRowId).isSuccessful } catch (_: Exception) { false }
            if (ok) loadTrustedDevices()
            onDone(ok)
        }
    }

    // ===== Тумблеры =====

    fun onTogglePin(enabled: Boolean) {
        if (enabled) {
            if (tokenManager.hasPassword) {
                tokenManager.localPinEnabled = true
                _uiState.update { it.copy(localPinEnabled = true) }
            }
        } else {
            tokenManager.localPinEnabled = false
            cryptoManager.clearPin()
            _uiState.update { it.copy(localPinEnabled = false, useBiometric = false) }
        }
    }

    fun onToggleBiometric(enabled: Boolean) {
        tokenManager.useBiometric = enabled
        _uiState.update { it.copy(useBiometric = enabled) }
    }

    // ===== Telegram =====

    fun onLinkTelegram(openUrl: (String) -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val resp = authApi.telegramLink()
                val body = resp.body()
                if (resp.isSuccessful && body?.botUrl != null) {
                    openUrl(body.botUrl)
                    pollTelegramBinding()
                }
            } catch (_: Exception) {}
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun pollTelegramBinding() {
        viewModelScope.launch {
            var attempts = 0
            while (attempts < 10) {
                kotlinx.coroutines.delay(3000)
                attempts++
                try {
                    val resp = authApi.telegramStatus()
                    if (resp.isSuccessful && resp.body()?.linked == true) {
                        _uiState.update { it.copy(telegramLinked = true, telegramUsername = resp.body()?.username) }
                        return@launch
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun onUnlinkTelegram() {
        viewModelScope.launch {
            try { authApi.telegramUnlink() } catch (_: Exception) {}
            _uiState.update { it.copy(telegramLinked = false, telegramUsername = null) }
        }
    }

    // ===== Email =====

    /** Сохраняет почту (сброс подтверждения происходит на сервере). */
    fun setEmail(email: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                authApi.updateProfile(UpdateProfileRequest(email = email))
                _uiState.update { it.copy(email = email, emailVerified = false, email2faEnabled = false) }
            } catch (_: Exception) {}
            onDone()
        }
    }

    /** Отправляет код подтверждения на текущую почту. */
    fun onEmailSendCode(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = authApi.emailSendCode()
                if (resp.isSuccessful) onResult(true, null)
                else onResult(false, resp.errorBody()?.string() ?: "Не удалось отправить код")
            } catch (_: Exception) {
                onResult(false, "Нет связи с сервером")
            }
        }
    }

    /** Проверяет код подтверждения почты и включает 2FA. */
    fun onEmailVerify(code: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = authApi.emailVerify(EmailVerifyRequest(code))
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(emailVerified = true, email2faEnabled = true) }
                    try { authApi.emailToggle2FA(EmailToggle2FARequest(true)) } catch (_: Exception) {}
                    onResult(true, null)
                } else {
                    onResult(false, "Неверный или истёкший код")
                }
            } catch (_: Exception) {
                onResult(false, "Нет связи с сервером")
            }
        }
    }

    /** Включает/выключает способ входа через Email (почта уже подтверждена). */
    fun onToggleEmail2FA(enabled: Boolean) {
        viewModelScope.launch {
            try { authApi.emailToggle2FA(EmailToggle2FARequest(enabled)) } catch (_: Exception) {}
            _uiState.update { it.copy(email2faEnabled = enabled) }
        }
    }
}