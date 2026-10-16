package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.BookingApi
import com.rentmanager.app.data.api.CreateBookingRequest
import com.rentmanager.app.data.api.CreatePaymentRequest
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.local.BookingChangeEvents
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.local.TenantsListCache
import com.rentmanager.app.data.model.BookingDto
import com.rentmanager.app.data.model.PaymentDto
import com.rentmanager.app.data.model.PaymentScheduleDto
import com.rentmanager.app.data.model.forProperty
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.PropertyRepository
import com.rentmanager.app.util.PaymentOverdue
import com.rentmanager.app.util.parseAnyDate
import com.rentmanager.app.util.shortAddress
import com.rentmanager.app.util.mergeRanges
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Диапазон брони. source: app / avito / cian / manual. */
data class BookingRange(
    val id: String? = null,
    val start: LocalDate,
    val end: LocalDate,
    val source: String = "manual",
    /** Арендатор периода; null = покраска без арендатора (неразмеченный период) */
    val tenantId: String? = null,
    /**
     * Непогашенный долг внутри этого периода (сумма прошедших дат платежей без
     * оплаты). 0 = долга нет: красную обводку рисуем только там, где он есть.
     */
    val debtAmount: Double = 0.0
)

/** Арендатор для панели card_inf: имя и телефон по tenant_id брони */
data class TenantBrief(val name: String, val phone: String?)

@Immutable
data class MyPropertyItem(
    val id: String,
    val name: String,
    val address: String,
    val photoUrl: String? = null,
    val bookings: List<BookingRange> = emptyList(),
    val overdue: Boolean = false,
    /** Сумма просрочки — для ручного гашения с шахматки */
    val overdueAmount: Double = 0.0,
    val rentType: String = "посуточно",
    val rentAmount: Double? = null,
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue, // 1..12
    /** Режим отображения арендатора в списке: «Эля Цап · до 21.11.2026» */
    val tenantInfo: String? = null,
    /** Арендатор объекта — фолбэк, когда у брони нет tenant_id */
    val tenantId: String? = null,
    val tenantPhone: String? = null,
    /** Срок аренды из поля объекта — фолбэк, когда броней ещё нет */
    val rentEndDate: String? = null
)

/**
 * Подсказка «Добавить арендатора» для шахматки: объект + последний покрашенный
 * период. Живёт, пока у объекта есть покраски без арендатора, поэтому тапы по
 * ячейкам и диалоги отметки оплаты её не сбрасывают (раньше CTA исчезала вместе
 * с выбором ячейки).
 */
@Immutable
data class AttachSuggestion(
    val propertyId: String,
    val start: LocalDate,
    val end: LocalDate
)

/**
 * Последняя правка шахматки — для кнопки «Отменить» в снекбаре. Модель выбора
 * «первый тап — выделение, второй — действие» делает правку необратимой, а
 * случайный тап по занятому дню разрывает период арендатора. Поэтому каждое
 * действие обратимо одним тапом: снимок броней ДО правки + подпись.
 */
@Immutable
data class BookingEdit(
    val propertyId: String,
    /** Подпись снекбара: «07.10.2026 — снято» */
    val label: String,
    /** Брони объекта ДО правки */
    val before: List<BookingRange>
)

/**
 * Статус занятости на дату: попадание в диапазон брони — занято
 * («fullness»), а на занятом текущем месяце при просрочке — «expired»
 * (красная рамка: месяц арендуется, но платёж не получен).
 * Без брони ячейка всегда «free» — просрочка не может держать месяц
 * занятым, иначе его невозможно освободить.
 */
fun MyPropertyItem.statusAt(date: LocalDate): String {
    val booked = bookings.any { !date.isBefore(it.start) && !date.isAfter(it.end) }
    if (!booked) return "free"
    val today = LocalDate.now()
    // Просрочка — только у ПРОШЕДШИХ занятых дат: в посуточном режиме
    // красным должен быть неоплаченный прошлый день, а не весь текущий месяц
    return if (overdue && date.isBefore(today)) "expired"
    else "fullness"
}

@HiltViewModel
class MyPropertiesViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val bookingApi: BookingApi,
    private val financeApi: FinanceApi,
    private val tenantApi: TenantApi,
    private val tenantsListCache: TenantsListCache,
    private val detailCache: PropertyDetailCache,
    private val bookingChangeEvents: BookingChangeEvents
) : ViewModel() {
    private val _properties = MutableStateFlow<List<MyPropertyItem>>(emptyList())
    val properties: StateFlow<List<MyPropertyItem>> = _properties.asStateFlow()

    private val _tenantsById = MutableStateFlow<Map<String, TenantBrief>>(emptyMap())

    /** Справочник арендаторов для панели card_inf (имя и телефон по tenant_id брони) */
    val tenantsById: StateFlow<Map<String, TenantBrief>> = _tenantsById.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Закрытие окна «Нет связи с сервером» тапом вне */
    fun clearError() {
        _errorMessage.value = null
    }

    private val _attachSuggestion = MutableStateFlow<AttachSuggestion?>(null)

    /**
     * Подсказка прикрепления арендатора для шахматки. Считается по данным:
     * пока у объекта есть покраски и нет арендатора — CTA на месте; арендатор
     * прикреплён или покраски сняты — подсказка снимается.
     */
    val attachSuggestion: StateFlow<AttachSuggestion?> = _attachSuggestion.asStateFlow()

    private val _lastEdit = MutableStateFlow<BookingEdit?>(null)

    /** Последняя правка шахматки — для кнопки «Отменить» в снекбаре */
    val lastEdit: StateFlow<BookingEdit?> = _lastEdit.asStateFlow()

    /** Снекбар ушёл — правку больше не откатить */
    fun dismissEdit() {
        _lastEdit.value = null
    }

    /** Подпись правки для снекбара: «07.10.2026 — снято» */
    private fun editLabel(start: LocalDate, end: LocalDate, verb: String): String {
        val dd = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val from = start.format(dd)
        return if (start == end) "$from — $verb" else "$from–${end.format(dd)} — $verb"
    }

    /**
     * Откат последней правки: состояние броней объекта возвращаем к снимку ДО
     * неё. Серверного restore для броней нет, поэтому появившиеся брони убираем,
     * пропавшие пересоздаём С ТЕМ ЖЕ арендатором (стирание обязано сохранять
     * арендатора), а изменённые (сервер мог склеить период с соседним) — правим
     * обратно по границам снимка.
     */
    fun undoLastEdit() {
        val edit = _lastEdit.value ?: return
        _lastEdit.value = null
        viewModelScope.launch {
            try {
                val current = bookingApi.getBookings(edit.propertyId).body().orEmpty()
                    .map { it.toBookingRange() }
                val beforeIds = edit.before.mapNotNull { it.id }.toSet()
                val currentById = current.mapNotNull { b -> b.id?.let { it to b } }.toMap()
                // 1) появившееся после правки — убираем
                current.filter { it.id != null && it.id !in beforeIds }.forEach { b ->
                    bookingApi.deleteBooking(edit.propertyId, b.id!!)
                }
                // 2) пропавшее — возвращаем с его арендатором
                edit.before.filter { it.id == null || it.id !in currentById.keys }.forEach { b ->
                    runCatching {
                        bookingApi.createBooking(
                            edit.propertyId,
                            CreateBookingRequest(
                                b.start.toString(),
                                b.end.toString(),
                                tenantId = b.tenantId
                            )
                        )
                    }
                }
                // 3) изменённое — возвращаем границы из снимка
                edit.before.forEach { b ->
                    val id = b.id ?: return@forEach
                    val now = currentById[id] ?: return@forEach
                    if (now.start != b.start || now.end != b.end) {
                        runCatching {
                            bookingApi.updateBooking(
                                edit.propertyId,
                                id,
                                CreateBookingRequest(
                                    b.start.toString(),
                                    b.end.toString(),
                                    tenantId = b.tenantId
                                )
                            )
                        }
                    }
                }
                syncAfterBookingChange(edit.propertyId)
            } catch (_: Exception) {
            }
            refresh()
        }
    }

    init {
        refresh()
        // Арендатора прикрепили на экране прикрепления — дорисовываем период
        // сразу (BookingChangeEvents), не ожидая ON_RESUME-refresh
        viewModelScope.launch {
            bookingChangeEvents.added.collect { onBookingAdded(it) }
        }
    }

    /**
     * Арендатор прикреплён на другом экране: период уже занят, но брони с
     * сервера приедут только после refresh. Дорисовываем её у себя — шахматка
     * перекрашивается мгновенно — и сразу сверяемся с сервером: он склеивает
     * периоды и проставляет арендатора по своим правилам.
     */
    private fun onBookingAdded(event: BookingChangeEvents.BookingAdded) {
        val updated = _properties.value.map { item ->
            if (item.id != event.propertyId) {
                item
            } else {
                // Покраска без арендатора внутри периода стала арендой: подсказка
                // «Добавить арендатора» гаснет сразу, а не после ответа сервера
                val kept = item.bookings.filterNot {
                    it.tenantId == null &&
                        !it.start.isAfter(event.end) && !it.end.isBefore(event.start)
                }
                item.copy(
                    bookings = kept + BookingRange(
                        start = event.start,
                        end = event.end,
                        tenantId = event.tenantId
                    )
                )
            }
        }
        _properties.value = updated
        validateAttachSuggestion(updated)
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
                    validateAttachSuggestion(_properties.value)
                    loadTenants()
                } else {
                    _errorMessage.value = "Ошибка загрузки"
                }
            } catch (_: Exception) {
                _errorMessage.value = "Нет связи с сервером"
            }
        }
    }

    /**
     * Подсказка-кнопка НЕ воскрешается сама: по макету (заметка 2950:45816) «кнопка
     * появляется при выборе периода». Ставится при покраске, переживает refresh и
     * диалоги оплаты, а гаснет, когда период размечен (арендатор прикреплён) или
     * покраску сняли — либо при новом выборе (clearAttachSuggestion).
     */
    private fun validateAttachSuggestion(properties: List<MyPropertyItem>) {
        val current = _attachSuggestion.value ?: return
        val item = properties.find { it.id == current.propertyId }
        val last = item?.bookings
            ?.filter { it.tenantId == null }
            ?.maxByOrNull { it.end }
        _attachSuggestion.value = if (item != null && last != null) {
            current.copy(start = last.start, end = last.end)
        } else {
            null
        }
    }

    /** Новый выбор в шахматке сбрасывает кнопку-подсказку (как в макете). */
    fun clearAttachSuggestion() {
        _attachSuggestion.value = null
    }

    fun saveBooking(propertyId: String, start: LocalDate, end: LocalDate) {
        // Прошлое не бронируем: старые дни нельзя ни выбрать, ни сохранить
        if (start.isBefore(LocalDate.now())) {
            _errorMessage.value = "Прошедшие даты забронировать нельзя"
            return
        }
        // Снимок ДО правки — для «Отменить» в снекбаре
        val before = _properties.value.find { it.id == propertyId }?.bookings.orEmpty()
        viewModelScope.launch {
            try {
                val resp = bookingApi.createBooking(
                    propertyId,
                    CreateBookingRequest(start.toString(), end.toString())
                )
                if (resp.isSuccessful) {
                    // Подсказку прикрепления держим до привязки арендатора:
                    // её больше не сбрасывают тапы по ячейкам
                    _attachSuggestion.value = AttachSuggestion(propertyId, start, end)
                    _lastEdit.value = BookingEdit(propertyId, editLabel(start, end, "отмечено"), before)
                    syncAfterBookingChange(propertyId)
                    refresh()
                } else {
                    // Раньше отказ сервера проглатывался — выглядело как
                    // «клетка не перекрашивается»
                    _errorMessage.value = "Не удалось сохранить период: " +
                        when (resp.code()) {
                            409 -> "у арендатора уже есть бронь на эти даты"
                            else -> "ошибка ${resp.code()}"
                        }
                }
            } catch (_: Exception) { }
        }
    }

    fun deleteBookings(propertyId: String, start: LocalDate, end: LocalDate) {
        // Снимок ДО правки — для «Отменить» в снекбаре
        val before = _properties.value.find { it.id == propertyId }?.bookings.orEmpty()
        viewModelScope.launch {
            try {
                val item = _properties.value.find { it.id == propertyId } ?: return@launch
                val toDelete = item.bookings.filter { b ->
                    b.id != null && !start.isAfter(b.end) && !end.isBefore(b.start)
                }
                // Пересечения нет (бронь уже снята) — всё равно синкаем график
                // с бронями: без этого старые платежи custom_dates оставались
                // в графике навсегда и «долг не убирался»
                if (toDelete.isEmpty()) {
                    syncAfterBookingChange(propertyId)
                    refresh()
                    return@launch
                }
                for (b in toDelete) {
                    val del = bookingApi.deleteBooking(propertyId, b.id!!)
                    if (!del.isSuccessful) {
                        _errorMessage.value = "Не удалось снять период (${del.code()})"
                        break
                    }
                    _lastEdit.value = BookingEdit(propertyId, editLabel(start, end, "снято"), before)
                    // Вырезаем только выделенный диапазон: нераскрашенные
                    // остатки брони пересоздаём, иначе снятие одного месяца
                    // сносило бы всю многомесячную бронь целиком
                    // Остатки сохраняют арендатора исходного периода: иначе при
                    // стирании части периода связь с арендатором терялась
                    if (b.start.isBefore(start)) {
                        runCatching {
                            bookingApi.createBooking(
                                propertyId,
                                CreateBookingRequest(b.start.toString(), start.minusDays(1).toString(), tenantId = b.tenantId)
                            )
                        }
                    }
                    if (b.end.isAfter(end)) {
                        runCatching {
                            bookingApi.createBooking(
                                propertyId,
                                CreateBookingRequest(end.plusDays(1).toString(), b.end.toString(), tenantId = b.tenantId)
                            )
                        }
                    }
                }
                syncAfterBookingChange(propertyId)
                refresh()
            } catch (_: Exception) { }
        }
    }

    /**
     * Синхронизация после ручной покраски шахматки. Поведение — по типу графика:
     * - постоянный (месяцы): «Срок аренды» = последний зелёный месяц + расчётная дата
     *   (7-е число, покрашено сен–дек → «до 07.12.гггг»);
     * - переменный (сутки): «Срок аренды» = последняя дата брони,
     *   custom_dates графика = брони (ставка × сутки, слитые периоды).
     * Брони сняты → поле очищается.
     */
    private suspend fun syncAfterBookingChange(propertyId: String) {
        try {
            val dto = propertyRepository.getProperties().body()?.firstOrNull { it.id == propertyId }
                ?: return
            val bookings = bookingApi.getBookings(propertyId).body().orEmpty()
                .map { it.toBookingRange() }
                .sortedBy { it.start }
            val schedule = financeApi.getSchedules().body()?.forProperty(propertyId)
            // Семантика «брони = посуточные платежи (ставка × сутки)» — только
            // для посуточных объектов. Длительная аренда: покраска месяцев — это
            // срок аренды; график из броней не пересобирается, иначе месячная
            // ставка превращается в «ставку за сутки» × число дней (30 000 × 92)
            val scheduleType = if ((dto.rentType ?: "посуточно") == "длительно") "auto"
            else (schedule?.type ?: "manual")
            val dd = DateTimeFormatter.ofPattern("dd.MM.yyyy")

            // «Срок аренды» — всегда конец аренды (последняя бронь), а не дата
            // следующего платежа: карточка объекта показывает срок договора
            val newRentEnd = bookings.maxByOrNull { it.end }?.end?.format(dd)
            if (dto.rentEndDate != newRentEnd) {
                val updated = dto.copy(rentEndDate = newRentEnd)
                propertyRepository.updateProperty(propertyId, updated)
                runCatching { detailCache.saveProperty(updated) }
            }
            if (scheduleType != "auto") {
                // Даты платежей = брони, сгруппированные ПО АРЕНДАТОРУ: периоды
                // разных арендаторов не сливаются (иначе в одну запись графика
                // попадают чужие сутки и «долг» объекта суммирует арендаторов),
                // а внутри группы пересечение не задваивает сутки.
                // Сумма записи = ставка × сутки периода; день передачи
                // (один выехал, другой заехал) считают оба.
                val rate = dto.rentAmount ?: 0.0
                val merged = bookings.groupBy { it.tenantId ?: "" }
                    .flatMap { (_, group) -> mergeRanges(group.map { it.start to it.end }) }
                    .sortedBy { it.first }
                val customJson = if (merged.isEmpty()) null else Gson().toJson(
                    merged.map { (s, e) ->
                        mapOf(
                            "date" to s.format(dd),
                            "amount" to (rate * (java.time.temporal.ChronoUnit.DAYS.between(s, e) + 1)).toString()
                        )
                    }
                )
                financeApi.createSchedule(
                    PaymentScheduleDto(
                        propertyId = propertyId,
                        dayOfMonth = null,
                        amount = null,
                        type = "manual",
                        customDates = customJson,
                        requisites = schedule?.requisites
                    )
                )
            }
        } catch (_: Exception) { }
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

    /** Справочник арендаторов: сначала кэш (мгновенный кадр), затем сервер. */
    private suspend fun loadTenants() {
        try {
            val cached = tenantsListCache.get().orEmpty()
            if (cached.isNotEmpty()) {
                _tenantsById.value = cached.associate {
                    it.id to TenantBrief(it.fullName, it.phone.takeIf { p -> p.isNotBlank() })
                }
            }
            val resp = tenantApi.getTenants()
            if (resp.isSuccessful) {
                val list = resp.body().orEmpty()
                tenantsListCache.put(list)
                _tenantsById.value = list.associate {
                    it.id to TenantBrief(it.fullName, it.phone.takeIf { p -> p.isNotBlank() })
                }
            }
        } catch (_: Exception) { }
    }

    /**
     * Долг и просрочка считаются по ДАТАМ ПЛАТЕЖЕЙ, а не «в целом по объекту»:
     * у каждого периода своя сумма, поэтому карточка периода показывает только
     * его долг (новый будущий период — 0, а не долг всего объекта).
     */
    private suspend fun loadOverdue(item: MyPropertyItem, schedules: List<PaymentScheduleDto>): MyPropertyItem =
        try {
            val schedule = schedules.forProperty(item.id)
            val payments = financeApi.listPayments(item.id).body() ?: emptyList()
            val today = LocalDate.now()
            val unpaidPast = unpaidDueDates(item.bookings, schedule, payments)
                .filter { it.first.isBefore(today) }
            item.copy(
                overdue = unpaidPast.isNotEmpty(),
                overdueAmount = unpaidPast.sumOf { it.second },
                bookings = item.bookings.map { b ->
                    val debt = unpaidPast
                        .filter { (d, _) -> !d.isBefore(b.start) && !d.isAfter(b.end) }
                        .sumOf { it.second }
                    if (debt > 0.0) b.copy(debtAmount = debt) else b
                }
            )
        } catch (_: Exception) {
            item
        }

    /**
     * Даты платежей объекта. Помесячно — от начала первой аренды до конца
     * последней по дню из графика (аренда продлилась — даты появились сами,
     * график руками править не нужно); посуточно — даты переменного графика,
     * которые пересобираются из броней при каждой правке.
     */
    private fun dueDatesOf(
        bookings: List<BookingRange>,
        schedule: PaymentScheduleDto?
    ): List<Pair<LocalDate, Double>> {
        val s = schedule ?: return emptyList()
        s.dayOfMonth?.let { day ->
            val first = bookings.minOfOrNull { it.start } ?: return emptyList()
            val last = bookings.maxOfOrNull { it.end } ?: return emptyList()
            val amount = s.amount ?: 0.0
            val out = ArrayList<Pair<LocalDate, Double>>()
            var month = YearMonth.from(first)
            val endMonth = YearMonth.from(last)
            while (!month.isAfter(endMonth)) {
                val date = month.atDay(day.coerceIn(1, month.lengthOfMonth()))
                if (!date.isBefore(first) && !date.isAfter(last)) out += date to amount
                month = month.plusMonths(1)
            }
            return out
        }
        s.customDates?.let { return PaymentOverdue.dueDatesFromCustom(it) }
        return emptyList()
    }

    /** Непогашенное: дата прошла (или предстоит), а платежа «paid» на неё и позже нет. */
    private fun unpaidDueDates(
        bookings: List<BookingRange>,
        schedule: PaymentScheduleDto?,
        payments: List<PaymentDto>
    ): List<Pair<LocalDate, Double>> {
        val paid = payments.filter { it.status == "paid" }.mapNotNull { parseAnyDate(it.date) }
        return dueDatesOf(bookings, schedule).filter { (due, _) -> paid.none { !it.isBefore(due) } }
    }

    /**
     * Ручное гашение просрочки с шахматки (долгое нажатие на красной ячейке):
     * арендатор мог заплатить наличными или вне приложения, а код ждёт
     * подтверждения. Записываем платёж со статусом «paid» и сегодняшней
     * датой — просрочка гаснет в шахматке, на дашборде и в карточке.
     */
    fun markOverduePaid(propertyId: String, amount: Double = -1.0) {
        viewModelScope.launch {
            try {
                val item = _properties.value.find { it.id == propertyId } ?: return@launch
                val sum = if (amount > 0.0) amount else item.overdueAmount
                financeApi.createPayment(propertyId, CreatePaymentRequest(sum))
                refresh()
            } catch (_: Exception) { }
        }
    }
}

private fun PropertyDto.toMyPropertyItem(): MyPropertyItem = MyPropertyItem(
    id = id,
    name = name,
    // Макет «Моя недвижимость»: короткий адрес — улица и номер дома (первые 2 сегмента, без города/области/страны)
    address = address.shortAddress(),
    photoUrl = photos?.firstOrNull()?.url,
    rentType = rentType ?: "посуточно",
    rentAmount = rentAmount,
    tenantInfo = tenantInfo?.takeIf { it.isNotBlank() },
    tenantId = tenantId,
    tenantPhone = phone?.takeIf { it.isNotBlank() },
    rentEndDate = rentEndDate
)


private fun BookingDto.toBookingRange(): BookingRange = BookingRange(
    id = id,
    start = LocalDate.parse(startDate),
    end = LocalDate.parse(endDate),
    source = source,
    tenantId = tenantId
)
