package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

// Цвета ячеек (совпадают с шахматкой в карточках)
private val TableCellFullBg = Color(0xFFCFDECB)
private val TableCellFullStroke = Color(0xFF66A256)
private val TableCellExpiredStroke = Color(0xFFFF4249)
private val TableCellFreeBg = Color(0xFFEFEFEF)
private val TableCellFreeStroke = Color(0xFF727272)

private val TableMonthAbbrev = listOf(
    "ЯНВ", "ФЕВ", "МАР", "АПР", "МАЙ", "ИЮН",
    "ИЮЛ", "АВГ", "СЕН", "ОКТ", "НОЯ", "ДЕК"
)

/** Общая ось дат: месяцы — 20 лет (240), сутки — 2 года (730). */
private fun buildTimeline(viewMode: ViewMode): List<LocalDate> = when (viewMode) {
    ViewMode.MONTHS -> {
        val start = YearMonth.from(LocalDate.now())
        (0 until 240).map { start.plusMonths(it.toLong()).atDay(1) }
    }
    ViewMode.DAYS -> {
        val today = LocalDate.now()
        (0 until 730).map { today.plusDays(it.toLong()) }
    }
}

private fun tableColors(state: String): Pair<Color, Color> = when (state) {
    "expired" -> TableCellFullBg to TableCellExpiredStroke
    "free" -> TableCellFreeBg to TableCellFreeStroke
    else -> TableCellFullBg to TableCellFullStroke
}

private fun headerLabel(date: LocalDate, viewMode: ViewMode): String = when (viewMode) {
    ViewMode.MONTHS -> TableMonthAbbrev[date.monthValue - 1]
    ViewMode.DAYS -> date.dayOfMonth.toString()
}

/**
 * Общая таблица занятости (вариант «как у конкурентов»):
 * одна шкала дат на всех объектов, синхронный горизонтальный скролл,
 * закреплённая колонка с названием объекта.
 */
@Composable
fun ScheduleTable(
    properties: List<MyPropertyItem>,
    viewMode: ViewMode,
    onRangeSelected: (String, LocalDate, LocalDate, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onOverdueMark: (String) -> Unit = {}
) {
    val dates = remember(viewMode) { buildTimeline(viewMode) }
    val horizontalScroll = rememberScrollState()
    val yearGroups = remember(dates) { dates.groupBy { it.year }.toSortedMap() }

    // Выбор диапазона: первый тап — старт, второй — завершение и сохранение/снятие.
    var selPropertyId by remember { mutableStateOf<String?>(null) }
    var selStart by remember { mutableStateOf<Int?>(null) }
    var selEnd by remember { mutableStateOf<Int?>(null) }
    var selRemove by remember { mutableStateOf(false) }
    val currentOnRangeSelected by rememberUpdatedState(onRangeSelected)
    val currentOnOverdueMark by rememberUpdatedState(onOverdueMark)
    val cellPitchPx = with(LocalDensity.current) { (44.dp + 4.dp).toPx() }

    Column(modifier = modifier.fillMaxWidth()) {
        // Лента годов (только для «Месяцев»)
        if (viewMode == ViewMode.MONTHS) {
            Row(Modifier.fillMaxWidth().height(18.dp)) {
                Box(Modifier.width(140.dp).height(18.dp).background(Color(0xFFF7F7F7)))
                Row(
                    Modifier.weight(1f).height(18.dp).horizontalScroll(horizontalScroll)
                ) {
                    yearGroups.entries.forEachIndexed { idx, (year, monthsInYear) ->
                        if (idx > 0) Spacer(Modifier.width(4.dp))
                        val groupWidthDp = (monthsInYear.size * 44 + (monthsInYear.size - 1) * 4).dp
                        Box(
                            Modifier.width(groupWidthDp).height(18.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                "$year",
                                Modifier.padding(start = 4.dp),
                                fontSize = 10.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
        }

        // Шапка: закреплённая колонка + скроллящиеся даты
        Row(Modifier.fillMaxWidth().height(39.dp)) {
            Box(
                Modifier.width(140.dp).height(39.dp).background(Color(0xFFF7F7F7)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Объект",
                    Modifier.padding(start = 16.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF727272)
                )
            }
            Row(
                Modifier.weight(1f).height(39.dp).horizontalScroll(horizontalScroll)
            ) {
                dates.forEach { date ->
                    Box(Modifier.width(44.dp).height(39.dp), contentAlignment = Alignment.Center) {
                        Text(
                            headerLabel(date, viewMode),
                            fontSize = if (viewMode == ViewMode.MONTHS) 11.sp else 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF727272)
                        )
                    }
                }
            }
        }

        // Тело: строки объектов с ячейками
        LazyColumn(Modifier.fillMaxSize()) {
            items(properties, key = { it.id }) { property ->
                val currentProperty by rememberUpdatedState(property)
                val rowSelected = selPropertyId == property.id
                Row(Modifier.fillMaxWidth().height(39.dp)) {
                    Box(
                        Modifier.width(140.dp).height(39.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            property.name,
                            Modifier.padding(start = 16.dp),
                            fontSize = 13.sp,
                            color = Color(0xFF212121)
                        )
                    }
                    Row(
                        Modifier.weight(1f).height(39.dp).horizontalScroll(horizontalScroll)
                    ) {
                        val totalWidthDp = (dates.size * 44 + (dates.size - 1) * 4).dp
                        Canvas(
                            Modifier
                                .width(totalWidthDp)
                                .height(39.dp)
                                .pointerInput(dates) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            if (dates.isNotEmpty()) {
                                                val index = (offset.x / cellPitchPx).toInt()
                                                    .coerceIn(0, dates.lastIndex)
                                                if (selPropertyId == null || selPropertyId != currentProperty.id) {
                                                    selPropertyId = currentProperty.id
                                                    selStart = index
                                                    selEnd = index
                                                    // «expired» (занято + просрочка) — тоже
                                                    // занятая ячейка: тап снимает бронь
                                                    val state = currentProperty.statusAt(dates[index])
                                                    selRemove = state == "fullness" || state == "expired"
                                                } else {
                                                    val s = selStart ?: index
                                                    val lo = minOf(s, index)
                                                    val hi = maxOf(s, index)
                                                    val startDate = dates.getOrNull(lo)
                                                    val endDate = dates.getOrNull(hi)
                                                    if (startDate != null && endDate != null) {
                                                        currentOnRangeSelected(currentProperty.id, startDate, endDate, selRemove)
                                                    }
                                                    selPropertyId = null
                                                    selStart = null
                                                    selEnd = null
                                                }
                                            }
                                        },
                                        // Долгое нажатие на красной ячейке —
                                        // ручное гашение просрочки
                                        onLongPress = { offset ->
                                            if (dates.isNotEmpty()) {
                                                val index = (offset.x / cellPitchPx).toInt()
                                                    .coerceIn(0, dates.lastIndex)
                                                if (currentProperty.statusAt(dates[index]) == "expired") {
                                                    currentOnOverdueMark(currentProperty.id)
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            val cellW = 44.dp.toPx()
                            val cellH = 39.dp.toPx()
                            val gap = 4.dp.toPx()
                            val radius = CornerRadius(4.dp.toPx())
                            dates.forEachIndexed { i, date ->
                                val (bg, stroke) = tableColors(property.statusAt(date))
                                val x = i * (cellW + gap)
                                drawRoundRect(
                                    color = bg,
                                    topLeft = Offset(x, 0f),
                                    size = Size(cellW, cellH),
                                    cornerRadius = radius
                                )
                                drawRoundRect(
                                    color = stroke,
                                    topLeft = Offset(x, 0f),
                                    size = Size(cellW, cellH),
                                    cornerRadius = radius,
                                    style = Stroke(1.dp.toPx())
                                )
                                if (rowSelected) {
                                    val s = selStart
                                    val e = selEnd
                                    if (s != null && e != null && i in minOf(s, e)..maxOf(s, e)) {
                                        drawRoundRect(
                                            color = TableCellFullStroke,
                                            topLeft = Offset(x, 0f),
                                            size = Size(cellW, cellH),
                                            cornerRadius = radius,
                                            style = Stroke(2.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

