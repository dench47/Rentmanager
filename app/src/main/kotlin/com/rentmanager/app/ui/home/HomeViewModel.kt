package com.rentmanager.app.ui.home

import androidx.lifecycle.ViewModel
import com.rentmanager.app.data.local.TokenManager
import com.rentmanager.app.ui.role.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val selectedRole: UserRole? = null,
    val avatarUrl: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(userName = tokenManager.userName ?: "Пользователь")
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun selectRole(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }
}
