package com.rentmanager.app.ui.role

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.R
import com.rentmanager.app.data.api.CreatePaymentRequest
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.api.PropertyApi
import com.rentmanager.app.util.PaymentOverdue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

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
    val isLoading: Boolean = false,
    val hasDebt: Boolean = false,
    val nextPaymentDate: String = "",
    val nextPaymentAmount: String = "",
    val monthlyIncome: String = "",
    val hasUnreadMessages: Boolean = true,
    val cards: List<RoleCard> = emptyList()
)

@HiltViewModel
class RoleViewModel @Inject constructor(
    private val propertyApi: PropertyApi,
    private val financeApi: FinanceApi
) : ViewModel() {

    private var overduePropertyId: String? = null
    private var overdueAmount: Double = 0.0

    private val _uiState = MutableStateFlow(
        RoleUiState(
            role = UserRole.LANDLORD,
            title = "",
            hasDeals = false,
            isLoading = true
        )
    )
    val uiState: StateFlow<RoleUiState> = _uiState.asStateFlow()

    fun setRole(role: UserRole) {
        _uiState.value = when (role) {
            UserRole.LANDLORD -> RoleUiState(
                role = UserRole.LANDLORD,
                title = "Арендодатель",
                hasDeals = false,
                isLoading = true,
                cards = listOf(
                    RoleCard("1", "Моя\nнедвижимость", R.drawable.ic_card_my_properties),
                    RoleCard("2", "Арендаторы", R.drawable.ic_card_tenants),
                    RoleCard("3", "Другие объекты", R.drawable.ic_card_other),
                    RoleCard("4", "Финансы", R.drawable.ic_card_finance),
                    RoleCard("5", "Сообщения", R.drawable.ic_card_messages_unread),
                    RoleCard("6", "Заказать услугу", R.drawable.ic_card_service)
                )
            )
            UserRole.TENANT -> RoleUiState(
                role = UserRole.TENANT,
                title = "Арендатор",
                hasDeals = false,
                isLoading = true,
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

    fun onCardClick(cardId: String) {
        // Navigation handled by callback
    }

    fun loadLandlordStats() {
        viewModelScope.launch {
            try {
                val props = propertyApi.getProperties().body() ?: emptyList()
                val schedules = financeApi.getSchedules().body() ?: emptyList()
                if (props.isEmpty()) {
                    _uiState.update { it.copy(hasDeals = false, hasDebt = false, isLoading = false) }
                    return@launch
                }
                var hasDebt = false
                for (p in props) {
                    val schedule = schedules.firstOrNull { it.propertyId == p.id }
                    if (schedule != null) {
                        val payments = financeApi.listPayments(p.id).body() ?: emptyList()
                        if (PaymentOverdue.isOverdue(schedule, payments)) {
                            hasDebt = true
                            break
                        }
                    }
                }
                val nearest = schedules
                    .mapNotNull { PaymentOverdue.nextPayment(it) }
                    .filter { it.date != null }
                    .minByOrNull { it.date!! }
                val total = schedules.sumOf { PaymentOverdue.monthlyAmount(it) }
                _uiState.update {
                    it.copy(
                        hasDeals = true,
                        isLoading = false,
                        hasDebt = hasDebt,
                        nextPaymentDate = nearest?.date?.let(::formatDate) ?: "",
                        nextPaymentAmount = nearest?.amount?.let(::formatAmount) ?: "",
                        monthlyIncome = formatAmount(total) + "/мес"
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadTenantFinance() {
        viewModelScope.launch {
            try {
                val props = propertyApi.getTenantProperties().body() ?: emptyList()
                val schedules = financeApi.getTenantSchedules().body() ?: emptyList()
                if (props.isEmpty()) {
                    overduePropertyId = null
                    overdueAmount = 0.0
                    _uiState.update { it.copy(hasDeals = false, hasDebt = false, isLoading = false) }
                    return@launch
                }
                val ownSchedules = schedules.filter { s -> props.any { it.id == s.propertyId } }
                val nearest = ownSchedules
                    .mapNotNull { PaymentOverdue.nextPayment(it) }
                    .filter { it.date != null }
                    .minByOrNull { it.date!! }

                var hasDebt = false
                var debtPropertyId: String? = null
                var debtAmount = 0.0
                for (p in props) {
                    val schedule = schedules.firstOrNull { it.propertyId == p.id }
                    if (schedule != null) {
                        val payments = financeApi.listPayments(p.id).body() ?: emptyList()
                        if (PaymentOverdue.isOverdue(schedule, payments)) {
                            hasDebt = true
                            debtPropertyId = p.id
                            debtAmount = schedule.amount ?: 0.0
                            break
                        }
                    }
                }
                overduePropertyId = debtPropertyId
                overdueAmount = debtAmount
                _uiState.update {
                    it.copy(
                        hasDeals = true,
                        isLoading = false,
                        hasDebt = hasDebt,
                        nextPaymentDate = nearest?.date?.let(::formatDate) ?: "",
                        nextPaymentAmount = nearest?.amount?.let(::formatAmount) ?: ""
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun pay() {
        val propertyId = overduePropertyId ?: return
        viewModelScope.launch {
            try {
                financeApi.createPayment(propertyId, CreatePaymentRequest(overdueAmount))
                loadTenantFinance()
            } catch (_: Exception) { }
        }
    }

    private fun formatAmount(amount: Double?): String {
        val v = amount ?: return ""
        return String.format("%,.0f ₽", v).replace(',', ' ')
    }

    private fun formatDate(date: LocalDate): String =
        "%02d.%02d.%d".format(date.dayOfMonth, date.monthValue, date.year)
}