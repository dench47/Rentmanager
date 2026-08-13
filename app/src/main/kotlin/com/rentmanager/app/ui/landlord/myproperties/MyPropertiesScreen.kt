package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

// Цвета из Figma (node 2183:9531)
private val White = Color.White
private val TextPrimary = Color(0xFF212121)
private val TextGray = Color(0xFF727272)
private val ToggleBg = Color(0xFFEFEFEF)
private val TabBg = Color(0xFFEDEDED)

// Шахматка (ячейки 44×39, r=4, gap=4)
private val CellFullBg = Color(0xFFCFDECB)
private val CellFullStroke = Color(0xFF66A256)
private val CellExpiredStroke = Color(0xFFFF4249)
private val CellFreeBg = Color(0xFFEFEFEF)
private val CellFreeStroke = Color(0xFF727272)

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
    val properties by viewModel.properties.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    var pickerPropertyId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = White,
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

            // Список объектов
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(properties, key = { it.id }) { property ->
                    PropertyCard(
                        property = property,
                        viewMode = viewMode,
                        onClick = { onPropertyClick(property.id) },
                        onPeriodClick = { pickerPropertyId = property.id }
                    )
                }
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
    onClick: () -> Unit,
    onPeriodClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
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
                // Фото 40×40 с borderRadius 8
                if (property.photoUrl != null) {
                    AsyncImage(
                        model = property.photoUrl,
                        contentDescription = property.name,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.mock_avatar_legend),
                        contentDescription = property.name,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Название + адрес
                Column {
                    Text(
                        property.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        property.address,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextGray,
                        letterSpacing = (-0.4).sp
                    )
                }
            }

            // Селектор периода (год / месяц+год)
            PeriodSelector(
                viewMode = viewMode,
                selectedYear = property.year,
                selectedMonth = property.month,
                onClick = onPeriodClick
            )
        }

        // Шахматка загруженности
        ScheduleRow(
            schedule = property.schedule,
            viewMode = viewMode,
            year = property.year,
            month = property.month
        )
    }
}

@Composable
private fun ScheduleRow(
    schedule: List<String>,
    viewMode: ViewMode,
    year: Int,
    month: Int
) {
    val monthsLabels = listOf("СЕН", "ОКТ", "НОЯ", "ДЕК", "ЯНВ", "ФЕВ", "МАР", "АПР", "МАЙ", "ИЮНЬ", "ИЮЛЬ", "АВГ")

    val labels: List<String>
    val topLabels: List<String?>
    val states: List<String>
    when (viewMode) {
        ViewMode.MONTHS -> {
            labels = monthsLabels
            topLabels = List(monthsLabels.size) { null }
            states = schedule
        }
        ViewMode.DAYS -> {
            val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
            labels = (1..daysInMonth).map { it.toString() }
            topLabels = (1..daysInMonth).map { day ->
                weekdayAbbr(LocalDate.of(year, month, day).dayOfWeek)
            }
            states = daySchedule(daysInMonth)
        }
    }

    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.take(states.size).forEachIndexed { index, label ->
            ScheduleCell(
                label = label,
                state = states[index],
                topLabel = topLabels[index]
            )
        }
    }
}

private fun daySchedule(days: Int): List<String> = (1..days).map { day ->
    when {
        day % 7 == 1 -> "expired"
        day % 7 == 2 -> "free"
        else -> "fullness"
    }
}

@Composable
private fun ScheduleCell(label: String, state: String, topLabel: String? = null) {
    val bg: Color
    val stroke: Color
    val textColor: Color
    when (state) {
        "expired" -> {
            bg = CellFullBg
            stroke = CellExpiredStroke
            textColor = CellExpiredStroke
        }
        "free" -> {
            bg = CellFreeBg
            stroke = CellFreeStroke
            textColor = CellFreeStroke
        }
        else -> {
            bg = CellFullBg
            stroke = CellFullStroke
            textColor = TextPrimary
        }
    }

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 39.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (topLabel != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-2).dp)
            ) {
                Text(
                    topLabel,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.4).sp,
                    color = textColor
                )
                Text(
                    label,
                    fontSize = 14.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.4).sp,
                    color = textColor
                )
            }
        } else {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
                color = textColor,
                textAlign = TextAlign.Center
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
            .background(TabBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Финансы
            TabBarPill(
                iconRes = R.drawable.ic_card_finance,
                label = "Финансы",
                onClick = onFinanceClick
            )

            // Создать объект
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onCreateProperty() }
                    .padding(horizontal = 15.dp, vertical = 8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus_circle),
                    contentDescription = "Создать объект",
                    modifier = Modifier.size(30.dp),
                    tint = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Создать объект",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.4).sp,
                    color = TextPrimary
                )
            }

            // Написать
            TabBarPill(
                iconRes = R.drawable.ic_email,
                label = "Написать",
                onClick = onWriteClick
            )
        }
    }
}

@Composable
private fun TabBarPill(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, TextPrimary, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = TextPrimary
        )
        Text(
            label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.4).sp,
            color = TextPrimary
        )
    }
}

@Composable
private fun PeriodSelector(
    viewMode: ViewMode,
    selectedYear: Int,
    selectedMonth: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (viewMode == ViewMode.DAYS) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    monthName(selectedMonth),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    selectedYear.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    letterSpacing = (-0.4).sp
                )
            }
        } else {
            Text(
                selectedYear.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                letterSpacing = (-0.4).sp
            )
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



