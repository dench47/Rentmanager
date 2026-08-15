package com.rentmanager.app.ui.role

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.rentmanager.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserRole { LANDLORD, TENANT }

data class RoleCard(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    val showBadge: Boolean = false
)

data class RoleUiState(
    val role: UserRole,
    val title: String,
    val hasDeals: Boolean,
    val hasDebt: Boolean = false,
    val nextPaymentDate: String = "",
    val nextPaymentAmount: String = "",
    val monthlyIncome: String = "",
    val hasUnreadMessages: Boolean = true,
    val cards: List<RoleCard> = emptyList()
)

class RoleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        RoleUiState(
            role = UserRole.LANDLORD,
            title = "",
            hasDeals = false
        )
    )
    val uiState: StateFlow<RoleUiState> = _uiState.asStateFlow()

    fun setRole(role: UserRole, hasDeals: Boolean = true, hasDebt: Boolean = false) {
        when (role) {
            UserRole.LANDLORD -> {
                _uiState.value = RoleUiState(
                    role = UserRole.LANDLORD,
                    title = "Арендодатель",
                    hasDeals = hasDeals,
                    hasDebt = hasDebt,
                    nextPaymentDate = "10.02.2026",
                    nextPaymentAmount = "130 000 ₽",
                    monthlyIncome = "1 700 000 ₽/мес",
                    cards = listOf(
                        RoleCard("1", "Моя\nнедвижимость", R.drawable.ic_card_my_properties),
                        RoleCard("2", "Арендаторы", R.drawable.ic_card_tenants),
                        RoleCard("3", "Другие объекты", R.drawable.ic_card_other),
                        RoleCard("4", "Финансы", R.drawable.ic_card_finance),
                        RoleCard("5", "Сообщения", R.drawable.ic_card_messages_unread),
                        RoleCard("6", "Заказать услугу", R.drawable.ic_card_service)
                    )
                )
            }
            UserRole.TENANT -> {
                _uiState.value = RoleUiState(
                    role = UserRole.TENANT,
                    title = "Арендатор",
                    hasDeals = hasDeals,
                    hasDebt = hasDebt,
                    nextPaymentDate = "10.02.2026",
                    nextPaymentAmount = "130 000 ₽",
                    cards = listOf(
                        RoleCard("1", "Недвижимость\nв пользовании", R.drawable.ic_card_my_properties),
                        RoleCard("2", "Арендодатели", R.drawable.ic_card_tenants),
                        RoleCard("3", "Другие объекты\nв пользовании", R.drawable.ic_card_other),
                        RoleCard("4", "Финансы", R.drawable.ic_card_finance),
                        RoleCard("5", "Сообщения", R.drawable.ic_card_messages_no_badge),
                        RoleCard("6", "Заказать услугу", R.drawable.ic_card_service)
                    )
                )
            }
        }
    }

    fun onCardClick(cardId: String) {
        // Navigation handled by callback
    }
}