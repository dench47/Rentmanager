package com.rentmanager.app.ui.keyboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KeyboardUiState(
    val inputValue: String = "",
    val phoneNumber: String = "+7 ",
    val isNumericMode: Boolean = true
)

class KeyboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    fun onKeyPress(key: String) {
        _uiState.update { state ->
            if (state.phoneNumber.length >= 18) return@update state // max length
            state.copy(phoneNumber = state.phoneNumber + key)
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            if (state.phoneNumber.length > 3) {
                state.copy(phoneNumber = state.phoneNumber.dropLast(1))
            } else state
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(inputValue = value) }
    }
}