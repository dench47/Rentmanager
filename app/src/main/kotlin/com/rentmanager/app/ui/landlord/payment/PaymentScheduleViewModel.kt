package com.rentmanager.app.ui.landlord.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.model.PaymentScheduleDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentScheduleUiState(
    val isLoading: Boolean = true,
    val saved: Boolean = false,
    val schedule: PaymentScheduleDto? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PaymentScheduleViewModel @Inject constructor(
    private val financeApi: FinanceApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentScheduleUiState())
    val uiState: StateFlow<PaymentScheduleUiState> = _uiState.asStateFlow()

    fun load(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = financeApi.getSchedules()
                if (resp.isSuccessful) {
                    val schedule = resp.body()?.firstOrNull { it.propertyId == propertyId }
                    _uiState.update { it.copy(isLoading = false, schedule = schedule) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка загрузки") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun save(propertyId: String, dayOfMonth: Int?, amount: Double?, customDates: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, saved = false) }
            try {
                val resp = financeApi.createSchedule(
                    PaymentScheduleDto(
                        propertyId = propertyId,
                        dayOfMonth = dayOfMonth,
                        amount = amount,
                        type = if (dayOfMonth != null) "auto" else "manual",
                        customDates = customDates
                    )
                )
                if (resp.isSuccessful) _uiState.update { it.copy(isLoading = false, saved = true) }
                else _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка сохранения") }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }
}
