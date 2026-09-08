package com.rentmanager.app.ui.landlord.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.BookingApi
import com.rentmanager.app.data.api.CreateBookingRequest
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.model.PaymentRequisiteDto
import com.rentmanager.app.data.model.PaymentScheduleDto
import com.rentmanager.app.data.model.forProperty
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PaymentScheduleUiState(
    val isLoading: Boolean = true,
    val schedule: PaymentScheduleDto? = null,
    val property: PropertyDto? = null,
    /** Брони объекта — источник дат переменного графика посуточной аренды */
    val bookings: List<Pair<LocalDate, LocalDate>> = emptyList(),
    /** Реквизиты арендодателя (общие на аккаунт) */
    val requisites: List<PaymentRequisiteDto> = emptyList(),
    val errorMessage: String? = null
) {
    val isDailyRent: Boolean
        get() = (property?.rentType ?: "посуточно") == "посуточно"
}

/** Одноразовые события экрана (применение графика, ошибки, реквизиты). */
sealed interface ScheduleEvent {
    /** График успешно применён (обновляем состояние из ответа) */
    data class Applied(val schedule: PaymentScheduleDto) : ScheduleEvent
    /** «Не удалось применить график. Повторите еще раз» */
    object ApplyFailed : ScheduleEvent
    /** Реквизит создан */
    object RequisiteCreated : ScheduleEvent
}

@HiltViewModel
class PaymentScheduleViewModel @Inject constructor(
    private val financeApi: FinanceApi,
    private val bookingApi: BookingApi,
    private val propertyRepository: PropertyRepository,
    private val detailCache: PropertyDetailCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentScheduleUiState())
    val uiState: StateFlow<PaymentScheduleUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ScheduleEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ScheduleEvent> = _events.asSharedFlow()

    fun load(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val schedules = financeApi.getSchedules().body().orEmpty()
                val schedule = schedules.forProperty(propertyId)
                val property = detailCache.load(propertyId)?.property ?: runCatching {
                    propertyRepository.getProperties().body()?.firstOrNull { it.id == propertyId }
                }.getOrNull()
                val bookings = loadBookings(propertyId)
                val requisites = runCatching {
                    financeApi.getRequisites().body().orEmpty()
                }.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        schedule = schedule,
                        property = property,
                        bookings = bookings,
                        requisites = requisites
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    /** Посуточно + переменный график: добавление даты = бронь этих суток.
     *  Дата уже занята (есть в любой брони) — бронь не создаём. */
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

    /** Undo удаления строки посуточного графика: пересоздаёт брони по диапазонам. */
    fun recreateBookings(propertyId: String, ranges: List<Pair<LocalDate, LocalDate>>) {
        viewModelScope.launch {
            ranges.forEach { (s, e) ->
                runCatching {
                    bookingApi.createBooking(propertyId, CreateBookingRequest(s.toString(), e.toString()))
                }
            }
            _uiState.update { it.copy(bookings = loadBookings(propertyId)) }
        }
    }

    fun saveFixed(propertyId: String, dayOfMonth: Int, amount: Double, requisiteId: String?) {
        pushSchedule(
            PaymentScheduleDto(
                propertyId = propertyId,
                dayOfMonth = dayOfMonth,
                amount = amount,
                type = "auto",
                requisites = requisiteId
            )
        )
    }

    fun saveVariableManual(propertyId: String, payments: List<VariablePayment>, requisiteId: String?) {
        val dd = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val customJson = if (payments.isEmpty()) null else com.google.gson.Gson().toJson(
            payments.map { mapOf("date" to it.date.format(dd), "amount" to it.amount) }
        )
        pushSchedule(
            PaymentScheduleDto(
                propertyId = propertyId,
                type = "manual",
                customDates = customJson,
                requisites = requisiteId
            )
        )
    }

    /** Отмена графика: сохраняем пустую запись — все поля пусты,
     *  клиент видит «график не настроен». */
    fun cancelSchedule(propertyId: String) {
        pushSchedule(
            PaymentScheduleDto(
                propertyId = propertyId,
                type = if (_uiState.value.isDailyRent) "manual" else "auto",
                dayOfMonth = null,
                amount = null,
                customDates = null,
                requisites = null
            )
        )
    }

    fun createRequisite(name: String, account: String, bank: String) {
        viewModelScope.launch {
            try {
                val resp = financeApi.createRequisite(
                    PaymentRequisiteDto(name = name, account = account, bank = bank)
                )
                val created = resp.body()
                if (resp.isSuccessful && created != null) {
                    _uiState.update { it.copy(requisites = it.requisites + created) }
                    _events.tryEmit(ScheduleEvent.RequisiteCreated)
                }
            } catch (_: Exception) { }
        }
    }

    private fun pushSchedule(dto: PaymentScheduleDto) {
        viewModelScope.launch {
            try {
                val resp = financeApi.createSchedule(dto)
                val body = resp.body()
                if (resp.isSuccessful && body != null) {
                    _uiState.update { it.copy(schedule = body) }
                    _events.tryEmit(ScheduleEvent.Applied(body))
                } else {
                    _events.tryEmit(ScheduleEvent.ApplyFailed)
                }
            } catch (_: Exception) {
                _events.tryEmit(ScheduleEvent.ApplyFailed)
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
