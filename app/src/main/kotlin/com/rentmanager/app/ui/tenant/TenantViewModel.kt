package com.rentmanager.app.ui.tenant

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.rentmanager.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ServiceCard(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    val twoLines: Boolean = false
)

data class TenantUiState(
    val userName: String = "Арендатор",
    val nextPaymentDate: String = "10.02.2026",
    val nextPaymentAmount: String = "130 000 ₽",
    val isPaid: Boolean = true,
    val services: List<ServiceCard> = listOf(
        ServiceCard("1", "Недвижимость\nв пользовании", R.drawable.ic_card_my_properties, twoLines = true),
        ServiceCard("2", "Арендодатели", R.drawable.ic_card_tenants),
        ServiceCard("3", "Другие объекты\nв пользовании", R.drawable.ic_card_other, twoLines = true),
        ServiceCard("4", "Финансы", R.drawable.ic_card_finance),
        ServiceCard("5", "Сообщения", R.drawable.ic_card_messages_no_badge),
        ServiceCard("6", "Заказать услугу", R.drawable.ic_card_service)
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