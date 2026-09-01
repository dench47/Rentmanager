package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.rentmanager.app.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

// Цвета из Figma (node 2183:9531)
private val White = Color.White
private val TextPrimary = Color(0xFF212121)
private val TextGray = Color(0xFF727272)
private val ToggleBg = Color(0xFFEFEFEF)

// Шахматка (ячейки 44×39, r=4, gap=4)
private val CellFullBg = Color(0xFFCFDECB)
private val CellFullStroke = Color(0xFF66A256)
private val CellExpiredStroke = Color(0xFFFF4249)
private val CellFreeBg = Color(0xFFEFEFEF)
private val CellFreeStroke = Color(0xFF727272)

private val NameTextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp)
private val AddressTextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp)
private val PeriodMonthTextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp)
private val PeriodYearTextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp)

// Названия месяцев (полные)
private val MonthNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

private fun monthName(month: Int): String =
    MonthNames.getOrElse(month - 1) { "" }

private fun weekdayAbbr(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.MONDAY -> "ПН"
    DayOfWeek.TUESDAY -> "ВТ"
    DayOfWeek.WEDNESDAY -> "СР"
    DayOfWeek.THURSDAY -> "ЧТ"
    DayOfWeek.FRIDAY -> "ПТ"
    DayOfWeek.SATURDAY -> "СБ"
    DayOfWeek.SUNDAY -> "ВС"
}

@Composable
fun MyPropertiesScreen(
    onPropertyClick: (String) -> Unit,
    onCreateProperty: () -> Unit,
    onBack: () -> Unit,
    onFinanceClick: () -> Unit,
    onWriteClick: () -> Unit,
    viewModel: MyPropertiesViewModel = viewModel()
) {
    val properties by viewModel.visibleProperties.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Обновление списка при возврате на экран / из фона
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var pickerPropertyId by remember { mutableStateOf<String?>(null) }

    // Общий кэш раскладки текста шапки: мерим уникальные подписи один раз.
    val textMeasurer = rememberTextMeasurer(cacheSize = 256)

    val cameraPainter = rememberVectorPainter(Icons.Outlined.PhotoCamera)

    // Прогрев кэша фото: грузим превью заранее, чтобы при скролле карточки не мигали пустым квадратом.
    val prefetchContext = LocalContext.current
    val prefetchDensity = LocalDensity.current
    LaunchedEffect(properties) {
        val photoSizePx = with(prefetchDensity) { 40.dp.roundToPx() }
        val loader = prefetchContext.imageLoader
        properties.forEach { property ->
            property.photoUrl?.let { url ->
                loader.enqueue(
                    ImageRequest.Builder(prefetchContext)
                        .data(url)
                        .size(photoSizePx)
                        .build()
                )
            }
        }
    }

    val headerLayoutCache = remember(properties, textMeasurer) {
        val currentYear = LocalDate.now().year
        HeaderLayoutCache(
            nameAddress = properties.associate {
                it.id to (textMeasurer.measure(it.name, NameTextStyle) to textMeasurer.measure(it.address, AddressTextStyle))
            },
            monthNames = MonthNames.associate { it to textMeasurer.measure(it, PeriodMonthTextStyle) },
            years = (currentYear..(currentYear + 20)).associate {
                it.toString() to textMeasurer.measure(it.toString(), PeriodYearTextStyle)
            }
        )
    }

    Scaffold(
        containerColor = White,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            BottomTabBar(
                onFinanceClick = onFinanceClick,
                onCreateProperty = onCreateProperty,
                onWriteClick = onWriteClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(White)
        ) {
            MyPropertiesNavBar(onBack = onBack)

            ViewModeToggle(
                currentMode = viewMode,
                onModeChange = { viewModel.setViewMode(it) }
            )

            DisplayModeToggle(
                currentMode = displayMode,
                onModeChange = { viewModel.setDisplayMode(it) }
            )

            errorMessage?.let {
                Text(it, color = Color(0xFFE53935), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }

            // Отображение: карточки (независимая шахматка) или общая таблица
            when (displayMode) {
                DisplayMode.CARDS -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(properties, key = { it.id }) { property ->
                        PropertyCard(
                            property = property,
                            viewMode = viewMode,
                            cameraPainter = cameraPainter,
                            headerLayoutCache = headerLayoutCache,
                            onClick = { onPropertyClick(property.id) },
                            onPeriodClick = { pickerPropertyId = property.id },
                            onRangeSelected = { start, end, isRemove ->
                                if (isRemove) viewModel.deleteBookings(property.id, start, end)
                                else viewModel.saveBooking(property.id, start, end)
                            }
                        )
                    }
                }
                DisplayMode.TABLE -> ScheduleTable(
                    properties = properties,
                    viewMode = viewMode,
                    onRangeSelected = { propertyId, start, end, isRemove ->
                        if (isRemove) viewModel.deleteBookings(propertyId, start, end)
                        else viewModel.saveBooking(propertyId, start, end)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }

    pickerPropertyId?.let { id ->
        val pickerProperty = properties.find { it.id == id }
        if (pickerProperty != null) {
            PeriodPickerSheet(
                viewMode = viewMode,
                selectedYear = pickerProperty.year,
                selectedMonth = pickerProperty.month,
                onSelect = { year, month ->
                    viewModel.selectPeriod(id, year, month)
                    pickerPropertyId = null
                },
                onDismiss = { pickerPropertyId = null }
            )
        }
    }
}
@Composable
private fun MyPropertiesNavBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Левая часть: стрелка + заголовок
        Row(
            modifier = Modifier.clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }
            Text(
                "Моя недвижимость",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                letterSpacing = (-0.3).sp
            )
        }

        // Правая часть: поиск + transfer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { /* TODO: поиск */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Поиск",
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { /* TODO: transfer */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = "Transfer",
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Фон — светло-серая капсула
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(ToggleBg)
        )

        // Сегменты
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToggleSegment(
                label = "Месяцы",
                active = currentMode == ViewMode.MONTHS,
                onClick = { onModeChange(ViewMode.MONTHS) }
            )
            ToggleSegment(
                label = "Сутки",
                active = currentMode == ViewMode.DAYS,
                onClick = { onModeChange(ViewMode.DAYS) }
            )
        }
    }
}

@Composable
private fun DisplayModeToggle(
    currentMode: DisplayMode,
    onModeChange: (DisplayMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(ToggleBg)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToggleSegment(
                label = "Карточки",
                active = currentMode == DisplayMode.CARDS,
                onClick = { onModeChange(DisplayMode.CARDS) }
            )
            ToggleSegment(
                label = "Таблица",
                active = currentMode == DisplayMode.TABLE,
                onClick = { onModeChange(DisplayMode.TABLE) }
            )
        }
    }
}

@Composable
private fun RowScope.ToggleSegment(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(100.dp))
            .then(
                if (active) Modifier.background(TextPrimary)
                else Modifier.background(Color.Transparent)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.4).sp,
            color = if (active) White else TextGray
        )
    }
}
@Composable
private fun PropertyCard(
    property: MyPropertyItem,
    viewMode: ViewMode,
    cameraPainter: Painter,
    headerLayoutCache: HeaderLayoutCache,
    onClick: () -> Unit,
    onPeriodClick: () -> Unit,
    onRangeSelected: (LocalDate, LocalDate, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
            .logMeasure("PropertyCard"),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        // Шапка карточки
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Фото 40×40 с borderRadius 8 (нет фото — заглушка-фотоаппарат)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF2F2F7)),
                    contentAlignment = Alignment.Center
                ) {
                    // Заглушка видна всегда: пока фото грузится, при ошибке или если фото нет.
                    Icon(
                        painter = cameraPainter,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(20.dp)
                    )
                    if (property.photoUrl != null) {
                        val context = LocalContext.current
                        val density = LocalDensity.current
                        val photoSizePx = with(density) { 40.dp.roundToPx() }
                        val imageRequest = remember(property.photoUrl, photoSizePx) {
                            ImageRequest.Builder(context)
                                .data(property.photoUrl)
                                .size(photoSizePx)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = property.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Название + адрес (рисуем из кэша раскладки)
                val nameAddress = headerLayoutCache.nameAddress[property.id]
                if (nameAddress != null) {
                    val (nameLayout, addressLayout) = nameAddress
                    val density = LocalDensity.current
                    Canvas(
                        modifier = Modifier.size(
                            width = with(density) { maxOf(nameLayout.size.width, addressLayout.size.width).toDp() },
                            height = with(density) { (nameLayout.size.height + addressLayout.size.height).toDp() }
                        )
                    ) {
                        drawText(nameLayout, color = TextPrimary, topLeft = Offset(0f, 0f))
                        drawText(addressLayout, color = TextGray, topLeft = Offset(0f, nameLayout.size.height.toFloat()))
                    }
                }
            }

            // Селектор периода (год / месяц+год)
            PeriodSelector(
                viewMode = viewMode,
                selectedYear = property.year,
                selectedMonth = property.month,
                headerLayoutCache = headerLayoutCache,
                onClick = onPeriodClick
            )
        }

        // Шахматка загруженности
        ScheduleRow(
            property = property,
            viewMode = viewMode,
            year = property.year,
            month = property.month,
            onRangeSelected = onRangeSelected
        )
    }
}

private val MonthAbbrevLabels = listOf("ЯНВ", "ФЕВ", "МАР", "АПР", "МАЙ", "ИЮН", "ИЮЛ", "АВГ", "СЕН", "ОКТ", "НОЯ", "ДЕК")

private class HeaderLayoutCache(
    val nameAddress: Map<String, Pair<TextLayoutResult, TextLayoutResult>>,
    val monthNames: Map<String, TextLayoutResult>,
    val years: Map<String, TextLayoutResult>
)

@Composable
private fun ScheduleRow(
    property: MyPropertyItem,
    viewMode: ViewMode,
    year: Int,
    month: Int,
    onRangeSelected: (LocalDate, LocalDate, Boolean) -> Unit
) {
    val cells = remember(viewMode, year, month, property) {
        buildCells(viewMode, year, month, property)
    }
    var selectionStart by remember { mutableStateOf<Int?>(null) }
    var selectionEnd by remember { mutableStateOf<Int?>(null) }
    var selectionRemove by remember { mutableStateOf(false) }
    val currentOnRangeSelected by rememberUpdatedState(onRangeSelected)
    val cellPitchPx = with(LocalDensity.current) { (44.dp + 4.dp).toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(39.dp)
            .horizontalScroll(rememberScrollState())
            .logMeasure("ScheduleRow"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(39.dp)
                .pointerInput(cells) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (cells.dates.isNotEmpty()) {
                                val index = (offset.x / cellPitchPx).toInt()
                                    .coerceIn(0, cells.dates.lastIndex)
                                val start = selectionStart
                                if (start == null) {
                                    selectionStart = index
                                    selectionEnd = index
                                    selectionRemove = cells.states.getOrNull(index) == "fullness"
                                } else {
                                    val lo = minOf(start, index)
                                    val hi = maxOf(start, index)
                                    val startDate = cells.dates.getOrNull(lo)
                                    val endDate = cells.dates.getOrNull(hi)
                                    if (startDate != null && endDate != null) {
                                        currentOnRangeSelected(startDate, endDate, selectionRemove)
                                    }
                                    selectionStart = null
                                    selectionEnd = null
                                }
                            }
                        }
                    )
                }
        ) {
            AndroidView(
                factory = { context -> ScheduleGridView(context) },
                modifier = Modifier.height(39.dp),
                update = { view ->
                    view.setData(cells.labels, cells.topLabels, cells.states, viewMode == ViewMode.DAYS)
                    view.setSelection(selectionStart, selectionEnd)
                }
            )
        }
    }
}

private fun cellColors(state: String): Triple<Color, Color, Color> = when (state) {
    "expired" -> Triple(CellFullBg, CellExpiredStroke, CellExpiredStroke)
    "free" -> Triple(CellFreeBg, CellFreeStroke, CellFreeStroke)
    else -> Triple(CellFullBg, CellFullStroke, TextPrimary)
}

// TEMP: диагностика времени measure (удалить после профилирования)
private fun Modifier.logMeasure(tag: String): Modifier = layout { measurable, constraints ->
    val start = System.nanoTime()
    val placeable = measurable.measure(constraints)
    val ms = (System.nanoTime() - start) / 1_000_000.0
    if (ms > 0.5) android.util.Log.d("Bench", "$tag measure = $ms ms")
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private data class ScheduleCells(
    val labels: List<String>,
    val topLabels: List<String?>,
    val states: List<String>,
    val dates: List<LocalDate>
)

private fun buildCells(
    viewMode: ViewMode,
    year: Int,
    month: Int,
    property: MyPropertyItem
): ScheduleCells {
    val today = LocalDate.now()
    return when (viewMode) {
        ViewMode.MONTHS -> {
            // Календарный год: текущий год — с текущего месяца по декабрь,
            // будущие годы — полный ЯНВ..ДЕК.
            val startMonth = if (year == today.year) today.monthValue else 1
            val months = (startMonth..12).toList()
            ScheduleCells(
                labels = months.map { MonthAbbrevLabels[it - 1] },
                topLabels = List(months.size) { null },
                states = months.map { m -> property.statusAt(LocalDate.of(year, m, 1)) },
                dates = months.map { m -> LocalDate.of(year, m, 1) }
            )
        }
        ViewMode.DAYS -> {
            // С текущего дня (в текущем месяце), прошлых дней нет.
            val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
            val startDay =
                if (year == today.year && month == today.monthValue) today.dayOfMonth else 1
            val days = (startDay..daysInMonth).toList()
            ScheduleCells(
                labels = days.map { it.toString() },
                topLabels = days.map { day -> weekdayAbbr(LocalDate.of(year, month, day).dayOfWeek) },
                states = days.map { day -> property.statusAt(LocalDate.of(year, month, day)) },
                dates = days.map { day -> LocalDate.of(year, month, day) }
            )
        }
    }
}

@Composable
private fun BottomTabBar(
    onFinanceClick: () -> Unit,
    onCreateProperty: () -> Unit,
    onWriteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color(0x99EDEDED))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(80.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(
                iconRes = R.drawable.ic_menu_finance,
                label = "Финансы",
                onClick = onFinanceClick
            )
            TabItem(
                iconRes = R.drawable.ic_plus_circle,
                label = "Создать объект",
                iconSize = 30.dp,
                onClick = onCreateProperty
            )
            TabItem(
                iconRes = R.drawable.ic_email,
                label = "Написать",
                onClick = onWriteClick
            )
        }
    }
}

@Composable
private fun TabItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    iconSize: Dp = 24.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val contentColor = if (isPressed) White else TextPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(84.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(if (isPressed) TextPrimary else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(6.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(iconSize),
            tint = contentColor
        )
        Text(
            label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.4).sp,
            color = contentColor
        )
    }
}

@Composable
private fun PeriodSelector(
    viewMode: ViewMode,
    selectedYear: Int,
    selectedMonth: Int,
    headerLayoutCache: HeaderLayoutCache,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val density = LocalDensity.current
        if (viewMode == ViewMode.DAYS) {
            val monthLayout = headerLayoutCache.monthNames.getValue(monthName(selectedMonth))
            val yearLayout = headerLayoutCache.years.getValue(selectedYear.toString())
            val widthPx = maxOf(monthLayout.size.width, yearLayout.size.width)
            val gapPx = with(density) { 1.dp.roundToPx() }
            Canvas(
                modifier = Modifier.size(
                    width = with(density) { widthPx.toDp() },
                    height = with(density) { (monthLayout.size.height + gapPx + yearLayout.size.height).toDp() }
                )
            ) {
                drawText(
                    monthLayout,
                    color = TextPrimary,
                    topLeft = Offset((widthPx - monthLayout.size.width).toFloat(), 0f)
                )
                drawText(
                    yearLayout,
                    color = TextPrimary,
                    topLeft = Offset(
                        (widthPx - yearLayout.size.width).toFloat(),
                        (monthLayout.size.height + gapPx).toFloat()
                    )
                )
            }
        } else {
            val yearLayout = headerLayoutCache.years.getValue(selectedYear.toString())
            Canvas(
                modifier = Modifier.size(
                    width = with(density) { yearLayout.size.width.toDp() },
                    height = with(density) { yearLayout.size.height.toDp() }
                )
            ) {
                drawText(yearLayout, color = TextPrimary, topLeft = Offset(0f, 0f))
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Выбрать период",
            modifier = Modifier.size(24.dp),
            tint = TextPrimary
        )
    }
}

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
                color = Color(0xFF151515),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (viewMode == ViewMode.MONTHS) {
                // 20 лет вперёд, начиная с текущего
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
                // Ближайшие 2 года × 12 месяцев
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
            color = Color(0xFF151515),
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



