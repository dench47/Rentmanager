package com.rentmanager.app.ui.landlord.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.model.TenantDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BookingEntryUi(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val propertyName: String,
    val propertyAddress: String,
    val propertyPhoto: String?
)

data class TenantCardUiState(
    val isLoading: Boolean = true,
    val tenant: TenantDto? = null,
    /** Текущая/будущая аренда (пересекается с сегодняшним днём или позже) */
    val currentBooking: BookingEntryUi? = null,
    /** Прошлые аренды, новые сверху */
    val pastBookings: List<BookingEntryUi> = emptyList(),
    /** Паспорт раскрыт (глаз). По умолчанию — замаскирован */
    val passportVisible: Boolean = false,
    /** Служебная информация развёрнута (аккордеон) */
    val serviceInfoExpanded: Boolean = false,
    /** История аренды развёрнута (аккордеон) */
    val historyExpanded: Boolean = false
)

@HiltViewModel
class TenantCardViewModel @Inject constructor(
    private val tenantApi: TenantApi,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantCardUiState())
    val uiState: StateFlow<TenantCardUiState> = _uiState.asStateFlow()

    /** Правило «отображение сразу»: весь экран целиком в первый кадр.
     *  init ViewModel выполняется до отрисовки:
     *   • повторный вход → полный кеш /card (арендатор + Арендует + история);
     *   • первый вход → кеш списка (имя/телефон/ава), брони доедут за ~50мс
     *     запроса — внутри анимации входа, глазом не видно. */
    private val tenantId: String = savedStateHandle.get<String>("tenantId").orEmpty()

    init {
        val cachedCard = TenantCardCache.takeCardIfMatches(tenantId)
        if (cachedCard != null) {
            _uiState.update { applyCard(it, cachedCard) }
        } else {
            TenantCardCache.takeIfMatches(tenantId)?.let { cached ->
                _uiState.update { it.copy(isLoading = false, tenant = cached) }
            }
        }
        load(tenantId)
    }

    private fun applyCard(state: TenantCardUiState, dto: TenantDto): TenantCardUiState {
        val entries = dto.bookings.orEmpty().mapNotNull { b ->
            runCatching {
                BookingEntryUi(
                    startDate = LocalDate.parse(b.startDate),
                    endDate = LocalDate.parse(b.endDate),
                    propertyName = b.propertyName?.ifBlank { null } ?: "Без названия",
                    propertyAddress = (b.propertyAddress ?: "").substringBefore(',').trim(),
                    propertyPhoto = b.propertyPhoto
                )
            }.getOrNull()
        }
        val today = LocalDate.now()
        return state.copy(
            isLoading = false,
            tenant = dto,
            currentBooking = entries.lastOrNull { !it.endDate.isBefore(today) },
            pastBookings = entries.filter { it.endDate.isBefore(today) }
                .sortedByDescending { it.startDate }
        )
    }

    fun load(tenantId: String) {
        viewModelScope.launch {
            // Один запрос /tenants/{id}/card: арендатор + брони с объектами
            runCatching { tenantApi.getTenantCard(tenantId).body() }.getOrNull()?.let { dto ->
                TenantCardCache.putCard(dto)
                _uiState.update { applyCard(it, dto) }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun togglePassport() = _uiState.update { it.copy(passportVisible = !it.passportVisible) }
    fun toggleServiceInfo() = _uiState.update { it.copy(serviceInfoExpanded = !it.serviceInfoExpanded) }
    fun toggleHistory() = _uiState.update { it.copy(historyExpanded = !it.historyExpanded) }
}

/** Маска паспорта: серия «·· 08», номер «······2545» (первые цифры скрыты). */
fun passportMasked(passportData: String?): Pair<String, String>? {
    if (passportData.isNullOrBlank()) return null
    val digits = passportData.filter { it.isDigit() }
    if (digits.length < 6) return null
    val series = "·· " + digits.substring(digits.length - 6, digits.length - 4)
    val number = "······" + digits.takeLast(4)
    return series to number
}

/** Серия/номер целиком (глаз открыт): «45 08» / «7485912545». */
fun passportPlain(passportData: String?): Pair<String, String>? {
    if (passportData.isNullOrBlank()) return null
    val digits = passportData.filter { it.isDigit() }
    if (digits.length < 6) return null
    val series = digits.substring(digits.length - 6, digits.length - 4)
    val number = digits.substring(digits.length - 6 + 2).ifBlank { digits.takeLast(4) }
    return series to number
}
