package com.rentmanager.app.ui.home

import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.local.TokenManager
import com.rentmanager.app.ui.role.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val selectedRole: UserRole? = null,
    val avatarUrl: String? = null,
    val isProfileLoaded: Boolean = false,
    // ===== Telegram-привязка =====
    val showTelegramPrompt: Boolean = false,
    val telegramLinkLoading: Boolean = false,
    val telegramCheckDone: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            userName = tokenManager.userName ?: "",
            avatarUrl = tokenManager.avatarUrl
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { refreshProfile() }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val resp = authApi.getMe()
                if (resp.isSuccessful) {
                    val user = resp.body()!!
                    val url = user.avatarUrl
                    _uiState.update {
                        it.copy(userName = user.name, avatarUrl = url, isProfileLoaded = true)
                    }
                    tokenManager.userName = user.name
                    tokenManager.avatarUrl = url
                    // Проверяем привязку Telegram
                    if (!_uiState.value.telegramCheckDone) {
                        checkTelegramBinding()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ===== Telegram-привязка =====

    private fun checkTelegramBinding() {
        viewModelScope.launch {
            try {
                val resp = authApi.telegramStatus()
                val body = resp.body()
                if (resp.isSuccessful) {
                    if (body?.linked != true) {
                        _uiState.update { it.copy(showTelegramPrompt = true) }
                    }
                }
            } catch (_: Exception) { }
            _uiState.update { it.copy(telegramCheckDone = true) }
        }
    }

    fun onLinkTelegram(openUrl: (String) -> Unit) {
        _uiState.update { it.copy(telegramLinkLoading = true) }
        viewModelScope.launch {
            try {
                val resp = authApi.telegramLink()
                val body = resp.body()
                if (resp.isSuccessful && body?.botUrl != null) {
                    openUrl(body.botUrl)
                    pollTelegramBinding()
                } else {
                    _uiState.update { it.copy(telegramLinkLoading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(telegramLinkLoading = false) }
            }
        }
    }

    private fun pollTelegramBinding() {
        viewModelScope.launch {
            var attempts = 0
            while (attempts < 10) {
                delay(3000)
                attempts++
                try {
                    val resp = authApi.telegramStatus()
                    if (resp.isSuccessful && resp.body()?.linked == true) {
                        _uiState.update { it.copy(showTelegramPrompt = false, telegramLinkLoading = false) }
                        return@launch
                    }
                } catch (_: Exception) { }
            }
            _uiState.update { it.copy(telegramLinkLoading = false) }
        }
    }

    fun dismissTelegramPrompt() {
        _uiState.update { it.copy(showTelegramPrompt = false) }
    }

    fun selectRole(role: UserRole) {
        tokenManager.selectedRole = role.name
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun getAutoRole(): UserRole? = when (tokenManager.selectedRole) {
        "LANDLORD" -> UserRole.LANDLORD; "TENANT" -> UserRole.TENANT; else -> null
    }

    fun updateUserName(name: String) { _uiState.update { it.copy(userName = name) } }
}