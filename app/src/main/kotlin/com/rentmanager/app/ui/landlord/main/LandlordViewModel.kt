package com.rentmanager.app.ui.landlord.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LandlordCard(
    val id: String,
    val title: String,
    val description: String = "",
    val iconPlaceholder: String
)

data class LandlordUiState(
    val userName: String = "Арендодатель",
    val propertiesCount: Int = 5,
    val activeTenantsCount: Int = 3,
    val monthlyIncome: String = "1 700 000 ₽/мес",
    val nextPaymentDate: String = "10.02.2026",
    val nextPaymentAmount: String = "130 000 ₽",
    val isPaid: Boolean = true,
    val cards: List<LandlordCard> = listOf(
        LandlordCard("1", "Моя недвижимость", "", "\uD83C\uDFE0"),
        LandlordCard("2", "Арендаторы", "", "\uD83D\uDC64"),
        LandlordCard("3", "Другие объекты", "", "\uD83C\uDFE2"),
        LandlordCard("4", "Финансы", "", "\uD83D\uDCB0"),
        LandlordCard("5", "Сообщения", "", "\uD83D\uDCAC"),
        LandlordCard("6", "Заказать услугу", "", "\u2795")
    )
)

class LandlordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LandlordUiState())
    val uiState: StateFlow<LandlordUiState> = _uiState.asStateFlow()

    fun onCardClick(cardId: String) {
        // Navigation handled by callback
    }
}