package com.rentmanager.app.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.api.SubscriptionOperationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class OperationsUiState(
    val operations: List<SubscriptionOperationDto> = emptyList(),
    /** 0 — все, 1 — зачисления, 2 — списания */
    val filter: Int = 0,
    val isLoading: Boolean = true,
    /** Баланс и число объектов подписки — для редкого пустого состояния
     *  «деньги закинули, объекта ещё нет» (2996:42128) */
    val balance: Double = 0.0,
    val objects: Int = 0
)

/** История операций подписки: сервер отдаёт списком, группируем по месяцам. */
@HiltViewModel
class OperationsHistoryViewModel @Inject constructor(
    private val financeApi: FinanceApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperationsUiState())
    val uiState: StateFlow<OperationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val body = financeApi.subscriptionOperations().body().orEmpty()
                _uiState.update { it.copy(operations = body, isLoading = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
            runCatching {
                financeApi.subscriptionState().body()?.let { s ->
                    _uiState.update { it.copy(balance = s.balance, objects = s.objects) }
                }
            }
        }
    }

    fun setFilter(filter: Int) {
        _uiState.update { it.copy(filter = filter) }
    }

    /** Дата операции для группировки по месяцам (created_at — RFC3339). */
    fun monthOf(op: SubscriptionOperationDto): LocalDate =
        runCatching {
            Instant.parse(op.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }.getOrDefault(LocalDate.now())
}
