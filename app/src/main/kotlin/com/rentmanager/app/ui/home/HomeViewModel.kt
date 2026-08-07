package com.rentmanager.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.local.TokenManager
import com.rentmanager.app.ui.role.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isProfileLoaded: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(userName = tokenManager.userName ?: "")
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { refreshProfile() }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val resp = authApi.getMe()
                if (resp.isSuccessful) {
                    val user = resp.body()!!
                    _uiState.update {
                        it.copy(userName = user.name, avatarUrl = user.avatarUrl, isProfileLoaded = true)
                    }
                }
            } catch (_: Exception) {}
        }
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