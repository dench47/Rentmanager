package com.rentmanager.app.ui.counter.add

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AddCounterUiState(
    val counterType: String = "Электроэнергия",
    val counterNumber: String = "",
    val initialValue: String = "",
    val date: String = "",
    val remindToSubmit: Boolean = false,
    val types: List<String> = listOf(
        "Электроэнергия",
        "Холодная вода",
        "Горячая вода",
        "Отопление"
    ),
    val isTypeDropdownOpen: Boolean = false
)

class AddCounterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AddCounterUiState())
    val uiState: StateFlow<AddCounterUiState> = _uiState.asStateFlow()

    fun selectType(type: String) {
        _uiState.update { it.copy(counterType = type, isTypeDropdownOpen = false) }
    }

    fun toggleTypeDropdown() {
        _uiState.update { it.copy(isTypeDropdownOpen = !it.isTypeDropdownOpen) }
    }

    fun onNumberChange(value: String) {
        _uiState.update { it.copy(counterNumber = value) }
    }

    fun onValueChange(value: String) {
        _uiState.update { it.copy(initialValue = value) }
    }

    fun onDateChange(value: String) {
        _uiState.update { it.copy(date = value) }
    }

    fun toggleRemind() {
        _uiState.update { it.copy(remindToSubmit = !it.remindToSubmit) }
    }
}