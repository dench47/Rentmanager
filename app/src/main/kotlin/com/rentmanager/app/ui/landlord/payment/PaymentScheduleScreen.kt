package com.rentmanager.app.ui.landlord.payment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.DesignToast
import com.rentmanager.app.ui.components.DesignToastData
import com.rentmanager.app.ui.components.DesignWidthDialog
import com.rentmanager.app.ui.components.DayOfMonthPickerSheet
import com.rentmanager.app.ui.components.DatePickerSheet
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.landlord.createproperty.ScreenToolbar
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardShape
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobPlaceholderStyle
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.util.mergeRanges
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---- Черновик экрана: переживает уход и возврат на экран ----
object PaymentScheduleCache {
    var typeIsFixed: Boolean = true
    var fixedDay: Int? = null
    var fixedAmount: String = ""
    var variableDate: LocalDate? = null
    var variableAmount: String = ""
    val payments: MutableList<VariablePayment> = mutableListOf()
    var requisiteId: String? = null
}

data class VariablePayment(
    val date: LocalDate,
    val amount: String
)

/** Строка платежа посуточной аренды (из броней): период + сумма (ставка × сутки) */
private data class BookingRow(val start: LocalDate, val end: LocalDate, val amount: Double)

private val RuDateFormat = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
private val DdMmYyyy = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun PaymentScheduleScreen(
    propertyId: String,
    onBack: () -> Unit,
    viewModel: PaymentScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ---- Состояние экрана ----
    var typeIsFixed by remember { mutableStateOf(PaymentScheduleCache.typeIsFixed) }
    var scheduleActive by remember { mutableStateOf(false) } // на сервере есть непустой график
    var editing by remember { mutableStateOf(false) }        // режим правки активного графика

    var fixedDay by remember { mutableStateOf(PaymentScheduleCache.fixedDay) }
    var fixedAmount by remember { mutableStateOf(PaymentScheduleCache.fixedAmount) }
    var fixedDayError by remember { mutableStateOf<String?>(null) }
    var fixedAmountError by remember { mutableStateOf<String?>(null) }

    var variableDate by remember { mutableStateOf(PaymentScheduleCache.variableDate) }
    var variableAmount by remember { mutableStateOf(PaymentScheduleCache.variableAmount) }
    var variableDateError by remember { mutableStateOf<String?>(null) }
    var variableAmountError by remember { mutableStateOf<String?>(null) }
    val payments = remember {
        mutableStateListOf<VariablePayment>().also { it.addAll(PaymentScheduleCache.payments) }
    }

    var requisiteId by remember { mutableStateOf(PaymentScheduleCache.requisiteId) }

    var showCalendar by remember { mutableStateOf(false) }
    var showDaySheet by remember { mutableStateOf(false) }
    var showRequisites by remember { mutableStateOf(false) }
    var showCreateRequisite by remember { mutableStateOf(false) }
    var conflictDate by remember { mutableStateOf<LocalDate?>(null) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<DesignToastData?>(null) }
    var scheduleConsumed by remember { mutableStateOf(false) }

    fun persist() {
        PaymentScheduleCache.typeIsFixed = typeIsFixed
        PaymentScheduleCache.fixedDay = fixedDay
        PaymentScheduleCache.fixedAmount = fixedAmount
        PaymentScheduleCache.variableDate = variableDate
        PaymentScheduleCache.variableAmount = variableAmount
        PaymentScheduleCache.payments.clear()
        PaymentScheduleCache.payments.addAll(payments)
        PaymentScheduleCache.requisiteId = requisiteId
    }

    fun resetDraft() {
        fixedDay = null
        fixedAmount = ""
        fixedDayError = null
        fixedAmountError = null
        variableDate = null
        variableAmount = ""
        variableDateError = null
        variableAmountError = null
        payments.clear()
        requisiteId = null
        persist()
    }

    LaunchedEffect(propertyId) { viewModel.load(propertyId) }

    // Первичная загрузка графика → черновик
    LaunchedEffect(uiState.isLoading, uiState.schedule, uiState.property) {
        if (uiState.isLoading || scheduleConsumed) return@LaunchedEffect
        val s = uiState.schedule
        val hasFixed = s?.dayOfMonth != null
        val hasVariable = s?.customDates != null
        scheduleActive = hasFixed || hasVariable
        if (scheduleActive) {
            editing = false
            typeIsFixed = hasFixed
            if (hasFixed) {
                payments.clear()
                fixedDay = s?.dayOfMonth
                fixedAmount = s?.amount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
            } else {
                // Посуточно: список дат = брони; ручной список — только для длительной аренды
                if (!uiState.isDailyRent) {
                    payments.clear()
                    payments.addAll(parseCustomDates(s?.customDates))
                }
            }
            if (requisiteId == null) requisiteId = s?.requisites
        }
        scheduleConsumed = true
        persist()
    }

    // Одноразовые события (применение графика, ошибки, создание реквизита)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScheduleEvent.Applied -> {
                    val s = event.schedule
                    val hasData = s.dayOfMonth != null || s.customDates != null
                    scheduleActive = hasData
                    if (hasData) {
                        editing = false
                        typeIsFixed = s.dayOfMonth != null
                        if (typeIsFixed) payments.clear()
                    } else {
                        resetDraft()
                    }
                    persist()
                }
                ScheduleEvent.ApplyFailed -> toast = DesignToastData(
                    text = "Не удалось применить график. Повторите еще раз",
                    iconRes = R.drawable.ic_globe_warning
                )
                ScheduleEvent.RequisiteCreated -> {
                    showCreateRequisite = false
                    requisiteId = viewModel.uiState.value.requisites.lastOrNull()?.id
                    persist()
                }
            }
        }
    }

    // Ошибка загрузки — тем же тостом дизайнера
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toast = DesignToastData(text = it, iconRes = R.drawable.ic_globe_warning)
        }
    }

    val isDailyRent = uiState.isDailyRent
    val readOnly = scheduleActive && !editing
    val requisite = uiState.requisites.firstOrNull { it.id == requisiteId }

    // Строки переменного графика: посуточно — брони, иначе ручной список
    val bookingRows = remember(uiState.bookings, uiState.property?.rentAmount) {
        if (!isDailyRent) emptyList()
        else mergeRanges(uiState.bookings).map { (s, e) ->
            val nights = java.time.temporal.ChronoUnit.DAYS.between(s, e) + 1
            BookingRow(s, e, (uiState.property?.rentAmount ?: 0.0) * nights)
        }
    }

    fun validateFixed(): Boolean {
        fixedDayError = if (fixedDay == null || fixedDay !in 1..31) "Выберите значение" else null
        val amount = fixedAmount.replace(" ", "").toDoubleOrNull()
        fixedAmountError = if (amount == null || amount <= 0.0) "Заполните поле" else null
        return fixedDayError == null && fixedAmountError == null
    }

    fun applySchedule() {
        if (typeIsFixed) {
            if (!validateFixed()) return
            viewModel.saveFixed(
                propertyId, fixedDay!!, fixedAmount.replace(" ", "").toDouble(), requisiteId
            )
        } else {
            if (isDailyRent) viewModel.saveVariableFromBookings(propertyId, requisiteId)
            else viewModel.saveVariableManual(propertyId, payments.toList(), requisiteId)
        }
    }

    fun addPayment() {
        // Заметка дизайнера (Figma 2872:34854): нажатие «Добавить платеж»
        // подчёркивает незаполненные поля; платёж создаётся только когда всё заполнено
        var valid = true
        val date = variableDate
        if (date == null) {
            variableDateError = "Выберите значение"
            valid = false
        }
        val amount = variableAmount.replace(" ", "").toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            variableAmountError = "Заполните поле"
            valid = false
        }
        if (!valid) return
        val paymentDate = date ?: return

        if (isDailyRent) {
            val busy = uiState.bookings.any { (s, e) -> !paymentDate.isBefore(s) && !paymentDate.isAfter(e) }
            if (busy) {
                conflictDate = paymentDate
                return
            }
            viewModel.addBookingDay(propertyId, paymentDate) { added ->
                if (added) {
                    variableDate = null
                    variableAmount = ""
                    variableDateError = null
                    variableAmountError = null
                    toast = DesignToastData(
                        text = "Платёж на ${paymentDate.format(RuDateFormat)} добавлен",
                        iconRes = R.drawable.ic_check_white
                    )
                }
            }
        } else {
            if (payments.any { it.date == paymentDate }) {
                conflictDate = paymentDate
                return
            }
            payments.add(VariablePayment(paymentDate, variableAmount.replace(" ", "")))
            payments.sortBy { it.date }
            variableDate = null
            variableAmount = ""
            variableDateError = null
            variableAmountError = null
            toast = DesignToastData(
                text = "Платёж на ${paymentDate.format(RuDateFormat)} добавлен",
                iconRes = R.drawable.ic_check_white
            )
        }
        persist()
    }

    fun deletePayment(index: Int) {
        if (isDailyRent) {
            val row = bookingRows.getOrNull(index) ?: return
            viewModel.deleteBooking(propertyId, row.start, row.end)
            toast = DesignToastData(
                text = "Платёж был удален",
                iconRes = R.drawable.ic_check_white,
                actionText = "Отменить удаление",
                onAction = { viewModel.recreateBookings(propertyId, listOf(row.start to row.end)) }
            )
        } else {
            val snapshot = payments.toList()
            payments.removeAt(index)
            persist()
            toast = DesignToastData(
                text = "Платёж был удален",
                iconRes = R.drawable.ic_check_white,
                actionText = "Отменить удаление",
                onAction = {
                    payments.clear()
                    payments.addAll(snapshot)
                    persist()
                }
            )
        }
    }

    // ---- Валидность верхней кнопки ----
    val primaryEnabled = if (typeIsFixed) {
        fixedDay != null && (fixedAmount.replace(" ", "").toDoubleOrNull() ?: 0.0) > 0.0
    } else {
        if (isDailyRent) bookingRows.isNotEmpty() else payments.isNotEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Белая страница — как карточка объекта: серые блоки r20 (#EFEFEF)
            // на белом читаются чётко, в отличие от фона #F5F5F5
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ---- Toolbar: стрелка + название слева, кликабельно вместе ----
            // В макете (2872-34102) статус-бар высокий (53dp): зазор от часов до
            // названия заметно больше, чем даёт системный статус-бар MIUI —
            // добираем воздух сверху
            Spacer(Modifier.height(10.dp))
            ScreenToolbar(title = "График платежей", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // ---- Тип платежей: лейбл через 12dp после тулбара (Figma: label y115, toolbar до y103)
                Spacer(Modifier.height(12.dp))
                Text("Тип платежей", style = Headline2MobStyle)
                Spacer(Modifier.height(6.dp))
                PaymentTypeSegment(
                    fixedSelected = typeIsFixed,
                    onSelect = {
                        typeIsFixed = it
                        if (scheduleActive) editing = true
                        persist()
                    }
                )

                Spacer(Modifier.height(16.dp))

                // ---- Карточка типа платежа: серый фон, белые поля (Figma 2872-34114) ----
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    color = CardBackground
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (scheduleActive) {
                            Text(
                                if (editing) "Есть несохранённые изменения" else "График активен",
                                style = CardSubtitleStyle.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (editing) ErrorRed else GreyText
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        // Заголовочный блок (Figma 34114: от фона 10, заголовок 18,
                        // зазор 6, подпись 16, до полей 12)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (typeIsFixed) "Постоянный платеж" else "Переменный платеж",
                                    style = Headline2MobStyle
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (typeIsFixed) "Укажите день и сумму оплаты"
                                    else "Укажите даты и суммы платежей",
                                    style = CardSubtitleStyle
                                )
                            }
                            Text(
                                if (typeIsFixed) "Ежемесячно" else "По датам",
                                style = CardSubtitleStyle
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // ---- Поля ----
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (typeIsFixed) {
                                DayField(
                                    day = fixedDay,
                                    error = fixedDayError,
                                    readOnly = readOnly,
                                    onClick = { showDaySheet = true }
                                )
                                AmountField(
                                    amount = fixedAmount,
                                    error = fixedAmountError,
                                    readOnly = readOnly,
                                    onValueChange = {
                                        fixedAmount = it
                                        fixedAmountError = null
                                        persist()
                                    }
                                )
                            } else {
                                DateField(
                                    date = variableDate,
                                    error = variableDateError,
                                    readOnly = readOnly,
                                    onClick = { showCalendar = true }
                                )
                                AmountField(
                                    amount = variableAmount,
                                    error = variableAmountError,
                                    readOnly = false,
                                    onValueChange = {
                                        variableAmount = it
                                        variableAmountError = null
                                        persist()
                                    }
                                )
                            }
                        }

                        // ---- Добавить платеж (переменный) ----
                        if (!typeIsFixed) {
                            Spacer(Modifier.height(10.dp))
                            OutlineCtaButton(
                                text = "Добавить платеж",
                                borderColor = Graphite,
                                iconRes = R.drawable.ic_plus_circle_graphite,
                                onClick = {
                                    if (readOnly) editing = true
                                    addPayment()
                                }
                            )
                        }

                        // ---- Список платежей переменного графика ----
                        if (!typeIsFixed && (payments.isNotEmpty() || bookingRows.isNotEmpty())) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Платежи",
                                style = CardSubtitleStyle,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            val rows: List<Any> = if (isDailyRent) bookingRows else payments.toList()
                            rows.forEachIndexed { index, row ->
                                val (dateText, amountText) = when (row) {
                                    is BookingRow ->
                                        (if (row.start == row.end) row.start.format(DdMmYyyy)
                                        else "${row.start.format(DdMmYyyy)} – ${row.end.format(DdMmYyyy)}") to
                                            formatAmount(row.amount)
                                    is VariablePayment ->
                                        row.date.format(DdMmYyyy) to
                                            formatAmount(row.amount.toDoubleOrNull() ?: 0.0)
                                    else -> "" to ""
                                }
                                PaymentRow(
                                    title = dateText,
                                    amount = amountText,
                                    onDelete = {
                                        if (readOnly) editing = true
                                        deletePayment(index)
                                    }
                                )
                                if (index != rows.lastIndex) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp)
                                            .height(1.dp)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        // ---- Следующий платёж постоянного графика ----
                        if (typeIsFixed && scheduleActive && fixedDay != null) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Следующий платёж",
                                style = CardSubtitleStyle,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    nextPaymentDate(fixedDay!!).format(DdMmYyyy),
                                    style = CardSubtitleStyle.copy(color = Graphite)
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    formatAmount(fixedAmount.replace(" ", "").toDoubleOrNull() ?: 0.0),
                                    style = CardSubtitleStyle.copy(
                                        color = Graphite,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ---- Поле «Реквизиты» ----
                RequisitesField(
                    name = requisite?.name,
                    caption = requisite?.accountCaption,
                    onClick = { showRequisites = true }
                )

                Spacer(Modifier.height(16.dp))
            }

            // ---- Tabbar ----
            // В пустом состоянии создания нижних кнопок нет (Figma 2872-34102):
            // «Применить график» появляется, когда становится что применять.
            if (scheduleActive || primaryEnabled) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    when {
                        !scheduleActive -> BlackCtaButton(
                            text = "Применить график",
                            enabled = primaryEnabled,
                            onClick = { applySchedule() }
                        )
                        !editing -> BlackCtaButton(
                            text = "Изменить график",
                            onClick = { editing = true }
                        )
                        else -> BlackCtaButton(
                            text = "Сохранить изменения",
                            enabled = primaryEnabled,
                            onClick = { showApplyDialog = true }
                        )
                    }
                    if (scheduleActive) {
                        OutlineCtaButton(
                            text = "Отменить график",
                            borderColor = Graphite,
                            onClick = { showCancelDialog = true }
                        )
                    }
                }
            }
        }

        // ---- Тост поверх контента, над Tabbar ----
        toast?.let { data ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 170.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                DesignToast(data = data, onDismiss = { toast = null })
            }
        }
    }

    // ---- Шиты ----
    if (showCalendar) {
        DatePickerSheet(
            title = "Дата платежа",
            initialDate = variableDate ?: LocalDate.now(),
            onDone = {
                variableDate = it
                variableDateError = null
                persist()
            },
            onDismiss = { showCalendar = false },
            ctaText = "Выбрать"
        )
    }
    if (showDaySheet) {
        DayOfMonthPickerSheet(
            selectedDay = fixedDay,
            onDone = {
                fixedDay = it
                fixedDayError = null
                persist()
            },
            onDismiss = { showDaySheet = false },
            subtitle = "Выберите день, когда арендатор должен вносить платёж"
        )
    }
    if (showRequisites) {
        if (uiState.requisites.isEmpty()) {
            EmptyRequisitesSheet(
                onAdd = {
                    showRequisites = false
                    showCreateRequisite = true
                },
                onDismiss = { showRequisites = false }
            )
        } else {
            RequisitesSheet(
                requisites = uiState.requisites,
                selectedId = requisiteId,
                onPick = {
                    requisiteId = it.id
                    persist()
                    showRequisites = false
                },
                onDismiss = { showRequisites = false }
            )
        }
    }
    if (showCreateRequisite) {
        CreateRequisiteSheet(
            onSave = { name, account, bank ->
                viewModel.createRequisite(name, account, bank)
            },
            onDismiss = { showCreateRequisite = false }
        )
    }

    // ---- Диалоги ----
    conflictDate?.let { date ->
        ScheduleDialog(
            iconRes = R.drawable.ic_calendar_warning,
            title = "На ${date.format(RuDateFormat)} уже запланирован платёж",
            body = null,
            confirmText = "Изменить платёж",
            onConfirm = {
                conflictDate = null
                showCalendar = true
            },
            onDismiss = { conflictDate = null }
        )
    }
    if (showApplyDialog) {
        ScheduleDialog(
            iconRes = null,
            title = "Применить изменения?",
            body = "Новые условия графика заменят текущие и будут отправлены арендатору",
            confirmText = "Применить изменения",
            onConfirm = {
                showApplyDialog = false
                applySchedule()
            },
            onDismiss = { showApplyDialog = false }
        )
    }
    if (showCancelDialog) {
        ScheduleDialog(
            iconRes = null,
            title = "Отменить и очистить график?",
            body = "Все будущие платежи и уведомления арендатору будут отменены",
            confirmText = "Очистить график",
            confirmColor = ErrorRed,
            onConfirm = {
                showCancelDialog = false
                viewModel.cancelSchedule(propertyId)
            },
            onDismiss = { showCancelDialog = false }
        )
    }
}

// ===================== Компоненты экрана =====================

/** Сегмент «Тип платежей» (Figma 2872-34106): трек 48dp #EFEFEF r100,
 *  выбранная половина залита графитом с белым текстом (паттерн ToggleSegment). */
@Composable
private fun PaymentTypeSegment(
    fixedSelected: Boolean,
    onSelect: (isFixed: Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(CardBackground)
    ) {
        SegmentLabel(
            text = "Постоянный",
            selected = fixedSelected,
            onClick = { onSelect(true) },
            modifier = Modifier.weight(1f)
        )
        SegmentLabel(
            text = "Переменный",
            selected = !fixedSelected,
            onClick = { onSelect(false) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(100.dp))
            .then(
                if (selected) Modifier.background(Graphite) else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                letterSpacing = (-0.4).sp,
                color = if (selected) Color.White else GreyText
            )
        )
    }
}

/** Поле «Дата» (переменный график): белое 64dp r20, календарь из макета в зоне 40dp. */
@Composable
private fun RowScope.DateField(
    date: LocalDate?,
    error: String?,
    readOnly: Boolean,
    onClick: () -> Unit
) {
    FieldContainer(error = error, modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(enabled = !readOnly) { onClick() }
                .padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                date?.format(DdMmYyyy) ?: "Дата",
                style = when {
                    date != null -> Headline2MobStyle
                    error != null -> Headline2MobPlaceholderStyle.copy(color = ErrorRed)
                    else -> Headline2MobPlaceholderStyle
                },
                modifier = Modifier.weight(1f)
            )
            // Иконка 24×24 в зоне 40 с padding 8 — как в макете (Figma 2460:8862)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_calendar_payment),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/** Поле «День месяца» (постоянный график): белое 64dp r20, наш шеврон 40dp. */
@Composable
private fun RowScope.DayField(
    day: Int?,
    error: String?,
    readOnly: Boolean,
    onClick: () -> Unit
) {
    FieldContainer(error = error, modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(enabled = !readOnly) { onClick() }
                .padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                day?.toString() ?: "День месяца",
                style = when {
                    day != null -> Headline2MobStyle
                    error != null -> Headline2MobPlaceholderStyle.copy(color = ErrorRed)
                    else -> Headline2MobPlaceholderStyle
                },
                modifier = Modifier.weight(1f)
            )
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/** Поле «Сумма, ₽»: числовой ввод, паддинг 20 (иконки нет — Figma 2872-34122). */
@Composable
private fun RowScope.AmountField(
    amount: String,
    error: String?,
    readOnly: Boolean,
    onValueChange: (String) -> Unit
) {
    FieldContainer(error = error, modifier = Modifier.weight(1f)) {
        BasicTextField(
            value = amount,
            onValueChange = onValueChange,
            enabled = !readOnly,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Graphite
            ),
            cursorBrush = SolidColor(Graphite),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (amount.isEmpty()) {
                        Text(
                            "Сумма, ₽",
                            style = if (error != null) Headline2MobPlaceholderStyle.copy(color = ErrorRed)
                            else Headline2MobPlaceholderStyle
                        )
                    }
                    inner()
                }
            }
        )
    }
}

/** Поле с ошибкой (Figma 2872:34143): красная рамка 1dp + красная подпись
 *  под полем («Выберите значение» / «Заполните поле»), фон белый, r20. */
@Composable
private fun FieldContainer(
    error: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(CardShape)
                .background(Color.White)
                .then(
                    if (error != null) Modifier.border(1.dp, ErrorRed, CardShape)
                    else Modifier
                )
        ) {
            content()
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                error,
                style = CardSubtitleStyle.copy(color = ErrorRed),
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

/** Строка платежа (Figma: 44dp, дата слева, сумма, корзина 18dp). */
@Composable
private fun PaymentRow(
    title: String,
    amount: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = CardSubtitleStyle.copy(color = Graphite))
        Spacer(Modifier.weight(1f))
        Text(
            amount,
            style = CardSubtitleStyle.copy(color = Graphite, fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.width(12.dp))
        Image(
            painter = painterResource(R.drawable.ic_action_delete),
            contentDescription = "Удалить платёж",
            modifier = Modifier
                .size(18.dp)
                .clickable { onDelete() }
        )
    }
}

/** Поле «Реквизиты для оплаты» (Figma 2872-34124): серое 372×64, паддинг 20,
 *  наш шеврон 40dp справа — и в пустом, и в заполненном состоянии. */
@Composable
private fun RequisitesField(
    name: String?,
    caption: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CardShape)
            .background(CardBackground)
            .clickable { onClick() }
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (name != null) {
            Column(Modifier.weight(1f)) {
                Text(name, style = Headline2MobStyle)
                caption?.let { Text(it, style = CardSubtitleStyle) }
            }
        } else {
            Text(
                "Реквизиты для оплаты",
                style = Headline2MobPlaceholderStyle,
                modifier = Modifier.weight(1f)
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_card_chevron),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
    }
}

/** Диалог дизайнера (Figma Delete/Continue): карточка r20, иконка сверху,
 *  заголовок, серое тело, чёрная + контурная кнопки. */
@Composable
private fun ScheduleDialog(
    iconRes: Int?,
    title: String,
    body: String?,
    confirmText: String,
    confirmColor: Color = Graphite,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DesignWidthDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                iconRes?.let {
                    Image(
                        painter = painterResource(it),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    title,
                    style = Headline2MobStyle,
                    textAlign = TextAlign.Center
                )
                body?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        style = CardSubtitleStyle,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(20.dp))
                BlackCtaButton(
                    text = confirmText,
                    containerColor = confirmColor,
                    onClick = onConfirm
                )
                Spacer(Modifier.height(6.dp))
                OutlineCtaButton(
                    text = "Назад",
                    borderColor = Graphite,
                    onClick = onDismiss
                )
            }
        }
    }
}

// ===================== Утилиты =====================

private data class CustomDateEntry(val date: String, val amount: String)

private fun parseCustomDates(json: String?): List<VariablePayment> = try {
    Gson().fromJson(json ?: "[]", Array<CustomDateEntry>::class.java)
        .mapNotNull { entry ->
            runCatching { VariablePayment(LocalDate.parse(entry.date, DdMmYyyy), entry.amount) }
                .getOrNull()
        }
        .sortedBy { it.date }
} catch (_: Exception) {
    emptyList()
}

/** «25 000 ₽» — тысячные с пробелом. */
private fun formatAmount(v: Double): String {
    val long = v.toLong()
    return if (v == long.toDouble()) {
        String.format(Locale("ru"), "%,d", long).replace('\u00A0', ' ') + " ₽"
    } else {
        String.format(Locale("ru"), "%,.2f", v).replace('\u00A0', ' ') + " ₽"
    }
}

/** Ближайшая дата платежа: день в этом месяце (если не прошёл) или в следующем. */
private fun nextPaymentDate(day: Int): LocalDate {
    val today = LocalDate.now()
    return if (day >= today.dayOfMonth) {
        today.withDayOfMonth(day.coerceAtMost(today.lengthOfMonth()))
    } else {
        val next = today.plusMonths(1)
        next.withDayOfMonth(day.coerceAtMost(next.lengthOfMonth()))
    }
}
