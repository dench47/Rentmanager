package com.rentmanager.app.ui.auth.name

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EnterNameUiState(
    val name: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class EnterNameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EnterNameUiState())
    val uiState: StateFlow<EnterNameUiState> = _uiState.asStateFlow()

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName, errorMessage = null) }
    }

    fun onContinue() {
        val name = _uiState.value.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Введите имя") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }
}