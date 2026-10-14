package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import coil.request.ImageRequest
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.IconNotificationDialog
import com.rentmanager.app.ui.components.ScheduleDialog
import com.rentmanager.app.util.propertyTitle
import java.time.LocalDate
import java.time.YearMonth

// Палитра канваса «Моя недвижимость» 2 вариант (2949:45807)
private val White = Color.White
private val TextPrimary = Color(0xFF212121)
private val TextGray = Color(0xFF717171)
private val DividerGrey = Color(0xFFDBDBDB)
private val CellFree = Color(0xFFEFEFEF)
private val CellBusy = Color(0xFFE5F2E7)
private val BusyGreen = Color(0xFF2F7D4D)
private val OverdueRed = Color(0xFFFF4249)


/**
 * «Моя недвижимость», 2 вариант редизайна (канвас 2949:45807):
 * табы «Объекты | Шахматка», карточки-объекты с лентой ближайшей загрузки
 * и строкой арендатора, шахматка-таблица с закреплённой колонкой объектов,
 * компактный нижний таббар (Финансы / Создать объект / На главную).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPropertiesScreen(
    onPropertyClick: (String) -> Unit,
    onCreateProperty: () -> Unit,
    onBack: () -> Unit,
    onFinanceClick: () -> Unit,
    onWriteClick: () -> Unit,
    onAttachTenant: (propertyId: String, start: LocalDate?, end: LocalDate?) -> Unit = { _, _, _ -> },
    viewModel: MyPropertiesViewModel = viewModel()
) {
    val propertiesRaw by viewModel.properties.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val tenantsById by viewModel.tenantsById.collectAsState()

    // Снекбар правки шахматки: «07.10.2026 — снято · Отменить». Случайный тап
    // по дню лечится одним тапом (в шахматке второй тап сразу применяет правку)

    // Шит «Как устроен календарь» (иконка «?» в тулбаре) и фильтр объектов
    var showChessHelp by remember { mutableStateOf(false) }
    var showObjectsFilter by remember { mutableStateOf(false) }

    // Обновление списка при возврате на экран / из фона
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Период календарей: применённый (все карточки) и черновик в шите
    val today = remember { LocalDate.now() }
    var selectedYear by remember { mutableIntStateOf(today.year) }
    var selectedMonth by remember { mutableIntStateOf(today.monthValue) }
    var draftPeriod by remember { mutableStateOf(YearMonth.of(today.year, today.monthValue)) }
    var showPeriodSheet by remember { mutableStateOf(false) }

    // Выбор нового периода в календаре карточки (чёрные ячейки) и панель card_inf
    var selectionPropertyId by remember { mutableStateOf<String?>(null) }
    var selectionStart by remember { mutableStateOf<LocalDate?>(null) }
    var selectionEnd by remember { mutableStateOf<LocalDate?>(null) }
    /** Второй тап сделан — период закрыт (можно «Добавить арендатора») */
    var selectionComplete by remember { mutableStateOf(false) }
    var infoPropertyId by remember { mutableStateOf<String?>(null) }
    var infoRange by remember { mutableStateOf<ClosedRange<LocalDate>?>(null) }

    /** Поиск по названию и адресу объекта: регистр не важен (канвас 4390:92591). */
    fun matchesSearch(property: MyPropertyItem, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return property.name.contains(q, ignoreCase = true) ||
            property.address.contains(q, ignoreCase = true)
    }

    // Фильтр «Все объекты / Посуточно / Помесячно» из пилюли CalendarToolbar
    var objectsFilter by remember { mutableStateOf(ObjectsFilter.ALL) }

    // Поиск по объектам (канвас 4390:92591): лупа в тулбаре включает строку поиска,
    // список фильтруется по названию и адресу
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Фильтр «Все объекты / Посуточно / Помесячно» + поиск
    val properties = remember(propertiesRaw, objectsFilter, searchQuery) {
        propertiesRaw
            .filter { objectsFilter.matches(it.rentType) }
            .filter { matchesSearch(it, searchQuery) }
    }

    // Прогрев кэша фото 40dp — карточки не мигают пустым квадратом при скролле
    val prefetchContext = LocalContext.current
    val prefetchDensity = LocalDensity.current
    LaunchedEffect(properties) {
        val photoSizePx = with(prefetchDensity) { 40.dp.roundToPx() }
        properties.forEach { property ->
            property.photoUrl?.let { url ->
                prefetchContext.imageLoader.enqueue(
                    ImageRequest.Builder(prefetchContext)
                        .data(url)
                        .size(photoSizePx)
                        .build()
                )
            }
        }
    }

    /** Границы периода точки: посуточно — один день, помесячно — весь месяц. */
    fun periodBounds(property: MyPropertyItem, date: LocalDate): ClosedRange<LocalDate> {
        if (property.rentType == "посуточно") return date..date
        val month = YearMonth.from(date)
        return month.atDay(1)..month.atEndOfMonth()
    }

    /**
     * Тап по куску ячейки. Куски аренды несут бронь ([CalPiece.key]) — открываем
     * `card_inf` этого периода; свободные куски (в том числе серая половина месяца
     * после выезда 21.11) начинают/закрывают новый период.
     *
     * Выбор работает в обе стороны: тап 20-го, потом 10-го = период 10–20, как и
     * 10-го, потом 20-го. Долгих нажатий на шахматке нет: день выезда/заезда уже
     * нарисован отдельной половинкой (канвас 4393:107371), а гашение долга живёт
     * в «Графике платежей».
     */
    fun onCalendarDayTap(property: MyPropertyItem, piece: CalPiece) {
        val booking = piece.key.takeIf { it.isNotBlank() }?.let { key ->
            property.bookings.firstOrNull { (it.id ?: "${it.start}..${it.end}") == key }
        }
        if (booking != null) {
            infoPropertyId = property.id
            infoRange = booking.start..booking.end
            return
        }
        val today = LocalDate.now()
        val bounds = piece.range
        if (bounds.endInclusive.isBefore(today)) return
        val start = selectionStart
        val end = selectionEnd
        if (selectionPropertyId != property.id || start == null || end == null || selectionComplete) {
            // Первый тап: период = период нажатого куска (день или месяц-половинка)
            selectionPropertyId = property.id
            selectionStart = bounds.start.coerceAtLeast(today)
            selectionEnd = bounds.endInclusive
            selectionComplete = false
            infoPropertyId = null
            infoRange = null
            return
        }
        // Второй тап: объединяем периоды — порядок нажатий не важен
        selectionStart = minOf(start, bounds.start).coerceAtLeast(today)
        selectionEnd = maxOf(end, bounds.endInclusive)
        selectionComplete = true
        viewModel.clearAttachSuggestion()
    }

    Scaffold(
        containerColor = White,
        contentWindowInsets = WindowInsets.systemBars,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(White)
        ) {
            Spacer(Modifier.height(27.dp))
            // ---- Тулбар: назад + заголовок + сортировка ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (searchActive) {
                    // Канвас 4390:92590 — поиск занимает место названия экрана
                    // и обеих иконок, строка «Выбрать период» остаётся на месте
                    CalendarSearchToolbar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onBack = {
                            searchActive = false
                            searchQuery = ""
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onBack() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_left),
                            contentDescription = "Назад",
                            modifier = Modifier.size(24.dp),
                            tint = TextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Моя недвижимость",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp,
                            color = TextPrimary
                        )
                    }
                    // Канвас 19: в тулбаре две иконки 24×24 с зазором 8 — поиск и справка
                    Icon(
                        painter = painterResource(R.drawable.ic_myprops_search),
                        contentDescription = "Поиск",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { searchActive = true },
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_myprops_help),
                        contentDescription = "Как устроен календарь",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showChessHelp = true },
                        tint = Color.Unspecified
                    )
                }
            }

            // ---- Панель календаря: «Выбрать период» + фильтр объектов (канвас 19) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                CalendarToolbar(
                    filter = objectsFilter,
                    onPeriodClick = {
                        draftPeriod = YearMonth.of(selectedYear, selectedMonth)
                        showPeriodSheet = true
                    },
                    onFilterClick = { showObjectsFilter = true }
                )
            }

            if (properties.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                PropertiesStub(
                    filter = objectsFilter,
                    hasProperties = propertiesRaw.isNotEmpty(),
                    onShowAll = { objectsFilter = ObjectsFilter.ALL },
                    onCreateProperty = onCreateProperty,
                    onManageSubscription = onFinanceClick
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 6.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(properties, key = { it.id }) { property ->
                            val start = selectionStart
                            val selection = if (selectionPropertyId == property.id && start != null) {
                                start..(selectionEnd ?: start)
                            } else null
                            CalendarObjectCard(
                                property = property,
                                anchor = YearMonth.of(selectedYear, selectedMonth),
                                selection = selection,
                                onDayClick = { piece -> onCalendarDayTap(property, piece) },
                                onOpenProperty = { onPropertyClick(property.id) },
                                // 2px — только у периода, по которому открыта карточка
                                emphasized = if (infoPropertyId == property.id) infoRange else null
                            )
                        }
                    }

                    // Нижние панели поверх списка: card_inf приоритетнее выбора
                    val infoProperty = infoPropertyId?.let { id -> properties.find { it.id == id } }
                    val infoDates = infoRange
                    val selProperty = selectionPropertyId?.let { id -> properties.find { it.id == id } }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 13.dp)
                    ) {
                        when {
                            infoProperty != null && infoDates != null -> {
                                // Арендатор берётся из брони (tenant_id), фолбэк — поле объекта
                                val infoBooking = infoProperty.bookings.firstOrNull {
                                    it.start == infoDates.start && it.end == infoDates.endInclusive
                                }
                                val infoTenant = infoBooking?.tenantId?.let { tenantsById[it] }
                                PeriodInfoPanel(
                                    periodText = formatCalendarPeriod(
                                        infoDates.start,
                                        infoDates.endInclusive,
                                        infoProperty.rentType == "посуточно"
                                    ),
                                    // Долг считаем только по этому периоду: у нового
                                    // будущего периода долга нет (было 46 800 по объекту)
                                    overdueText = (infoBooking?.debtAmount ?: 0.0)
                                        .takeIf { it > 0.0 }
                                        ?.let {
                                            "Задолженность " + String.format(
                                                java.util.Locale.US,
                                                "%,.0f ₽",
                                                it
                                            ).replace(',', ' ')
                                        },
                                    tenantName = infoTenant?.name
                                        ?: infoProperty.tenantInfo?.substringBefore(" ·"),
                                    tenantPhone = infoTenant?.phone ?: infoProperty.tenantPhone,
                                    onClose = {
                                        infoPropertyId = null
                                        infoRange = null
                                    },
                                    onEmail = { onWriteClick() }
                                )
                            }

                            selProperty != null -> SelectionPanel(
                                propertyName = propertyTitle(selProperty.name, selProperty.address),
                                periodText = formatSelectionPeriod(
                                    selectionStart,
                                    selectionEnd,
                                    selProperty.rentType == "посуточно"
                                ),
                                periodComplete = selectionComplete,
                                onAddTenant = {
                                    val startDate = selectionStart
                                    val endDate = selectionEnd ?: selectionStart
                                    selectionPropertyId = null
                                    selectionStart = null
                                    selectionEnd = null
                                    selectionComplete = false
                                    if (startDate != null && endDate != null) {
                                        onAttachTenant(selProperty.id, startDate, endDate)
                                    }
                                },
                                onCancel = {
                                    selectionPropertyId = null
                                    selectionStart = null
                                    selectionEnd = null
                                    selectionComplete = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Ошибка загрузки — окном-уведомлением, закрытие тапом вне
    errorMessage?.let {
        IconNotificationDialog(
            iconRes = R.drawable.ic_globe_warning_vec,
            text = it,
            onDismiss = { viewModel.clearError() }
        )
    }

    if (showPeriodSheet) {
        CalendarPeriodSheet(
            draft = draftPeriod,
            onDraftChange = { draftPeriod = it },
            onApply = {
                // Прошлый период не открываем: подтягиваем к текущему месяцу
                val min = YearMonth.now()
                val safe = if (draftPeriod < min) min else draftPeriod
                draftPeriod = safe
                selectedYear = safe.year
                selectedMonth = safe.monthValue
                showPeriodSheet = false
            },
            onCurrentMonth = {
                val now = YearMonth.now()
                selectedYear = now.year
                selectedMonth = now.monthValue
                draftPeriod = now
                showPeriodSheet = false
            },
            onDismiss = { showPeriodSheet = false }
        )
    }

    // Шит «Показать объекты» — фильтр списка (применяется сразу после выбора)
    if (showObjectsFilter) {
        ObjectsFilterSheet(
            selected = objectsFilter,
            onSelect = { filter ->
                objectsFilter = filter
                showObjectsFilter = false
            },
            onDismiss = { showObjectsFilter = false }
        )
    }

    // Шит «Как устроен календарь» — легенда из 5 строк и подсказки
    if (showChessHelp) {
        CalendarLegendSheet(onDismiss = { showChessHelp = false })
    }
}

// ===================== Общие шиты =====================

/** Снекбар правки шахматки: тёмная плашка + «Отменить» (своя вёрстка). */
@Composable
private fun UndoSnackbar(message: String, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TextPrimary)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            fontSize = 13.sp,
            letterSpacing = (-0.4).sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Отменить",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = Color.White,
            modifier = Modifier
                .clickable { onUndo() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
