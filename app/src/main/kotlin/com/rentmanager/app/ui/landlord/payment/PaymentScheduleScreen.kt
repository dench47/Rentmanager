package com.rentmanager.app.ui.landlord.payment

import androidx.core.content.edit
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
import com.rentmanager.app.ui.components.DesignWidthDialog
import com.rentmanager.app.ui.components.IconNotificationDialog
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---- Черновик экрана: переживает уход и возврат на экран (в памяти процесса)
// и перезапуск приложения (SharedPreferences). Хранится отдельно по объекту. ----
object PaymentScheduleCache {
    /** Объект, чей черновик сейчас в кэше; при смене — сброс и загрузка другого */
    var loadedForProperty: String? = null
    /** Черновик этого объекта уже сохранялся (пусть даже пустой) — главнее сервера */
    var hasStoredDraft: Boolean = false
    /** На сервере применён график — чтобы вход сразу рисовался с плашкой,
     *  иначе она появляется после ответа сервера и двигает контент */
    var scheduleActive: Boolean = false
    /** Тип применённого графика: плашка «График активен» относится к своей
     *  вкладке — на другом типе платежа её быть не может */
    var activeTypeIsFixed: Boolean = true
    /** Снимок ПРИМЕНЁННОГО состояния: с ним сравниваем черновик, чтобы
     *  понять, есть несохранённые изменения (красная плашка-маяк) */
    var appliedPayments: MutableList<VariablePayment> = mutableListOf()
    var appliedFixedDay: Int? = null
    var appliedFixedAmount: String = ""
    var appliedRequisiteId: String? = null
    /** Последняя известная ставка объекта — плейсхолдер «Сумма» без мигания
     *  (ставка приезжает с объектом асинхронно) */
    var rateStr: String? = null
    var typeIsFixed: Boolean = true
    /** Пользователь уже сам выбирал тип на этом черновике — не сбрасываем
     *  вкладку на дефолт по типу аренды объекта */
    var typeTouched: Boolean = false
    var fixedDay: Int? = null
    var fixedAmount: String = ""
    var variableDate: LocalDate? = null
    var variableAmount: String = ""
    val payments: MutableList<VariablePayment> = mutableListOf()
    var requisiteId: String? = null

    private val gson = Gson()
    private const val PREFS = "payment_schedule_drafts"

    // Gson не сериализует java.time надёжно — даты в DTO строками ISO
    private data class DraftPaymentDto(val date: String, val amount: String)

    private data class DraftDto(
        val scheduleActive: Boolean = false,
        val activeTypeIsFixed: Boolean = true,
        val appliedPayments: List<DraftPaymentDto> = emptyList(),
        val appliedFixedDay: Int? = null,
        val appliedFixedAmount: String = "",
        val appliedRequisiteId: String? = null,
        val rateStr: String? = null,
        val typeIsFixed: Boolean = true,
        val typeTouched: Boolean = false,
        val fixedDay: Int? = null,
        val fixedAmount: String = "",
        val variableDate: String? = null,
        val variableAmount: String = "",
        val payments: List<DraftPaymentDto> = emptyList(),
        val requisiteId: String? = null
    )

    /** Разовый вход на экран: сбрасывает кэш и поднимает черновик объекта. */
    fun ensureLoaded(context: android.content.Context, propertyId: String) {
        if (loadedForProperty == propertyId) return
        loadedForProperty = propertyId
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        // Разовая миграция: в черновиках v1 могли остаться «ежедневные» строки,
        // порожденные слиянием брони длительной аренды — вычищаем
        if (prefs.getInt("drafts_version", 1) < 2) {
            prefs.edit { clear(); putInt("drafts_version", 2) }
        }
        typeIsFixed = true
        typeTouched = false
        fixedDay = null
        fixedAmount = ""
        variableDate = null
        variableAmount = ""
        payments.clear()
        requisiteId = null
        hasStoredDraft = false
        scheduleActive = false
        appliedPayments.clear()
        appliedFixedDay = null
        appliedFixedAmount = ""
        appliedRequisiteId = null
        rateStr = null
        runCatching {
            val json = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .getString(propertyId, null)
            if (json != null) hasStoredDraft = true
            val dto = gson.fromJson(json ?: "{}", DraftDto::class.java)
            scheduleActive = dto.scheduleActive
            activeTypeIsFixed = dto.activeTypeIsFixed
            appliedPayments.clear()
            dto.appliedPayments.forEach { p ->
                runCatching { VariablePayment(LocalDate.parse(p.date), p.amount) }.getOrNull()
                    ?.let { appliedPayments.add(it) }
            }
            appliedPayments.sortBy { it.date }
            appliedFixedDay = dto.appliedFixedDay
            appliedFixedAmount = dto.appliedFixedAmount
            appliedRequisiteId = dto.appliedRequisiteId
            rateStr = dto.rateStr
            typeIsFixed = dto.typeIsFixed
            typeTouched = dto.typeTouched
            fixedDay = dto.fixedDay
            fixedAmount = dto.fixedAmount
            variableDate = dto.variableDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            variableAmount = dto.variableAmount
            dto.payments.forEach { p ->
                runCatching { VariablePayment(LocalDate.parse(p.date), p.amount) }.getOrNull()
                    ?.let { payments.add(it) }
            }
            payments.sortBy { it.date }
            requisiteId = dto.requisiteId
        }
    }

    /** Сохранение черновика — вызывается при каждом изменении полей. */
    fun save(context: android.content.Context, propertyId: String) {
        hasStoredDraft = true
        runCatching {
            val dto = DraftDto(
                scheduleActive = scheduleActive,
                activeTypeIsFixed = activeTypeIsFixed,
                appliedPayments = appliedPayments.map { DraftPaymentDto(it.date.toString(), it.amount) },
                appliedFixedDay = appliedFixedDay,
                appliedFixedAmount = appliedFixedAmount,
                appliedRequisiteId = appliedRequisiteId,
                rateStr = rateStr,
                typeIsFixed = typeIsFixed,
                typeTouched = typeTouched,
                fixedDay = fixedDay,
                fixedAmount = fixedAmount,
                variableDate = variableDate?.toString(),
                variableAmount = variableAmount,
                payments = payments.map { DraftPaymentDto(it.date.toString(), it.amount) },
                requisiteId = requisiteId
            )
            context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .edit { putString(propertyId, gson.toJson(dto)) }
        }
    }
}

data class VariablePayment(
    val date: LocalDate,
    val amount: String
)

private val RuDateFormat = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
private val DdMmYyyy = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun PaymentScheduleScreen(
    propertyId: String,
    onBack: () -> Unit,
    viewModel: PaymentScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Вход на экран: поднимаем черновик этого объекта (в памяти, а после
    // перезапуска приложения — из локального хранилища). remember обязан
    // вернуть значение — возвращаем true (побочный эффект в ensureLoaded)
    remember(propertyId) {
        PaymentScheduleCache.ensureLoaded(context, propertyId)
        true
    }

    // ---- Состояние экрана ----
    var typeIsFixed by remember { mutableStateOf(PaymentScheduleCache.typeIsFixed) }
    var scheduleActive by remember { mutableStateOf(PaymentScheduleCache.scheduleActive) } // на сервере есть непустой график
    var activeTypeIsFixed by remember { mutableStateOf(PaymentScheduleCache.activeTypeIsFixed) } // тип применённого графика

    // Снимок применённого состояния (с ним сравниваем черновик — «маяк»
    // несохранённых изменений); переживает перезапуск вместе с черновиком
    var appliedPayments by remember { mutableStateOf(PaymentScheduleCache.appliedPayments.toList()) }
    var appliedFixedDay by remember { mutableStateOf(PaymentScheduleCache.appliedFixedDay) }
    var appliedFixedAmount by remember { mutableStateOf(PaymentScheduleCache.appliedFixedAmount) }
    var appliedRequisiteId by remember { mutableStateOf(PaymentScheduleCache.appliedRequisiteId) }

    // Ставка-плейсхолдер «Сумма, ₽» и значение по умолчанию — та же цифра,
    // что в «Арендной плате» карточки: при применённом постоянном графике —
    // его сумма, иначе ставка объявления. Пока объект грузится — последняя
    // известная из черновика, чтобы плейсхолдер не мигал при входе
    val rateStr = run {
        val scheduleType = uiState.schedule?.type
            ?: if ((uiState.property?.rentType ?: "посуточно") == "длительно") "auto" else "manual"
        val amount = if (scheduleType == "auto") {
            uiState.schedule?.amount ?: uiState.property?.rentAmount
        } else {
            uiState.property?.rentAmount
        }
        amount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }
    } ?: PaymentScheduleCache.rateStr

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
    // Выход с экрана при несохранённых изменениях (Figma 2959-41578):
    // применить и выйти / выйти без применения
    var showExitDialog by remember { mutableStateOf(false) }
    var exitAfterApply by remember { mutableStateOf(false) }
    // Отмена последнего удаления: диалог «Платёж был удален» висит, пока
    // пользователь не тапнет вне — кнопка возвращает платёж (Figma 2872-34877)
    var deletedUndo by remember { mutableStateOf<(() -> Unit)?>(null) }
    // Диалог успеха добавления (Figma 2872-34864): без кнопок, закрытие тапом вне
    var paymentAddedText by remember { mutableStateOf<String?>(null) }
    // Диалог «Нет связи с сервером» (Figma 2872-34883): зелёная рамка,
    // глобус с «!», пояснение зависит от действия
    var noConnectionText by remember { mutableStateOf<String?>(null) }
    var scheduleConsumed by remember { mutableStateOf(false) }

    fun persist() {
        PaymentScheduleCache.scheduleActive = scheduleActive
        PaymentScheduleCache.activeTypeIsFixed = activeTypeIsFixed
        PaymentScheduleCache.appliedPayments.clear()
        PaymentScheduleCache.appliedPayments.addAll(appliedPayments)
        PaymentScheduleCache.appliedFixedDay = appliedFixedDay
        PaymentScheduleCache.appliedFixedAmount = appliedFixedAmount
        PaymentScheduleCache.appliedRequisiteId = appliedRequisiteId
        PaymentScheduleCache.rateStr = rateStr
        PaymentScheduleCache.typeIsFixed = typeIsFixed
        PaymentScheduleCache.fixedDay = fixedDay
        PaymentScheduleCache.fixedAmount = fixedAmount
        PaymentScheduleCache.variableDate = variableDate
        PaymentScheduleCache.variableAmount = variableAmount
        PaymentScheduleCache.payments.clear()
        PaymentScheduleCache.payments.addAll(payments)
        PaymentScheduleCache.requisiteId = requisiteId
        PaymentScheduleCache.save(context, propertyId)
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
        // Возврат в «первоначальное» состояние: вкладка — снова по типу аренды
        PaymentScheduleCache.typeTouched = false
        typeIsFixed = !viewModel.uiState.value.isDailyRent
        appliedPayments = emptyList()
        appliedFixedDay = null
        appliedFixedAmount = ""
        appliedRequisiteId = null
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
        if (scheduleActive && s != null) {
            typeIsFixed = hasFixed
            activeTypeIsFixed = hasFixed
            // Снимок применённого — с сервера
            if (hasFixed) {
                appliedPayments = emptyList()
                appliedFixedDay = s.dayOfMonth
                appliedFixedAmount = s.amount?.let {
                    if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                } ?: ""
                appliedRequisiteId = s.requisites
            } else {
                appliedPayments = parseCustomDates(s.customDates)
                appliedFixedDay = null
                appliedFixedAmount = ""
                appliedRequisiteId = s.requisites
            }
            // Сохранённый черновик (даже пустой — например, всё удалено)
            // главнее сервера; с сервера берём только при первом входе
            if (!PaymentScheduleCache.hasStoredDraft) {
                if (hasFixed) {
                    fixedDay = s.dayOfMonth
                    fixedAmount = s.amount?.let {
                        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                    } ?: ""
                } else {
                    payments.clear()
                    payments.addAll(parseCustomDates(s.customDates))
                }
            }
            if (requisiteId == null) requisiteId = s.requisites
        } else {
            // Графика на сервере нет — снимок применённого пуст
            appliedPayments = emptyList()
            appliedFixedDay = null
            appliedFixedAmount = ""
            appliedRequisiteId = null
            if (!PaymentScheduleCache.typeTouched) {
                // График ещё не настроен: стартовая вкладка «Тип платежей» — по типу
                // аренды из карточки объекта («Аренда и платежи»: «₽ / сутки» →
                // переменный, «₽ / месяц» → постоянный)
                typeIsFixed = !uiState.isDailyRent
            }
        }
        // Посуточно: дни броней без строки в списке (платежи, созданные до
        // правок, или брони из шахматки) тоже показываются строками —
        // сумма = ставка объекта за сутки; введённые вручную суммы не трогаем.
        // Только будущие дни: прошедшие платежи неактуальны.
        // СТРОГО при rentType «посуточно» и БЕЗ применённого постоянного
        // графика: занятость длительной аренки — одна длинная бронь
        // (шахматка), размножать её в ежедневные платежи нельзя
        val canMergeBookings = uiState.property?.rentType == "посуточно" &&
            !(scheduleActive && activeTypeIsFixed)
        if (canMergeBookings && uiState.bookings.isNotEmpty()) {
            val today = LocalDate.now()
            val known = payments.map { it.date }.toHashSet()
            val rate = uiState.property?.rentAmount
            val rateStr = rate?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            } ?: "0"
            uiState.bookings.forEach { (s, e) ->
                var d = maxOf(s, today)
                while (!d.isAfter(e)) {
                    if (known.add(d)) payments.add(VariablePayment(d, rateStr))
                    d = d.plusDays(1)
                }
            }
            payments.sortBy { it.date }
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
                        typeIsFixed = s.dayOfMonth != null
                        activeTypeIsFixed = s.dayOfMonth != null
                        if (typeIsFixed) payments.clear()
                        // Снимок применённого = только что сохранённое состояние
                        if (typeIsFixed) {
                            appliedPayments = emptyList()
                            appliedFixedDay = fixedDay
                            appliedFixedAmount = fixedAmount.replace(" ", "")
                            appliedRequisiteId = requisiteId
                        } else {
                            appliedPayments = payments.toList()
                            appliedFixedDay = null
                            appliedFixedAmount = ""
                            appliedRequisiteId = requisiteId
                        }
                    } else {
                        resetDraft()
                    }
                    // «Применить и выйти» из диалога выхода: график сохранён — уходим
                    if (exitAfterApply) {
                        exitAfterApply = false
                        onBack()
                    }
                    persist()
                }
                ScheduleEvent.ApplyFailed -> {
                    exitAfterApply = false
                    noConnectionText =
                        "Не\u00A0удалось применить график. \nПовторите еще\u00A0раз"
                }
                ScheduleEvent.RequisiteCreated -> {
                    showCreateRequisite = false
                    requisiteId = viewModel.uiState.value.requisites.lastOrNull()?.id
                    persist()
                }
            }
        }
    }

    // Ошибка загрузки экрана — тем же окном «Нет связи», не тостом
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { noConnectionText = it }
    }

    val isDailyRent = uiState.isDailyRent
    val requisite = uiState.requisites.firstOrNull { it.id == requisiteId }

    // Плашки относятся к вкладке применённого типа графика
    val onAppliedTab = scheduleActive && typeIsFixed == activeTypeIsFixed
    // «Маяк» (ответ дизайнера): был зелёный статус, пользователь изменил
    // что-либо — добавил/удалил платёж, поменял сумму или реквизит — плашка
    // краснеет и просит нажать кнопку, а не просто выйти
    val hasUnsavedChanges = onAppliedTab && (
        if (activeTypeIsFixed) {
            fixedDay != appliedFixedDay ||
                fixedAmount.replace(" ", "") != appliedFixedAmount ||
                requisiteId != appliedRequisiteId
        } else {
            payments.toList() != appliedPayments || requisiteId != appliedRequisiteId
        }
        )

    // Даты для точек в календарном шите (Figma 2872-34192): строки списка
    // «Добавленные платежи» плюс (посуточно) дни броней — занятые даты,
    // созданные ранее или из шахматки, тоже помечаются точкой, иначе
    // конфликт «уже запланирован платёж» возникает на неотмеченной дате
    val savedPaymentDates = remember(payments.toList(), uiState.bookings, isDailyRent) {
        buildSet {
            payments.forEach { add(it.date) }
            if (isDailyRent) {
                uiState.bookings.forEach { (s, e) ->
                    var d = s
                    while (!d.isAfter(e)) {
                        add(d)
                        d = d.plusDays(1)
                    }
                }
            }
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
            // Даты + введённые на этом экране суммы — единый список для обоих
            // типов аренды (Figma 2872-34429: у каждой даты своя сумма);
            // посуточно брони создаются при добавлении строки (шахматка)
            viewModel.saveVariableManual(propertyId, payments.toList(), requisiteId)
        }
    }

    fun addPayment() {
        // Заметка дизайнера (Figma 2872:34854): нажатие «Добавить платеж»
        // подчёркивает незаполненные поля; платёж создаётся только когда всё заполнено.
        // Исключение — сумма: пустое поле не ошибка, берётся ставка объекта
        var valid = true
        val date = variableDate
        if (date == null) {
            variableDateError = "Выберите значение"
            valid = false
        }
        val amountStr = variableAmount.replace(" ", "").ifEmpty { rateStr ?: "" }
        val amount = amountStr.toDoubleOrNull()
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
                    // Строка списка — с введённой суммой (не ставкой из карточки)
                    payments.add(VariablePayment(paymentDate, amountStr))
                    payments.sortBy { it.date }
                    variableDate = null
                    variableAmount = ""
                    variableDateError = null
                    variableAmountError = null
                    // Колбэк асинхронный: persist() в конце addPayment уже
                    // отработал без этой строки — черновик обязан сохраниться тут
                    persist()
                    paymentAddedText = "Платёж на ${paymentDate.format(RuDateFormat)} добавлен"
                } else {
                    // Бронь не создалась (нет связи с сервером) — окно
                    // «Нет связи» с пояснением действия (2872-34883)
                    noConnectionText = "Не\u00A0удалось добавить платёж"
                }
            }
        } else {
            if (payments.any { it.date == paymentDate }) {
                conflictDate = paymentDate
                return
            }
            payments.add(VariablePayment(paymentDate, amountStr))
            payments.sortBy { it.date }
            variableDate = null
            variableAmount = ""
            variableDateError = null
            variableAmountError = null
            paymentAddedText = "Платёж на ${paymentDate.format(RuDateFormat)} добавлен"
        }
        persist()
    }

    fun deletePayment(index: Int) {
        val snapshot = payments.toList()
        val row = payments.getOrNull(index) ?: return
        // Посуточно строка связана с бронью этих суток — снимаем и её
        if (isDailyRent) viewModel.deleteBooking(propertyId, row.date, row.date)
        payments.removeAt(index)
        persist()
        deletedUndo = {
            if (isDailyRent) viewModel.recreateBookings(propertyId, listOf(row.date to row.date))
            payments.clear()
            payments.addAll(snapshot)
            payments.sortBy { it.date }
            persist()
        }
    }

    // ---- Валидность верхней кнопки ----
    val primaryEnabled = if (typeIsFixed) {
        fixedDay != null && (fixedAmount.replace(" ", "").toDoubleOrNull() ?: 0.0) > 0.0
    } else {
        payments.isNotEmpty()
    }

    // Выход с несохранённой работой — сначала диалог (2959-41578)
    val hasPendingWork = hasUnsavedChanges || (!scheduleActive && primaryEnabled)
    fun tryLeave() {
        if (hasPendingWork) showExitDialog = true else onBack()
    }
    // Системный «назад» перехватываем только когда есть что терять
    androidx.activity.compose.BackHandler(enabled = hasPendingWork) { showExitDialog = true }

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
            ScreenToolbar(title = "График платежей", onBack = { tryLeave() })

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
                        PaymentScheduleCache.typeTouched = true
                        persist()
                    }
                )

                Spacer(Modifier.height(20.dp))

                // ---- Плашки — ОТДЕЛЬНЫЙ элемент над карточкой (Figma 2872-34556):
                // 372×38 r10, зазор до карточки 12. Зелёная «График активен»
                // (2872-34486) — на вкладке применённого типа без изменений;
                // красная «Изменения еще не применены» — маяк: изменили платёж/
                // сумму/реквизит — нажмите кнопку, а не выходите
                if (onAppliedTab && !hasUnsavedChanges) {
                    StatusPlate("График активен", Color(0xFFE5F2E7), Color(0xFF2F7D4D))
                    Spacer(Modifier.height(12.dp))
                } else if (onAppliedTab) {
                    // Сюда попадаем только с hasUnsavedChanges = true
                    StatusPlate("Изменения еще не применены", Color(0xFFFBEAEC), ErrorRed)
                    Spacer(Modifier.height(12.dp))
                }

                // ---- Карточка типа платежа: серый фон, белые поля (Figma 2872-34114) ----
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    color = CardBackground
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {

                        // Заголовочный блок (Figma 34114: от края карточки 10,
                        // заголовок 18, зазор 6, подпись 16, до полей 12)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (typeIsFixed) {
                                DayField(
                                    day = fixedDay,
                                    error = fixedDayError,
                                    onClick = { showDaySheet = true }
                                )
                                AmountField(
                                    amount = fixedAmount,
                                    error = fixedAmountError,
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
                                    onClick = { showCalendar = true }
                                )
                                AmountField(
                                    amount = variableAmount,
                                    error = variableAmountError,
                                    placeholder = rateStr?.let { "$it ₽" } ?: "Сумма, ₽",
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
                                onClick = { addPayment() }
                            )
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
                    }
                }

                // ---- «Добавленные платежи» — ОТДЕЛЬНЫЙ блок под карточкой
                // (Figma 2872-34440/34451): серая карточка формы (2872-34441,
                // 203dp) не растягивается — список живёт на белом фоне экрана:
                // зазор от карточки 22, до строк 6, строки 44, после каждой
                // разделитель #DBDBDB во всю ширину
                if (!typeIsFixed && payments.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    Text(
                        "Добавленные платежи",
                        style = CardSubtitleStyle.copy(
                            fontWeight = FontWeight.Medium,
                            color = Graphite
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    payments.forEachIndexed { index, payment ->
                        PaymentRow(
                            date = payment.date.format(DdMmYyyy),
                            amount = formatAmount(payment.amount.toDoubleOrNull() ?: 0.0),
                            onDelete = { deletePayment(index) }
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFDBDBDB))
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    Spacer(Modifier.height(12.dp))
                }

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
                // Таббар (Figma 2872-34594): серая полоса #EDEDED на всю ширину
                // со скруглёнными верхними углами, кнопки с отступом 20,
                // вертикальные 10/6/10 (+ инсет навигации внутри серого фона)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFEDEDED),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Верхняя CTA — только когда есть что применять:
                    // зелёный статус (график применён, правок нет) — кнопки
                    // нет вовсе; красная плашка/создание — кнопка на месте.
                    // На вкладке применённого типа — «Применить изменения»
                    // (Figma 2872-34890), иначе «Применить график» (34889)
                    if (!onAppliedTab || hasUnsavedChanges) {
                        BlackCtaButton(
                            text = if (onAppliedTab) "Применить изменения" else "Применить график",
                            enabled = primaryEnabled,
                            onClick = { showApplyDialog = true }
                        )
                    }
                    // Нижняя CTA: при несохранённых правках — «Отменить
                    // изменения» (2872-34594), иначе «Отменить и очистить»
                    // (2872-34488); обе через диалог подтверждения
                    OutlineCtaButton(
                        text = if (onAppliedTab && hasUnsavedChanges) "Отменить изменения" else "Отменить и очистить",
                        borderColor = ErrorRed,
                        textColor = ErrorRed,
                        onClick = { showCancelDialog = true }
                    )
                }
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
                showCalendar = false
                persist()
            },
            onDismiss = { showCalendar = false },
            markedDates = savedPaymentDates
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

    // «Нет связи с сервером» (Figma 2872-34883): глобус с «!», пояснение
    // зависит от действия; закрытие тапом вне
    noConnectionText?.let { text ->
        IconNotificationDialog(
            iconRes = R.drawable.ic_globe_warning_vec,
            text = text,
            onDismiss = { noConnectionText = null }
        )
    }

    // Выход с несохранёнными изменениями (Figma 2959-41578): «Применить
    // и выйти» сохраняет и уходит; «Выйти без применения» просто уходит
    if (showExitDialog) {
        ScheduleDialog(
            iconRes = null,
            title = "Применить изменения?",
            body = "У вас есть несохраненные изменения. Новые условия графика заменят текущие и будут отправлены арендатору",
            confirmText = "Применить и выйти",
            backText = "Выйти без применения",
            onBackAction = {
                showExitDialog = false
                onBack()
            },
            onConfirm = {
                showExitDialog = false
                exitAfterApply = true
                applySchedule()
            },
            onDismiss = { showExitDialog = false }
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
                // Заменяем сумму существующего платежа на введённую; если дата
                // была только бронью (создана до правок) — добавляем строку
                val amount = variableAmount.replace(" ", "")
                val idx = payments.indexOfFirst { it.date == date }
                if (idx >= 0) payments[idx] = payments[idx].copy(amount = amount)
                else payments.add(VariablePayment(date, amount))
                payments.sortBy { it.date }
                variableDate = null
                variableAmount = ""
                variableDateError = null
                variableAmountError = null
                persist()
            },
            onDismiss = { conflictDate = null }
        )
    }
    // «Платёж на … добавлен» (Figma 2872-34864): галочка, зазор 10, без кнопок
    paymentAddedText?.let { text ->
        IconNotificationDialog(
            iconRes = R.drawable.ic_check_green,
            text = text,
            iconGap = 10.dp,
            onDismiss = { paymentAddedText = null }
        )
    }
    if (showApplyDialog) {
        if (scheduleActive) {
            // Правка активного графика (Figma 2872-34890)
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
        } else {
            // Первый запуск графика (Figma 2872-34889)
            ScheduleDialog(
                iconRes = null,
                title = "Применить график?",
                body = "График начнёт действовать. В указанные даты арендатор получит push-уведомление в 10:00 по местному времени",
                confirmText = "Применить график",
                onConfirm = {
                    showApplyDialog = false
                    applySchedule()
                },
                onDismiss = { showApplyDialog = false }
            )
        }
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
                // Посуточно брони и есть платежи: отменяем будущие, иначе
                // список воскреснет при следующем входе (merge по броням)
                if (isDailyRent) {
                    viewModel.deleteBooking(
                        propertyId, LocalDate.now(), LocalDate.now().plusYears(10)
                    )
                }
                if (scheduleActive) viewModel.cancelSchedule(propertyId) else resetDraft()
            },
            onDismiss = { showCancelDialog = false }
        )
    }
    // «Платёж был удален» (Figma 2872-34877): зелёная рамка, галочка,
    // единственная кнопка «Отменить удаление»; скрыть можно только тапом вне
    deletedUndo?.let { undo ->
        ScheduleDialog(
            iconRes = R.drawable.ic_check_green,
            title = "Платёж был удален",
            body = null,
            confirmText = "Отменить удаление",
            borderColor = Color(0xFF2F7D4D),
            showBack = false,
            onConfirm = {
                undo()
                deletedUndo = null
            },
            onDismiss = { deletedUndo = null }
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
    onClick: () -> Unit
) {
    FieldContainer(error = error, modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { onClick() }
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
    onClick: () -> Unit
) {
    FieldContainer(error = error, modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { onClick() }
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

/** Поле «Сумма, ₽»: числовой ввод, паддинг 20 (иконки нет — Figma 2872-34122).
 *  Плейсхолдер переопределяется ставкой объекта для переменного графика. */
@Composable
private fun RowScope.AmountField(
    amount: String,
    error: String?,
    placeholder: String = "Сумма, ₽",
    onValueChange: (String) -> Unit
) {
    FieldContainer(error = error, modifier = Modifier.weight(1f)) {
        BasicTextField(
            value = amount,
            onValueChange = onValueChange,
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
                            placeholder,
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

/** Плашка состояния над карточкой (Figma 2872-34556): отдельный элемент
 *  372×38 r10 с центрированным текстом 15/600. */
@Composable
private fun StatusPlate(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = Headline2MobStyle.copy(color = fg))
    }
}

/** Строка платежа (Figma 2872-34451): 44dp во всю ширину карточки; текстовая
 *  зона 322 из 372 — дата слева, сумма прижата к правому краю зоны; далее
 *  зазор и корзина 18dp (правый край зоны 44). Дата и сумма 15/600 #212121. */
@Composable
private fun PaymentRow(
    date: String,
    amount: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(date, style = Headline2MobStyle)
            Text(
                amount,
                style = Headline2MobStyle,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        Spacer(Modifier.width(19.dp))
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

/** Диалог дизайнера (Figma 2872-34877/34888/34890): карточка r20, опционально
 *  иконка 50 сверху, заголовок 20/600, тело 15/600 #717171, чёрная + контурная
 *  кнопки; вариант «Платёж был удален» — зелёная рамка и без «Назад». */
@Composable
private fun ScheduleDialog(
    iconRes: Int?,
    title: String,
    body: String?,
    confirmText: String,
    confirmColor: Color = Graphite,
    borderColor: Color? = null,
    showBack: Boolean = true,
    backText: String = "Назад",
    onBackAction: (() -> Unit)? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DesignWidthDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            color = Color.White,
            border = borderColor?.let { androidx.compose.foundation.BorderStroke(1.dp, it) }
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
                        style = Headline2MobStyle.copy(color = GreyText),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(20.dp))
                BlackCtaButton(
                    text = confirmText,
                    containerColor = confirmColor,
                    onClick = onConfirm
                )
                if (showBack) {
                    Spacer(Modifier.height(6.dp))
                    OutlineCtaButton(
                        text = backText,
                        borderColor = Graphite,
                        onClick = { onBackAction?.invoke() ?: onDismiss() }
                    )
                }
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
