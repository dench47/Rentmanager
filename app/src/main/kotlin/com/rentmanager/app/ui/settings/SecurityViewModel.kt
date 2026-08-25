package com.rentmanager.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.TrustedDeviceDto
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
                    _uiState.update { it.copy(email = user.email, emailVerified = user.emailVerified == true) }
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
}