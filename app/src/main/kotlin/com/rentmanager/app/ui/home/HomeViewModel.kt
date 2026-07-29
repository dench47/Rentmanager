package com.rentmanager.app.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class UserRole { LANDLORD, TENANT }

data class HomeUiState(
    val userName: String = "Андрей",
    val selectedRole: UserRole? = null,
    val avatarUrl: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun selectRole(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }
}