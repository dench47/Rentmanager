package com.rentmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.ToolbarTitleStyle
import java.time.LocalDate
import java.time.YearMonth

private val MonthNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

private val WeekdayNames = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

private val PickerYears = (2024..2035).toList()

/**
 * Календарный шит «Дата следующей проверки» (Figma 2755-37947).
 * Дропдаун «Месяц Год» открывает панель с двумя колонками (месяцы | годы):
 * тап сразу обновляет календарь, список остаётся открытым — повторный тап
 * по тому же значению закрывает панель. Стрелки ‹ › листают месяцы
 * (неактивны при открытой панели). Тап по дню только выбирает значение;
 * «Готово» применяет его к полю; свайп/фон — отмена.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    title: String,
    initialDate: LocalDate?,
    onDone: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayMonth by remember {
        mutableStateOf(YearMonth.from(initialDate ?: LocalDate.now()))
    }
    var picked by remember { mutableStateOf(initialDate) }
    var panelOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            SheetDragHandle()
            Text(title, style = ToolbarTitleStyle)
            Spacer(Modifier.height(12.dp))

            // Дропдаун «Месяц Год» + стрелки соседнего месяца
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { panelOpen = !panelOpen }
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${MonthNames[displayMonth.monthValue - 1]} ${displayMonth.year}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Graphite
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (panelOpen) "▲" else "▼",
                        fontSize = 10.sp,
                        color = GreyText
                    )
                }
                Spacer(Modifier.weight(1f))
                // Стрелки неактивны при открытом выборе месяца/года
                MonthArrow(text = "‹", enabled = !panelOpen) { displayMonth = displayMonth.minusMonths(1) }
                Spacer(Modifier.width(16.dp))
                MonthArrow(text = "›", enabled = !panelOpen) { displayMonth = displayMonth.plusMonths(1) }
            }
            Spacer(Modifier.height(8.dp))

            if (panelOpen) {
                MonthYearPanel(
                    displayMonth = displayMonth,
                    onPick = { ym ->
                        // Тап обновляет календарь сразу; повторный тап по тому же
                        // значению закрывает список
                        if (ym == displayMonth) panelOpen = false else displayMonth = ym
                    }
                )
            } else {
                CalendarGrid(
                    displayMonth = displayMonth,
                    picked = picked,
                    onPickDay = { picked = it }
                )
            }

            Spacer(Modifier.height(12.dp))
            BlackCtaButton(
                text = "Готово",
                enabled = picked != null,
                onClick = { picked?.let(onDone) }
            )
        }
    }
}

@Composable
private fun MonthArrow(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = InterFontFamily,
        color = if (enabled) Graphite else Color(0xFFC6C6C6),
        modifier = Modifier
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Панель выбора месяца/года: две скролл-колонки с разделителем (352×280, r20). */
@Composable
private fun MonthYearPanel(
    displayMonth: YearMonth,
    onPick: (YearMonth) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEFEFEF))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        ScrollColumn(
            items = MonthNames,
            selectedIndex = displayMonth.monthValue - 1,
            modifier = Modifier.weight(1f),
            scrollTo = displayMonth.monthValue - 1
        ) { index ->
            onPick(YearMonth.of(displayMonth.year, index + 1))
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFFD9D9D9))
        )
        ScrollColumn(
            items = PickerYears.map { it.toString() },
            selectedIndex = PickerYears.indexOf(displayMonth.year).coerceAtLeast(0),
            modifier = Modifier.weight(1f),
            scrollTo = PickerYears.indexOf(displayMonth.year).coerceAtLeast(0)
        ) { index ->
            onPick(YearMonth.of(PickerYears[index], displayMonth.monthValue))
        }
    }
}

@Composable
private fun ScrollColumn(
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    scrollTo: Int,
    onSelect: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (scrollTo > 1) listState.scrollToItem((scrollTo - 2).coerceAtLeast(0))
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(items) { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(if (selected) Modifier.background(Graphite) else Modifier.background(Color.Transparent))
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = InterFontFamily,
                    color = if (selected) Color.White else Graphite
                )
            }
        }
    }
}

/** Сетка календаря: Пн–Вс, дни соседних месяцев серым 14sp, выбранный — тёмный круг. */
@Composable
private fun CalendarGrid(
    displayMonth: YearMonth,
    picked: LocalDate?,
    onPickDay: (LocalDate) -> Unit
) {
    val firstDay = displayMonth.atDay(1)
    val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            WeekdayNames.forEach { wd ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(wd, style = CardSubtitleStyle.copy(color = GreyText, fontSize = 12.sp))
                }
            }
        }
        repeat(6) { weekIndex ->
            Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                repeat(7) { dayIndex ->
                    val date = gridStart.plusDays((weekIndex * 7 + dayIndex).toLong())
                    val inMonth = date.month == displayMonth.month
                    val selected = picked == date
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .then(
                                    if (selected) Modifier.background(Graphite)
                                    else Modifier.background(Color.Transparent)
                                )
                                .clickable { if (inMonth) onPickDay(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                fontSize = if (inMonth) 13.sp else 14.sp,
                                fontWeight = if (inMonth) FontWeight.Medium else FontWeight.Normal,
                                fontFamily = InterFontFamily,
                                color = when {
                                    selected -> Color.White
                                    inMonth -> Graphite
                                    else -> Color(0xFFB3B3B3)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Шит «День месяца» (Figma 2755-38085): подпись + сетка 1–31 по 7 в ряд,
 * выбранный — тёмный круг 40dp, CTA «Готово». Правило коротких месяцев —
 * подпись под сеткой (решение дизайн-вопроса: в последний день месяца).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOfMonthPickerSheet(
    selectedDay: Int?,
    onDone: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var picked by remember { mutableStateOf(selectedDay) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            SheetDragHandle()
            Text("День месяца", style = ToolbarTitleStyle)
            Spacer(Modifier.height(6.dp))
            Text(
                "Выберите число, до которого нужно передавать показания каждый месяц",
                style = CardSubtitleStyle
            )
            Spacer(Modifier.height(12.dp))
            (1..31).chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (picked == day) Modifier.background(Graphite)
                                        else Modifier.background(Color.Transparent)
                                    )
                                    .clickable { picked = day },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.toString(),
                                    style = CardSubtitleStyle.copy(color = if (picked == day) Color.White else Graphite)
                                )
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "В короткие месяцы — в последний день",
                style = CardSubtitleStyle.copy(color = GreyText)
            )
            Spacer(Modifier.height(12.dp))
            BlackCtaButton(
                text = "Готово",
                enabled = picked != null,
                onClick = { picked?.let(onDone) }
            )
        }
    }
}

/** Ручка шита 32×4 #212121 r100. */
@Composable
fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(width = 32.dp, height = 4.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Graphite)
        )
    }
}
