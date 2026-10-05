package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.IconNotificationDialog
import com.rentmanager.app.ui.components.ScheduleDialog
import com.rentmanager.app.util.DD_MM_YYYY
import com.rentmanager.app.util.hasPropertyName
import com.rentmanager.app.util.leaseEnd
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

private val MonthNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)
private val MonthShort = listOf(
    "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
    "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
)

private fun monthName(month: Int): String = MonthNames.getOrElse(month - 1) { "" }

/** Способ сортировки списка (аннотация дизайнера к иконке в тулбаре) */
private enum class SortMode(val label: String) {
    AS_IS("Без сортировки"),
    ALPHA("По алфавиту"),
    DAILY_FIRST("Сначала посуточные"),
    MONTHLY_FIRST("Сначала помесячные")
}

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
    val viewMode by viewModel.viewMode.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val attachSuggestion by viewModel.attachSuggestion.collectAsState()
    val lastEdit by viewModel.lastEdit.collectAsState()

    // Снекбар правки шахматки: «07.10.2026 — снято · Отменить». Случайный тап
    // по дню лечится одним тапом (в шахматке второй тап сразу применяет правку)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(lastEdit) {
        val edit = lastEdit ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = edit.label, duration = SnackbarDuration.Long)
        viewModel.dismissEdit()
    }

    // Шит «Как работает шахматка» (иконка «?» в тулбаре): жесты неочевидны
    var showChessHelp by remember { mutableStateOf(false) }

    // Обновление списка при возврате на экран / из фона
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0 — Объекты, 1 — Шахматка
    var sortMode by remember { mutableStateOf(SortMode.AS_IS) }
    var showSortSheet by remember { mutableStateOf(false) }

    // Месяц шапки вкладки «Объекты» и период шахматки
    val today = remember { LocalDate.now() }
    var selectedYear by remember { mutableIntStateOf(today.year) }
    var selectedMonth by remember { mutableIntStateOf(today.monthValue) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    // Долгое нажатие на красной ячейке — ручное гашение просрочки
    var confirmPaidPropertyId by remember { mutableStateOf<String?>(null) }

    // На «Шахматке» сортировка по типу аренды не имеет смысла — тип
    // выбирают тумблеры; там действует только алфавит/без сортировки
    val effectiveSortMode = if (activeTab == 1) {
        if (sortMode == SortMode.DAILY_FIRST || sortMode == SortMode.MONTHLY_FIRST) SortMode.AS_IS
        else sortMode
    } else sortMode
    val properties = remember(propertiesRaw, effectiveSortMode) {
        when (effectiveSortMode) {
            SortMode.AS_IS -> propertiesRaw
            SortMode.ALPHA -> propertiesRaw.sortedBy { it.name.lowercase() }
            SortMode.DAILY_FIRST -> propertiesRaw.sortedBy { it.rentType != "посуточно" }
            SortMode.MONTHLY_FIRST -> propertiesRaw.sortedBy { it.rentType == "посуточно" }
        }
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

    Scaffold(
        containerColor = White,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                UndoSnackbar(
                    message = data.visuals.message,
                    onUndo = {
                        data.dismiss()
                        viewModel.undoLastEdit()
                    }
                )
            }
        },
        bottomBar = {
            CompactTabBar(
                onFinanceClick = onFinanceClick,
                onCreateProperty = onCreateProperty,
                onHomeClick = onBack
            )
        }
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
                if (activeTab == 0) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sort_transfer),
                        contentDescription = "Сортировка",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showSortSheet = true },
                        tint = Color.Unspecified
                    )
                } else {
                    // Своя вёрстка (в макете такого элемента нет): подсказка по
                    // жестам шахматки — тапы и долгие тапы неочевидны
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .border(1.5.dp, TextPrimary, RoundedCornerShape(100.dp))
                            .clickable { showChessHelp = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // ---- Табы «Объекты | Шахматка» с волной активной вкладки ----
            TabsRow(activeTab = activeTab, onSelect = { activeTab = it })

            if (activeTab == 0) {
                ObjectsTab(
                    modifier = Modifier.weight(1f),
                    properties = properties,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    onMonthClick = { showMonthPicker = true },
                    onPropertyClick = onPropertyClick,
                    onAddTenant = { propertyId -> onAttachTenant(propertyId, null, null) }
                )
            } else {
                ChessTab(
                    modifier = Modifier.weight(1f),
                    properties = properties.filter {
                        if (viewMode == ViewMode.MONTHS) it.rentType != "посуточно"
                        else it.rentType == "посуточно"
                    },
                    viewMode = viewMode,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    onModeChange = { viewModel.setViewMode(it) },
                    onPeriodClick = {
                        if (viewMode == ViewMode.MONTHS) showYearPicker = true else showMonthPicker = true
                    },
                    onRangeSelected = { propertyId, start, end, isRemove ->
                        // Покраска месяца = весь месяц: конец диапазона — конец месяца
                        val monthEnd = if (viewMode == ViewMode.MONTHS) {
                            YearMonth.from(end).atEndOfMonth()
                        } else end
                        if (isRemove) viewModel.deleteBookings(propertyId, start, monthEnd)
                        else viewModel.saveBooking(propertyId, start, monthEnd)
                    },
                    onOverdueMark = { confirmPaidPropertyId = it },
                    attachSuggestion = attachSuggestion,
                    onSelectionStarted = { viewModel.clearAttachSuggestion() },
                    onAttachFromDate = { pid, date -> onAttachTenant(pid, date, null) },
                    onAttachTenant = onAttachTenant
                )
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

    // Ручное гашение просрочки (долгое нажатие на красной ячейке шахматки)
    confirmPaidPropertyId?.let { id ->
        val item = properties.find { it.id == id }
        if (item != null) {
            ScheduleDialog(
                iconRes = null,
                title = "Отметить платёж полученным?",
                body = "Платёж " + String.format(java.util.Locale.US, "%,.0f ₽", item.overdueAmount).replace(',', ' ') +
                    " будет отмечен как полученный сегодня — например, оплаченный наличными",
                confirmText = "Отметить оплату",
                onConfirm = {
                    confirmPaidPropertyId = null
                    viewModel.markOverduePaid(id)
                },
                onDismiss = { confirmPaidPropertyId = null }
            )
        }
    }

    if (showSortSheet) {
        val available = if (activeTab == 0) SortMode.entries
        else listOf(SortMode.AS_IS, SortMode.ALPHA)
        OptionSheet(
            title = "Сортировка",
            options = available.map { it.label },
            selected = effectiveSortMode.label,
            onSelect = { label ->
                sortMode = SortMode.entries.first { it.label == label }
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false }
        )
    }

    // Подсказка по жестам шахматки (иконка «?» в тулбаре на вкладке «Шахматка»)
    if (showChessHelp) {
        ChessHelpSheet(onDismiss = { showChessHelp = false })
    }

    if (showMonthPicker) {
        PeriodPickerSheet(
            viewMode = ViewMode.DAYS,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            onSelect = { year, month ->
                selectedYear = year
                selectedMonth = month
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    if (showYearPicker) {
        PeriodPickerSheet(
            viewMode = ViewMode.MONTHS,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            onSelect = { year, _ ->
                selectedYear = year
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }
}

// ===================== Табы «Объекты | Шахматка» =====================

/** Строка табов: подписи 18/600 через 13, под активной — волна 4dp,
 *  под всей строкой — разделитель #DBDBDB (2949:45807). */
@Composable
private fun TabsRow(activeTab: Int, onSelect: (Int) -> Unit) {
    // Макет 3089:45496: подписи ЦЕНТРИРОВАНЫ как группа с зазором 109,
    // «волна» активного таба — 142 шириной и по центру СВОЕЙ подписи.
    var x0 by remember { mutableIntStateOf(0) }
    var x1 by remember { mutableIntStateOf(0) }
    var w0 by remember { mutableIntStateOf(0) }
    var w1 by remember { mutableIntStateOf(0) }
    var lineX by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(109.dp, Alignment.CenterHorizontally)
        ) {
            TabLabel("Объекты", active = activeTab == 0, onPosition = { c ->
                x0 = c.positionInRoot().x.toInt(); w0 = c.size.width
            }) { onSelect(0) }
            TabLabel("Шахматка", active = activeTab == 1, onPosition = { c ->
                x1 = c.positionInRoot().x.toInt(); w1 = c.size.width
            }) { onSelect(1) }
        }
        // Зона 12dp: разделитель по центру, волна активного таба поверх;
        // зазор 6 между подписями и линией — макет 3089:45523 (22 + 6 + 12)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .onGloballyPositioned { lineX = it.positionInRoot().x.toInt() }
        ) {
            HorizontalDivider(
                color = DividerGrey,
                thickness = 1.dp,
                modifier = Modifier.align(Alignment.Center)
            )
            val labelX = if (activeTab == 0) x0 else x1
            val labelW = if (activeTab == 0) w0 else w1
            if (labelW > 0) {
                val waveW = with(density) { 142.dp.roundToPx() }
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset { IntOffset(labelX + labelW / 2 - waveW / 2 - lineX, 0) }
                        .width(142.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(TextPrimary)
                )
            }
        }
    }
}

@Composable
private fun TabLabel(
    text: String,
    active: Boolean,
    onPosition: (LayoutCoordinates) -> Unit,
    onClick: () -> Unit
) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        color = if (active) TextPrimary else TextGray,
        modifier = Modifier
            .clickable { onClick() }
            .onGloballyPositioned(onPosition)
    )
}

// ===================== Вкладка «Объекты» =====================

@Composable
private fun ObjectsTab(
    modifier: Modifier = Modifier,
    properties: List<MyPropertyItem>,
    selectedYear: Int,
    selectedMonth: Int,
    onMonthClick: () -> Unit,
    onPropertyClick: (String) -> Unit,
    onAddTenant: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item(key = "month_dropdown") {
            MonthDropdown(
                text = monthName(selectedMonth) + " " + selectedYear,
                onClick = onMonthClick
            )
        }
        items(properties, key = { it.id }) { property ->
            ObjectCard(
                property = property,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                onClick = { onPropertyClick(property.id) },
                onAddTenant = { onAddTenant(property.id) }
            )
        }
    }
}

/** Дропдаун «Сентябрь 2026»: текст 15/600 + стрелка вниз, высота 36. */
@Composable
private fun MonthDropdown(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = TextPrimary
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            painter = painterResource(R.drawable.ic_calendar_arrow_down),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = TextPrimary
        )
    }
}

/** Карточка объекта (2 вариант): белая с обводкой #DBDBDB r20, pad 10,
 *  шапка с типом аренды справа, задолженность при наличии, лента
 *  ближайшей загрузки, строка арендатора с шевроном. */
@Composable
private fun ObjectCard(
    property: MyPropertyItem,
    selectedYear: Int,
    selectedMonth: Int,
    onClick: () -> Unit,
    onAddTenant: () -> Unit
) {
    val daily = property.rentType == "посуточно"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, DividerGrey, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- Шапка: фото + имя/адрес, тип аренды справа ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF2F2F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Outlined.PhotoCamera),
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
                AsyncImage(
                    model = property.photoUrl,
                    contentDescription = property.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                // Название необязательно: если поле пустое, его слот занимает
                // адрес (Figma 3970-83270) — вторая строка с адресом не рисуется
                Text(
                    propertyTitle(property.name, property.address),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = TextPrimary
                )
                if (hasPropertyName(property.name)) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        property.address,
                        fontSize = 13.sp,
                        letterSpacing = (-0.4).sp,
                        color = TextGray
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(
                        if (daily) R.drawable.ic_renttype_clock else R.drawable.ic_renttype_calendar
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (daily) "Посуточно" else "Помесячно",
                    fontSize = 13.sp,
                    letterSpacing = (-0.4).sp,
                    color = TextGray
                )
            }
        }

        HorizontalDivider(color = DividerGrey, thickness = 1.dp)

        // ---- Задолженность — только при наличии ----
        if (property.overdue && property.overdueAmount > 0.0) {
            Text(
                "Задолженность " +
                    String.format(java.util.Locale.US, "%,.0f ₽", property.overdueAmount).replace(',', ' '),
                fontSize = 13.sp,
                letterSpacing = (-0.4).sp,
                color = OverdueRed
            )
        }

        // ---- Лента загрузки: месяцы (12 вперёд) или дни выбранного месяца ----
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (daily) "Открыть шахматку" else "Ближайшая загрузка",
                fontSize = 13.sp,
                letterSpacing = (-0.4).sp,
                color = TextGray
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val cells = if (daily) {
                    val ym = YearMonth.of(selectedYear, selectedMonth)
                    (1..ym.lengthOfMonth()).map { day ->
                        LocalDate.of(selectedYear, selectedMonth, day)
                    }
                } else {
                    val start = YearMonth.of(selectedYear, selectedMonth)
                    (0 until 12).map { start.plusMonths(it.toLong()).atDay(1) }
                }
                cells.forEach { date ->
                    LoadCell(
                        label = if (daily) date.dayOfMonth.toString() else MonthShort[date.monthValue - 1],
                        state = property.statusAt(date)
                    )
                }
            }
        }

        // ---- Арендатор · до N (или «не добавлен») ----
        // Своя кликабельная зона (перехватывает тап у карточки):
        // арендатора нет → «Добавить арендатора»; есть → пока заглушка
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.let {
                if (property.tenantInfo == null) it.clickable { onAddTenant() } else it
            }
        ) {
            val tenantLine = property.tenantInfo?.let { tenant ->
                // Срок аренды — то же правило, что в карточке объекта:
                // поле объекта, иначе последняя бронь (util/Lease.kt). ДД.ММ.ГГГГ
                val lastEnd = leaseEnd(property.rentEndDate, property.bookings.map { it.end })
                if (lastEnd != null) "$tenant · до ${lastEnd.format(DD_MM_YYYY)}"
                else tenant
            } ?: "Арендатор не добавлен · готова к аренде"
            Text(
                tenantLine,
                fontSize = 13.sp,
                letterSpacing = (-0.4).sp,
                color = TextGray,
                modifier = Modifier.weight(1f)
            )
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { rotationZ = -90f }
            )
        }
    }
}

/** Ячейка ленты загрузки: 48×38 r4; занято #E5F2E7/#2F7D4D, свободно
 *  #EFEFEF/#717171, просрочка — красная рамка и текст (2949:45807). */
@Composable
private fun LoadCell(label: String, state: String) {
    val busy = state != "free"
    val expired = state == "expired"
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (busy) CellBusy else CellFree)
            .then(
                if (expired) Modifier.border(1.dp, OverdueRed, RoundedCornerShape(4.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.4).sp,
            color = when {
                expired -> OverdueRed
                busy -> BusyGreen
                else -> TextGray
            }
        )
    }
}

// ===================== Вкладка «Шахматка» =====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChessTab(
    modifier: Modifier = Modifier,
    properties: List<MyPropertyItem>,
    viewMode: ViewMode,
    selectedYear: Int,
    selectedMonth: Int,
    onModeChange: (ViewMode) -> Unit,
    onPeriodClick: () -> Unit,
    onRangeSelected: (String, LocalDate, LocalDate, Boolean) -> Unit,
    onOverdueMark: (String) -> Unit,
    // Подсказка «Добавить арендатора» приходит из ViewModel: живёт, пока у
    // объекта есть покраски и арендатор не прикреплён (тапы её не сбрасывают)
    attachSuggestion: AttachSuggestion?,
    // Новый выбор в шахматке сбрасывает кнопку-подсказку (макет: «Кнопка
    // появляется при выборе периода») — она вернётся после следующей покраски
    onSelectionStarted: () -> Unit,
    // Долгий тап по ЗАНЯТОМУ (зелёному) дню — быстрый старт нового периода
    // с этой даты: переезд «А съезд до 12, Б заезд после 14»
    onAttachFromDate: (String, LocalDate) -> Unit,
    onAttachTenant: (String, LocalDate, LocalDate) -> Unit
) {
    // Выбор диапазона: строка объекта + индекс первой ячейки; по второму
    // тапу — покраска/стирание. Градиентная кнопка «Добавить арендатора»
    // живёт до привязки арендатора (см. MyPropertiesViewModel.attachSuggestion)
    var selPropertyId by remember { mutableStateOf<String?>(null) }
    var selStart by remember { mutableStateOf<Int?>(null) }

    val currentOnAttachTenant by rememberUpdatedState(onAttachTenant)
    val haptics = LocalHapticFeedback.current

    // Снять выделение: тап вне сетки (пустое место, сегменты, тулбар) или
    // системная «Назад». Раньше выделение снималось только вторым тапом по той
    // же клетке — а он уже применял правку (и мог стереть день арендатора).
    fun clearSelection() {
        selPropertyId = null
        selStart = null
    }

    androidx.activity.compose.BackHandler(enabled = selStart != null) { clearSelection() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { clearSelection() }
    ) {
        // ---- Сегмент «Месяцы / Сутки» ----
        // 12 сверху (от табов) и 20 снизу (до дропдауна) — макет 2949:45028
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(CellFree),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChessSegment(
                    label = "Месяцы",
                    active = viewMode == ViewMode.MONTHS,
                    left = true,
                    onClick = { onModeChange(ViewMode.MONTHS) }
                )
                ChessSegment(
                    label = "Сутки",
                    active = viewMode == ViewMode.DAYS,
                    left = false,
                    onClick = { onModeChange(ViewMode.DAYS) }
                )
            }
        }

        // ---- Дропдаун периода: год (месяцы) или месяц (сутки) ----
        Row(
            // 12 до таблицы — макет 2949:45028
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonthDropdown(
                text = if (viewMode == ViewMode.MONTHS) selectedYear.toString()
                else monthName(selectedMonth) + " " + selectedYear,
                onClick = onPeriodClick
            )
        }

        // ---- Таблица: закреплённая колонка + горизонтальная сетка ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            val sharedScroll = rememberScrollState()
            val dates = remember(viewMode, selectedYear, selectedMonth) {
                if (viewMode == ViewMode.MONTHS) {
                    (1..12).map { LocalDate.of(selectedYear, it, 1) }
                } else {
                    val ym = YearMonth.of(selectedYear, selectedMonth)
                    (1..ym.lengthOfMonth()).map { LocalDate.of(selectedYear, selectedMonth, it) }
                }
            }

            // Заголовочная строка: «Объект» + месяцы/дни (11/500)
            ChessRowContainer(
                label = {
                    Box(
                        Modifier
                            .width(112.dp)
                            .padding(start = 20.dp)
                    ) {
                        Text(
                            "Объект",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp,
                            color = TextGray
                        )
                    }
                },
                cells = {
                    Row(
                        modifier = Modifier.horizontalScroll(sharedScroll),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dates.forEach { date ->
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 38.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (viewMode == ViewMode.MONTHS) MonthShort[date.monthValue - 1]
                                    else date.dayOfMonth.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.4).sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(6.dp))

            // Строки объектов
            properties.forEachIndexed { propertyIndex, property ->
                val states = remember(property, dates) { dates.map { property.statusAt(it) } }
                if (propertyIndex > 0) Spacer(Modifier.height(12.dp))
                val rowSelected = selPropertyId == property.id
                ChessRowContainer(
                    label = {
                        Column(
                            Modifier
                                .width(112.dp)
                                .padding(start = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                propertyTitle(property.name, property.address),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.4).sp,
                                color = TextPrimary
                            )
                            // Без названия адрес стоит в его слоте — вторая строка не нужна
                            if (hasPropertyName(property.name)) {
                                Text(
                                    property.address.substringBefore(',').trim(),
                                    fontSize = 9.5.sp,
                                    letterSpacing = (-0.2).sp,
                                    color = TextGray
                                )
                            }
                        }
                    },
                    cells = {
                        Row(
                            modifier = Modifier.horizontalScroll(sharedScroll),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            dates.forEachIndexed { index, date ->
                                val state = states.getOrNull(index) ?: "free"
                                val inSelection = rowSelected && selStart != null && index == selStart
                                ChessCell(
                                    state = state,
                                    highlighted = inSelection,
                                    onClick = {
                                        val start = selStart
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!rowSelected || start == null) {
                                            selPropertyId = property.id
                                            selStart = index
                                            onSelectionStarted()
                                        } else {
                                            val lo = minOf(start, index)
                                            val hi = maxOf(start, index)
                                            val remove = states.getOrNull(lo) == "fullness" ||
                                                states.getOrNull(lo) == "expired"
                                            onRangeSelected(
                                                property.id,
                                                dates[lo],
                                                dates[hi],
                                                remove
                                            )
                                            selPropertyId = null
                                            selStart = null
                                        }
                                    },
                                    onLongClick = {
                                        // Просроченный день — ручное гашение оплаты.
                                        // Занятый (зелёный) — только ДЕНЬ ВЫЕЗДА (последний
                                        // день периода; одиночный день тоже): именно в этот
                                        // день может заехать другой арендатор («А съезд до
                                        // 12, Б заезд после 14»). Средние дни — без реакции.
                                        if (state == "expired") onOverdueMark(property.id)
                                        else if (state == "fullness" && isCheckoutDay(property.bookings, date)) {
                                            onAttachFromDate(property.id, date)
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }


    // Кнопка «Добавить арендатора» — по макету (2949:45028, заметка 2950:45816):
    // появляется при выборе периода, стоит на белом фоне экрана с отступом
    // 20 по краям и 20 от таблицы, без серой подложки. Состояние — из ViewModel,
    // поэтому refresh и диалоги оплаты её не гасят; новый выбор её сбрасывает.
    attachSuggestion?.let { suggestion ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp)
        ) {
            com.rentmanager.app.ui.landlord.createproperty.GradientCtaButton(
                text = "Добавить арендатора",
                onClick = {
                    currentOnAttachTenant(suggestion.propertyId, suggestion.start, suggestion.end)
                }
            )
        }
    }
}

/** Дата — последний день какого-то периода аренды (день выезда). Одиночный
 *  день (start == end) тоже подходит: в день выезда на объект может заехать
 *  другой арендатор («А съезд до 12, Б заезд после 14»). */
private fun isCheckoutDay(bookings: List<BookingRange>, date: java.time.LocalDate): Boolean =
    bookings.any { it.end == date }

/** Строка таблицы: закреплённая подпись слева + прокручиваемые ячейки. */
@Composable
private fun ChessRowContainer(
    label: @Composable () -> Unit,
    cells: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        label()
        cells()
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ChessSegment(
    label: String,
    active: Boolean,
    left: Boolean,
    onClick: () -> Unit
) {
    // Половина трека: тёмная плашка 184×44 прижата к внешнему краю
    // с отступом 2 (2949-44886), неактивная подпись — по центру половины
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(vertical = 2.dp)
                .padding(
                    start = if (left) 2.dp else 0.dp,
                    end = if (left) 0.dp else 2.dp
                )
                .clip(RoundedCornerShape(100.dp))
                .then(if (active) Modifier.background(TextPrimary) else Modifier)
        )
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = if (active) White else TextGray
        )
    }
}

/** Ячейка шахматки: 48×38 r4; выбор — рамка графитом. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChessCell(
    state: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (state == "free") CellFree else CellBusy)
            .then(
                when {
                    highlighted -> Modifier.border(1.5.dp, TextPrimary, RoundedCornerShape(4.dp))
                    state == "expired" -> Modifier.border(1.dp, OverdueRed, RoundedCornerShape(4.dp))
                    else -> Modifier
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}

// ===================== Нижний таббар =====================

/** Компактный таббар (2 вариант): #EDEDED, три таба 24+2+9.5/400. */
@Composable
private fun CompactTabBar(
    onFinanceClick: () -> Unit,
    onCreateProperty: () -> Unit,
    onHomeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color(0xFFEDEDED))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CompactTab(
                iconRes = R.drawable.ic_myprops_finance,
                label = "Финансы",
                onClick = onFinanceClick
            )
            CompactTab(
                iconRes = R.drawable.ic_plus_circle_graphite,
                label = "Создать объект",
                onClick = onCreateProperty
            )
            CompactTab(
                iconRes = R.drawable.ic_myprops_home,
                label = "На главную",
                onClick = onHomeClick
            )
        }
    }
}

@Composable
private fun CompactTab(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .width(102.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        // Многоцветные иконки макета (тёмный круг + белый плюс) — без tint
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
        Text(
            label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.2).sp,
            color = TextPrimary
        )
    }
}

// ===================== Общие шиты =====================

/** Простой лист выбора одной опции (сортировка). */
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

/**
 * Шит «Как работает шахматка» — жесты неочевидны: тап = выделение, второй тап =
 * правка, долгие тапы = прикрепление арендатора и отметка оплаты.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChessHelpSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Как работает шахматка",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            ChessHelpRow(
                "Нажатие по дню",
                "Отметьте занятые дни: первое нажатие — начало, второе — конец периода"
            )
            ChessHelpRow(
                "Нажатие по занятому периоду",
                "Второе нажатие снимает отметку — дни становятся свободными"
            )
            ChessHelpRow(
                "Долгое нажатие по зелёному дню",
                "Откроется «Добавить арендатора» с этой даты — если день последний в периоде (день выезда)"
            )
            ChessHelpRow(
                "Долгое нажатие по красному дню",
                "Отметить просроченный платёж полученным"
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "День выезда — последний день периода: в этот день предыдущий арендатор " +
                    "уже уехал, а следующий может заехать (А съезд до 12:00, Б заезд после 14:00).",
                fontSize = 13.sp,
                lineHeight = 15.7.sp,
                letterSpacing = (-0.4).sp,
                color = TextGray
            )
        }
    }
}

/** Строка подсказки жеста: заголовок 15/600 + пояснение 13/400. */
@Composable
private fun ChessHelpRow(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            fontSize = 13.sp,
            lineHeight = 15.7.sp,
            letterSpacing = (-0.4).sp,
            color = TextGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionSheet(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(options) { option ->
                    PickerRow(
                        label = option,
                        isSelected = option == selected,
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }
}

/** Шит выбора года (Месяцы) или месяца (Сутки) — как в прежней шахматке. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodPickerSheet(
    viewMode: ViewMode,
    selectedYear: Int,
    selectedMonth: Int,
    onSelect: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val currentYear = LocalDate.now().year

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (viewMode == ViewMode.MONTHS) "Выберите год" else "Выберите месяц",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (viewMode == ViewMode.MONTHS) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(20) { index ->
                        val year = currentYear + index
                        PickerRow(
                            label = year.toString(),
                            isSelected = year == selectedYear,
                            onClick = {
                                onSelect(year, selectedMonth)
                                onDismiss()
                            }
                        )
                    }
                }
            } else {
                val years = listOf(currentYear, currentYear + 1)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    years.forEachIndexed { yearIndex, year ->
                        item(key = "header_$year") {
                            Text(
                                text = year.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF8E8E93),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = if (yearIndex == 0) 0.dp else 12.dp,
                                        bottom = 4.dp
                                    )
                            )
                        }
                        items(12, key = { monthIndex -> "month_${year}_$monthIndex" }) { monthIndex ->
                            val month = monthIndex + 1
                            PickerRow(
                                label = MonthNames[monthIndex],
                                isSelected = year == selectedYear && month == selectedMonth,
                                onClick = {
                                    onSelect(year, month)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Text(
                text = "✓",
                fontSize = 16.sp,
                color = Color(0xFF007AFF)
            )
        }
    }
    HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
}
