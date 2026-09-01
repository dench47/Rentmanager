package com.rentmanager.app.ui.role

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.R
import com.rentmanager.app.data.api.BookingApi
import com.rentmanager.app.data.api.CreatePaymentRequest
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.api.PropertyApi
import com.rentmanager.app.data.local.RoleStatsCache
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
    /** Есть активная аренда (график платежей) — определяет плашку «Нет активной аренды». */
    val hasActiveRent: Boolean = false,
    val isLoading: Boolean = false,
    val hasDebt: Boolean = false,
    val nextPaymentDate: String = "",
    val nextPaymentAmount: String = "",
    val monthlyIncome: String = "",
    val debtAmount: String = "",
    val hasUnreadMessages: Boolean = true,
    val cards: List<RoleCard> = emptyList()
)

@HiltViewModel
class RoleViewModel @Inject constructor(
    private val propertyApi: PropertyApi,
    private val financeApi: FinanceApi,
    private val bookingApi: BookingApi,
    private val statsCache: RoleStatsCache
) : ViewModel() {

    private var overduePropertyId: String? = null
    private var overdueAmount: Double = 0.0

    private val landlordCards = listOf(
        RoleCard("1", "Моя\nнедвижимость", R.drawable.ic_menu_my_properties),
        RoleCard("2", "Арендаторы", R.drawable.ic_menu_tenants),
        RoleCard("3", "Другие объекты", R.drawable.ic_menu_other_objects),
        RoleCard("4", "Финансы", R.drawable.ic_menu_finance),
        RoleCard("5", "Сообщения", R.drawable.ic_menu_messages_no_badge),
        RoleCard("6", "Заказать услугу", R.drawable.ic_menu_order_service)
    )

    private val tenantCards = listOf(
        RoleCard("1", "Недвижимость\nв пользовании", R.drawable.ic_menu_my_properties),
        RoleCard("2", "Арендодатели", R.drawable.ic_menu_tenants),
        RoleCard("3", "Другие объекты\nв пользовании", R.drawable.ic_menu_other_objects),
        RoleCard("4", "Финансы", R.drawable.ic_menu_finance),
        RoleCard("5", "Сообщения", R.drawable.ic_menu_messages_no_badge),
        RoleCard("6", "Заказать услугу", R.drawable.ic_menu_order_service)
    )

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
        val title = if (role == UserRole.LANDLORD) "Арендодатель" else "Арендатор"
        val cards = if (role == UserRole.LANDLORD) landlordCards else tenantCards
        val cached = statsCache.load(key(role))
        _uiState.value = if (cached != null) {
            RoleUiState(
                role = role,
                title = title,
                hasDeals = cached.hasDeals,
                hasActiveRent = cached.hasActiveRent,
                isLoading = false,
                hasDebt = cached.hasDebt,
                debtAmount = cached.debtAmount,
                nextPaymentDate = cached.nextPaymentDate,
                nextPaymentAmount = cached.nextPaymentAmount,
                monthlyIncome = cached.monthlyIncome,
                cards = cards
            )
        } else {
            RoleUiState(role = role, title = title, hasDeals = false, isLoading = true, cards = cards)
        }
    }

    fun refresh(role: UserRole) {
        // Обновляем при каждом входе на экран: состояние могло измениться
        // (закрасили сегмент в шахматке и вернулись — плашка должна быть свежей)
        if (role == UserRole.TENANT) loadTenantFinance() else loadLandlordStats()
    }

    private fun key(role: UserRole) = if (role == UserRole.LANDLORD) "landlord" else "tenant"

    fun onCardClick(cardId: String) {
        // Navigation handled by callback
    }

    fun loadLandlordStats() {
        viewModelScope.launch {
            try {
                val resp = propertyApi.getProperties()
                if (!resp.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                val props = resp.body() ?: emptyList()
                val schedules = financeApi.getSchedules().body() ?: emptyList()
                if (props.isEmpty()) {
                    statsCache.save("landlord", RoleStatsCache.Stats(hasDeals = false, hasActiveRent = false, hasDebt = false, nextPaymentDate = "", nextPaymentAmount = "", monthlyIncome = "", debtAmount = ""))
                    _uiState.update { it.copy(hasDeals = false, hasActiveRent = false, hasDebt = false, isLoading = false, nextPaymentDate = "", nextPaymentAmount = "", monthlyIncome = "", debtAmount = "") }
                    return@launch
                }
                var hasDebt = false
                var debtSum = 0.0
                for (p in props) {
                    val schedule = schedules.firstOrNull { it.propertyId == p.id }
                    if (schedule != null) {
                        val payments = financeApi.listPayments(p.id).body() ?: emptyList()
                        if (PaymentOverdue.isOverdue(schedule, payments)) {
                            hasDebt = true
                            debtSum += PaymentOverdue.overdueAmount(schedule, payments)
                        }
                    }
                }
                val debtAmount = if (hasDebt) formatAmount(debtSum) + " ₽" else ""
                val nearest = schedules
                    .mapNotNull { PaymentOverdue.nextPayment(it) }
                    .filter { it.date != null }
                    .minByOrNull { it.date!! }
                val nextDate = nearest?.date?.let(::formatDate) ?: ""
                val nextAmount = nearest?.amount?.let(::formatAmount) ?: ""
                // Доход по всем объектам: график платежей есть — его месячная сумма;
                // иначе ставка объекта: длительно — как есть, посуточно — приведение к месяцу (×30)
                val income = formatAmount(
                    props.sumOf { p ->
                        val schedule = schedules.firstOrNull { it.propertyId == p.id }
                        if (schedule != null) {
                            PaymentOverdue.monthlyAmount(schedule)
                        } else {
                            val rate = p.rentAmount ?: 0.0
                            if (p.rentType == "длительно") rate else rate * 30
                        }
                    }
                ) + "/мес"
                // Активная аренда = есть зелёные ячейки в шахматке (хоть одна бронь)
                var hasActiveRent = false
                for (p in props) {
                    val bookings = bookingApi.getBookings(p.id).body().orEmpty()
                    if (bookings.isNotEmpty()) {
                        hasActiveRent = true
                        break
                    }
                }
                statsCache.save("landlord", RoleStatsCache.Stats(hasDeals = true, hasActiveRent = hasActiveRent, hasDebt = hasDebt, nextPaymentDate = nextDate, nextPaymentAmount = nextAmount, monthlyIncome = income, debtAmount = debtAmount))
                _uiState.update {
                    it.copy(hasDeals = true, hasActiveRent = hasActiveRent, isLoading = false, hasDebt = hasDebt, nextPaymentDate = nextDate, nextPaymentAmount = nextAmount, monthlyIncome = income, debtAmount = debtAmount)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadTenantFinance() {
        viewModelScope.launch {
            try {
                val resp = propertyApi.getTenantProperties()
                if (!resp.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                val props = resp.body() ?: emptyList()
                val schedules = financeApi.getTenantSchedules().body() ?: emptyList()
                if (props.isEmpty()) {
                    overduePropertyId = null
                    overdueAmount = 0.0
                    statsCache.save("tenant", RoleStatsCache.Stats(hasDeals = false, hasDebt = false, nextPaymentDate = "", nextPaymentAmount = "", monthlyIncome = ""))
                    _uiState.update { it.copy(hasDeals = false, hasDebt = false, isLoading = false, nextPaymentDate = "", nextPaymentAmount = "") }
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
                val nextDate = nearest?.date?.let(::formatDate) ?: ""
                val nextAmount = nearest?.amount?.let(::formatAmount) ?: ""
                statsCache.save("tenant", RoleStatsCache.Stats(hasDeals = true, hasDebt = hasDebt, nextPaymentDate = nextDate, nextPaymentAmount = nextAmount, monthlyIncome = ""))
                _uiState.update {
                    it.copy(hasDeals = true, isLoading = false, hasDebt = hasDebt, nextPaymentDate = nextDate, nextPaymentAmount = nextAmount)
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