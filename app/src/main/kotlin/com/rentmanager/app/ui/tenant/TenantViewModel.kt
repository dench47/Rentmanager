package com.rentmanager.app.ui.tenant

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ServiceCard(
    val id: String,
    val title: String,
    val iconPlaceholder: String // Unicode/emoji as placeholder for icon
)

data class TenantUiState(
    val userName: String = "Арендатор",
    val nextPaymentDate: String = "10.02.2026",
    val nextPaymentAmount: String = "130 000 ₽",
    val isPaid: Boolean = true,
    val services: List<ServiceCard> = listOf(
        ServiceCard("1", "Недвижимость в пользовании", "\uD83C\uDFE0"),
        ServiceCard("2", "Арендодатели", "\uD83D\uDC64"),
        ServiceCard("3", "Другие объекты в пользовании", "\uD83C\uDFE2"),
        ServiceCard("4", "Финансы", "\uD83D\uDCB0"),
        ServiceCard("5", "Сообщения", "\uD83D\uDCAC"),
        ServiceCard("6", "Заказать услугу", "\uD83D\uDEE0")
    ),
    val hasPaymentButton: Boolean = false
)

class TenantViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TenantUiState())
    val uiState: StateFlow<TenantUiState> = _uiState.asStateFlow()

    fun setHasPaymentButton(has: Boolean) {
        _uiState.update { it.copy(hasPaymentButton = has) }
    }

    fun onServiceClick(serviceId: String) {
        // Navigation handled by callback
    }

    fun onPayClick() {
        // Payment action
    }
}