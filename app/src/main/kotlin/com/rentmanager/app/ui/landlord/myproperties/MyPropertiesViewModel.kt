package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.BookingApi
import com.rentmanager.app.data.api.CreateBookingRequest
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.model.BookingDto
import com.rentmanager.app.data.model.PaymentScheduleDto
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.PropertyRepository
import com.rentmanager.app.util.PaymentOverdue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Диапазон брони. source: app / avito / cian / manual. */
data class BookingRange(
    val id: String? = null,
    val start: LocalDate,
    val end: LocalDate,
    val source: String = "manual"
)

@Immutable
data class MyPropertyItem(
    val id: String,
    val name: String,
    val address: String,
    val photoUrl: String? = null,
    val bookings: List<BookingRange> = emptyList(),
    val overdue: Boolean = false,
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue // 1..12
)

/**
 * Статус занятости на дату: по умолчанию свободно,
 * попадание в диапазон брони — занято, просрочка — expired.
 */
fun MyPropertyItem.statusAt(date: LocalDate): String {
    if (bookings.any { !date.isBefore(it.start) && !date.isAfter(it.end) }) return "fullness"
    val today = LocalDate.now()
    if (overdue && date.year == today.year && date.monthValue == today.monthValue) return "expired"
    return "free"
}

enum class ViewMode { MONTHS, DAYS }

enum class DisplayMode { CARDS, TABLE }

@HiltViewModel
class MyPropertiesViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val bookingApi: BookingApi,
    private val financeApi: FinanceApi,
    private val detailCache: PropertyDetailCache
) : ViewModel() {
    private val _properties = MutableStateFlow<List<MyPropertyItem>>(emptyList())
    val properties: StateFlow<List<MyPropertyItem>> = _properties.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.MONTHS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _displayMode = MutableStateFlow(DisplayMode.CARDS)
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val resp = propertyRepository.getProperties()
                if (resp.isSuccessful) {
                    val dtos = resp.body()!!
                    dtos.forEach { runCatching { detailCache.saveProperty(it) } }
                    // Сохраняем выбранный период каждого объекта, чтобы refresh не сбрасывал его на текущий год/месяц
                    val previous = _properties.value.associateBy { it.id }
                    val items = dtos.map { dto ->
                        val item = dto.toMyPropertyItem()
                        val prev = previous[dto.id]
                        if (prev != null) item.copy(year = prev.year, month = prev.month) else item
                    }
                    val schedules = loadSchedules()
                    val withBookings = items.map { loadBookings(it) }
                    _properties.value = withBookings.map { loadOverdue(it, schedules) }
                } else {
                    _errorMessage.value = "Ошибка загрузки"
                }
            } catch (_: Exception) {
                _errorMessage.value = "Нет связи с сервером"
            }
        }
    }

    fun saveBooking(propertyId: String, start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            try {
                val resp = bookingApi.createBooking(
                    propertyId,
                    CreateBookingRequest(start.toString(), end.toString())
                )
                if (resp.isSuccessful) refresh()
            } catch (_: Exception) { }
        }
    }

    fun deleteBookings(propertyId: String, start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            try {
                val item = _properties.value.find { it.id == propertyId } ?: return@launch
                val toDelete = item.bookings.filter { b ->
                    b.id != null && !start.isAfter(b.end) && !end.isBefore(b.start)
                }
                if (toDelete.isEmpty()) return@launch
                for (b in toDelete) {
                    bookingApi.deleteBooking(propertyId, b.id!!)
                }
                refresh()
            } catch (_: Exception) { }
        }
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
    }

    fun selectPeriod(propertyId: String, year: Int, month: Int) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(year = year, month = month) else it
        }.toMutableList()
    }

    private suspend fun loadBookings(item: MyPropertyItem): MyPropertyItem =
        try {
            val resp = bookingApi.getBookings(item.id)
            if (resp.isSuccessful) {
                item.copy(bookings = resp.body()!!.map { it.toBookingRange() })
            } else {
                item
            }
        } catch (_: Exception) {
            item
        }

    private suspend fun loadSchedules(): List<PaymentScheduleDto> =
        try {
            financeApi.getSchedules().body() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

    private suspend fun loadOverdue(item: MyPropertyItem, schedules: List<PaymentScheduleDto>): MyPropertyItem =
        try {
            val schedule = schedules.firstOrNull { it.propertyId == item.id }
            val payments = financeApi.listPayments(item.id).body() ?: emptyList()
            item.copy(overdue = PaymentOverdue.isOverdue(schedule, payments))
        } catch (_: Exception) {
            item
        }
}

private fun PropertyDto.toMyPropertyItem(): MyPropertyItem = MyPropertyItem(
    id = id,
    name = name,
    address = address,
    photoUrl = photos?.firstOrNull()?.url
)

private fun BookingDto.toBookingRange(): BookingRange = BookingRange(
    id = id,
    start = LocalDate.parse(startDate),
    end = LocalDate.parse(endDate),
    source = source
)
