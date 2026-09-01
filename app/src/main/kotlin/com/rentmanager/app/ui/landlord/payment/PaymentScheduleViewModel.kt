package com.rentmanager.app.ui.landlord.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.BookingApi
import com.rentmanager.app.data.api.CreateBookingRequest
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.model.PaymentScheduleDto
import com.rentmanager.app.data.model.forProperty
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.PropertyRepository
import com.rentmanager.app.util.mergeRanges
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PaymentScheduleUiState(
    val isLoading: Boolean = true,
    val saved: Boolean = false,
    val schedule: PaymentScheduleDto? = null,
    val property: PropertyDto? = null,
    /** Брони объекта — источник дат переменного графика посуточной аренды */
    val bookings: List<Pair<LocalDate, LocalDate>> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class PaymentScheduleViewModel @Inject constructor(
    private val financeApi: FinanceApi,
    private val bookingApi: BookingApi,
    private val propertyRepository: PropertyRepository,
    private val detailCache: PropertyDetailCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentScheduleUiState())
    val uiState: StateFlow<PaymentScheduleUiState> = _uiState.asStateFlow()

    fun load(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val schedule = financeApi.getSchedules().body()
                    ?.forProperty(propertyId)
                val property = detailCache.load(propertyId)?.property ?: runCatching {
                    propertyRepository.getProperties().body()?.firstOrNull { it.id == propertyId }
                }.getOrNull()
                val bookings = loadBookings(propertyId)
                _uiState.update {
                    it.copy(isLoading = false, schedule = schedule, property = property, bookings = bookings)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    /** Посуточно + переменный график: добавление даты = бронь этих суток.
     *  Дата уже занята (есть в любой брони) — бронь не создаём, дату не дублируем. */
    fun addBookingDay(propertyId: String, date: LocalDate, onDone: (added: Boolean) -> Unit) {
        viewModelScope.launch {
            var added = false
            try {
                val already = _uiState.value.bookings.any { (s, e) ->
                    !date.isBefore(s) && !date.isAfter(e)
                }
                if (!already) {
                    bookingApi.createBooking(propertyId, CreateBookingRequest(date.toString(), date.toString()))
                    added = true
                }
                _uiState.update { it.copy(bookings = loadBookings(propertyId)) }
            } catch (_: Exception) { }
            onDone(added)
        }
    }

    /** Посуточно + переменный график: снятие броней, пересекающихся с периодом строки списка. */
    fun deleteBooking(propertyId: String, start: LocalDate, end: LocalDate, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val toDelete = bookingApi.getBookings(propertyId).body().orEmpty()
                    .filter { b ->
                        val s = runCatching { LocalDate.parse(b.startDate) }.getOrNull()
                        val e = runCatching { LocalDate.parse(b.endDate) }.getOrNull()
                        s != null && e != null && !start.isAfter(e) && !end.isBefore(s)
                    }
                toDelete.forEach { it.id?.let { id -> runCatching { bookingApi.deleteBooking(propertyId, id) } } }
                _uiState.update { it.copy(bookings = loadBookings(propertyId)) }
            } catch (_: Exception) { }
            onDone()
        }
    }

    fun save(
        propertyId: String,
        dayOfMonth: Int?,
        amount: Double?,
        customDates: String?,
        requisites: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, saved = false) }
            try {
                val resp = financeApi.createSchedule(
                    PaymentScheduleDto(
                        propertyId = propertyId,
                        dayOfMonth = dayOfMonth,
                        amount = amount,
                        type = if (dayOfMonth != null) "auto" else "manual",
                        customDates = customDates,
                        requisites = requisites
                    )
                )
                if (resp.isSuccessful) _uiState.update { it.copy(isLoading = false, saved = true) }
                else _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка сохранения") }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    /**
     * Посуточно + переменный график: custom_dates графика = брони
     * (двусторонняя связь с шахматкой — шахматка красит даты, график их отдаёт).
     */
    fun saveVariableFromBookings(propertyId: String, requisites: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, saved = false) }
            try {
                val property = _uiState.value.property
                val rate = property?.rentAmount ?: 0.0
                val dd = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                // Слияние пересекающихся броней: сутки не задваиваются в суммах
                val merged = mergeRanges(_uiState.value.bookings)
                val customJson = if (merged.isEmpty()) null else com.google.gson.Gson().toJson(
                    merged.map { (s, e) ->
                        mapOf(
                            "date" to s.format(dd),
                            "amount" to (rate * (java.time.temporal.ChronoUnit.DAYS.between(s, e) + 1)).toString()
                        )
                    }
                )
                val resp = financeApi.createSchedule(
                    PaymentScheduleDto(
                        propertyId = propertyId,
                        type = "manual",
                        customDates = customJson,
                        requisites = requisites
                    )
                )
                if (resp.isSuccessful) _uiState.update { it.copy(isLoading = false, saved = true) }
                else _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка сохранения") }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    private suspend fun loadBookings(propertyId: String): List<Pair<LocalDate, LocalDate>> =
        try {
            bookingApi.getBookings(propertyId).body().orEmpty()
                .mapNotNull { b ->
                    val s = runCatching { LocalDate.parse(b.startDate) }.getOrNull()
                    val e = runCatching { LocalDate.parse(b.endDate) }.getOrNull()
                    if (s != null && e != null) s to e else null
                }
                .sortedBy { it.first }
        } catch (_: Exception) {
            emptyList()
        }
}
