package com.rentmanager.app.ui.landlord.main

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.rentmanager.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LandlordCard(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int
)

data class LandlordUiState(
    val userName: String = "Арендодатель",
    val propertiesCount: Int = 5,
    val activeTenantsCount: Int = 3,
    val monthlyIncome: String = "1 700 000 ₽/мес",
    val nextPaymentDate: String = "10.02.2026",
    val nextPaymentAmount: String = "130 000 ₽",
    val isPaid: Boolean = true,
    val hasUnreadMessages: Boolean = true,
    val cards: List<LandlordCard> = listOf(
        LandlordCard("1", "Моя недвижимость", R.drawable.ic_card_my_properties),
        LandlordCard("2", "Арендаторы", R.drawable.ic_card_tenants),
        LandlordCard("3", "Другие объекты", R.drawable.ic_card_other),
        LandlordCard("4", "Финансы", R.drawable.ic_card_finance),
        LandlordCard("5", "Сообщения",
            if (hasUnreadMessages) R.drawable.ic_card_messages_unread else R.drawable.ic_card_messages_no_badge
        ),
        LandlordCard("6", "Заказать услугу", R.drawable.ic_card_service)
    )
)

class LandlordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LandlordUiState())
    val uiState: StateFlow<LandlordUiState> = _uiState.asStateFlow()

    fun onCardClick(cardId: String) {
        // Navigation handled by callback
    }
}