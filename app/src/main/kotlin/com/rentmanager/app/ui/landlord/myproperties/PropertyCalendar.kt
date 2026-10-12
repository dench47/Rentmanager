package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

// =====================================================================
//  Календарь-полоса «Моя недвижимость» — канвас 19 (node 4390:91245)
//  Замеры и правила: см. c19_canon.md
// =====================================================================

/** Свободно — #EFEFEF; занято — #E5F2E7 / обводка #2F7D4D. */
internal val CalFree = Color(0xFFEFEFEF)
internal val CalBusyBg = Color(0xFFE5F2E7)
internal val CalBusyStroke = Color(0xFF2F7D4D)

/** Запланировано (аренда добавлена на будущее) — #E4ECF8 / #3568B6. */
internal val CalPlannedBg = Color(0xFFE4ECF8)
internal val CalPlannedStroke = Color(0xFF3568B6)

/** Выбранный период — сплошная плашка #212121. */
internal val CalSelected = Color(0xFF212121)

/** Задолженность — обводка #FF4249 (1px, у выбранного — 2px). */
internal val CalOverdueStroke = Color(0xFFFF4249)

internal val CalDivider = Color(0xFFD9D9D9)
internal val CalInk = Color(0xFF212121)
internal val CalInkGray = Color(0xFF727272)

/** Ячейка дня: 48×38 r4, шаг 52 (48 + gap 4). */
internal const val CAL_CELL_W = 48
internal const val CAL_CELL_H = 38
internal const val CAL_GAP = 4
internal const val CAL_NUM_H = 24
internal const val CAL_LABEL_H = 16
internal const val CAL_BLOCK_W = 6

/** Состояние дня в полосе. */
enum class CalState { FREE, BUSY, PLANNED, OVERDUE, SELECTED }

/**
 * Кусок дня: половинки 22/26 нужны в день передачи — выезд одного арендатора
 * и заезд другого в одну дату (пояснение дизайнера к 15-му числу).
 */
data class CalPiece(val state: CalState, val width: Int)

/** День: одна ячейка 48 или две половинки (22 + 26). */
data class CalDay(val date: LocalDate, val pieces: List<CalPiece>)

/** Блок полосы: месяц (посутово) или год (помесячно). */
data class CalBlock(
    val label: String,
    val numbers: List<String>,
    val days: List<CalDay>,
    /** Есть ли дивайдер слева (6×41 с линией 1×22 #D9D9D9) */
    val withDivider: Boolean = true
) {
    /** Ширина контента блока без дивайдера: шаг 52 на день. */
    val contentWidth: Int get() = days.size * (CAL_CELL_W + CAL_GAP)
}

/** Суммарная ширина блока с дивайдером — нужна для sticky-подписи. */
internal fun CalBlock.totalWidth(): Int = contentWidth + if (withDivider) CAL_BLOCK_W else 0

/**
 * Состояние дня по данным объекта (см. c19_canon.md, §3):
 * свободно → занято (аренда идёт) → запланировано (бронь в будущем) →
 * просрочка (занятое прошлое при долге).
 */
internal fun MyPropertyItem.calStateAt(date: LocalDate): CalState {
    val range = bookings.firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }
        ?: return CalState.FREE
    val today = LocalDate.now()
    return when {
        range.start.isAfter(today) -> CalState.PLANNED
        overdue && date.isBefore(today) -> CalState.OVERDUE
        else -> CalState.BUSY
    }
}

/**
 * День с половинками: если на дату заканчивается один период и начинается
 * другой — левая половина 22 отдаётся выезду, правая 26 — заезду.
 */
internal fun MyPropertyItem.calDayAt(date: LocalDate, selected: ClosedRange<LocalDate>?): CalDay {
    if (selected != null && date in selected) {
        return CalDay(date, listOf(CalPiece(CalState.SELECTED, CAL_CELL_W)))
    }
    val own = bookings.filter { !date.isBefore(it.start) && !date.isAfter(it.end) }
    val state = calStateAt(date)
    if (own.size < 2) return CalDay(date, listOf(CalPiece(state, CAL_CELL_W)))
    val today = LocalDate.now()
    fun stateOf(r: BookingRange): CalState = when {
        r.start.isAfter(today) -> CalState.PLANNED
        overdue && date.isBefore(today) -> CalState.OVERDUE
        else -> CalState.BUSY
    }
    val ending = own.filter { it.end == date }
    val starting = own.filter { it.start == date }
    return when {
        ending.isNotEmpty() && starting.isNotEmpty() ->
            CalDay(date, listOf(CalPiece(stateOf(ending.first()), 22), CalPiece(stateOf(starting.first()), 26)))
        else -> CalDay(date, listOf(CalPiece(state, CAL_CELL_W)))
    }
}

// ===================== Отрисовка полосы =====================

/** Кусок строки статусов: ячейка дня, часть полосы аренды или прокладка gap. */
private class CalSeg(
    val date: LocalDate,
    val state: CalState,
    val width: Int,
    val gap: Boolean = false
)

private fun isBarState(s: CalState) =
    s == CalState.BUSY || s == CalState.PLANNED || s == CalState.OVERDUE

/**
 * Раскладка строки статусов. Шаг дня всегда 52 (ячейка 48 + gap 4), поэтому
 * полосы аренды не сдвигают шкалу: полный день внутри полосы занимает 52,
 * половинки на границе (день передачи) — 22/26, прокладка добирает шаг.
 */
private fun layoutStatusRow(block: CalBlock): List<CalSeg> {
    val out = ArrayList<CalSeg>()
    block.days.forEach { day ->
        val pieces = day.pieces
        if (pieces.size == 1) {
            val p = pieces[0]
            if (isBarState(p.state)) {
                out += CalSeg(day.date, p.state, p.width + CAL_GAP)
            } else {
                out += CalSeg(day.date, p.state, p.width)
                out += CalSeg(day.date, p.state, CAL_GAP, gap = true)
            }
        } else {
            pieces.forEachIndexed { index, p ->
                val last = index == pieces.lastIndex
                if (isBarState(p.state)) {
                    out += CalSeg(day.date, p.state, p.width + if (last) CAL_GAP else 0)
                } else {
                    out += CalSeg(day.date, p.state, p.width)
                    if (last) out += CalSeg(day.date, p.state, CAL_GAP, gap = true)
                }
            }
        }
    }
    return out
}

private fun barStroke(state: CalState): Color = when (state) {
    CalState.PLANNED -> CalPlannedStroke
    CalState.OVERDUE -> CalOverdueStroke
    else -> CalBusyStroke
}

private fun barFill(state: CalState): Color =
    if (state == CalState.PLANNED) CalPlannedBg else CalBusyBg

/** Свободная ячейка, половинка или выбранный период (чёрная плашка). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalPlainCell(
    seg: CalSeg,
    onClick: (LocalDate) -> Unit,
    onLongClick: (LocalDate) -> Unit
) {
    val bg = when (seg.state) {
        CalState.SELECTED -> CalSelected
        CalState.OVERDUE -> CalBusyBg
        else -> CalFree
    }
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .width(seg.width.dp)
            .height(CAL_CELL_H.dp)
            .clip(shape)
            .background(bg)
            .then(
                if (seg.state == CalState.OVERDUE) Modifier.border(1.dp, CalOverdueStroke, shape)
                else Modifier
            )
            .combinedClickable(
                onClick = { onClick(seg.date) },
                onLongClick = { onLongClick(seg.date) }
            )
    )
}

/** Полоса аренды: рамка состояния вокруг подряд идущих ячеек (внутри 36 высотой). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalBarView(
    cells: List<CalSeg>,
    state: CalState,
    emphasized: Boolean,
    onClick: (LocalDate) -> Unit,
    onLongClick: (LocalDate) -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val stroke = if (state == CalState.OVERDUE) CalOverdueStroke else barStroke(state)
    Box(
        Modifier
            .width(cells.sumOf { it.width }.dp)
            .height(CAL_CELL_H.dp)
            .clip(shape)
            .border(if (emphasized) 2.dp else 1.dp, stroke, shape)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(CAL_CELL_H.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cells.forEach { cell ->
                Box(
                    Modifier
                        .width(cell.width.dp)
                        .height((CAL_CELL_H - 2).dp)
                        .background(barFill(state))
                        .combinedClickable(
                            onClick = { onClick(cell.date) },
                            onLongClick = { onLongClick(cell.date) }
                        )
                )
            }
        }
    }
}

@Composable
private fun CalStatusRow(
    block: CalBlock,
    emphasized: ClosedRange<LocalDate>?,
    onClick: (LocalDate) -> Unit,
    onLongClick: (LocalDate) -> Unit
) {
    val segs = remember(block) { layoutStatusRow(block) }
    Row(
        Modifier.height(CAL_CELL_H.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var i = 0
        while (i < segs.size) {
            val seg = segs[i]
            when {
                seg.gap -> {
                    Spacer(Modifier.width(seg.width.dp))
                    i++
                }
                !isBarState(seg.state) -> {
                    CalPlainCell(seg, onClick, onLongClick)
                    i++
                }
                else -> {
                    val cells = ArrayList<CalSeg>()
                    while (
                        i < segs.size && !segs[i].gap &&
                        isBarState(segs[i].state) && segs[i].state == seg.state
                    ) {
                        cells += segs[i]
                        i++
                    }
                    if (cells.isEmpty()) {
                        CalPlainCell(seg, onClick, onLongClick)
                    } else {
                        val emph = emphasized != null &&
                            !cells.first().date.isBefore(emphasized.start) &&
                            !cells.last().date.isAfter(emphasized.endInclusive)
                        CalBarView(cells, seg.state, emph, onClick, onLongClick)
                    }
                }
            }
        }
    }
}

/** Блок полосы: дивайдер месяца 6×41 (линия 1×22), строка номеров 24 и статусы 38. */
@Composable
private fun CalBlockView(
    block: CalBlock,
    emphasized: ClosedRange<LocalDate>?,
    onClick: (LocalDate) -> Unit,
    onLongClick: (LocalDate) -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        if (block.withDivider) {
            Box(
                Modifier
                    .width(CAL_BLOCK_W.dp)
                    .height((CAL_LABEL_H + CAL_NUM_H + CAL_CELL_H).dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(CalDivider)
                )
            }
        }
        Column(Modifier.width(block.contentWidth.dp)) {
            // Строка липкой подписи — текст рисует оверлей PropertyCalendar
            Spacer(Modifier.height(CAL_LABEL_H.dp))
            Row(Modifier.height(CAL_NUM_H.dp)) {
                block.numbers.forEachIndexed { index, number ->
                    Box(
                        Modifier
                            .width(CAL_CELL_W.dp)
                            .height(CAL_NUM_H.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            number,
                            fontSize = 13.sp,
                            color = CalInk,
                            maxLines = 1
                        )
                    }
                    if (index != block.numbers.lastIndex) Spacer(Modifier.width(CAL_GAP.dp))
                }
            }
            CalStatusRow(block, emphasized, onClick, onLongClick)
        }
    }
}

/**
 * Полоса-календарь карточки объекта: «подпись месяца → номера дней → статусы».
 * Подпись месяца липкая и двигается по формуле из пояснения дизайнера:
 * labelX = min(max(cardCenterX − labelWidth/2, monthStartX + 98),
 *              monthEndX − 98 − labelWidth).
 */
@Composable
internal fun PropertyCalendar(
    blocks: List<CalBlock>,
    onDayClick: (LocalDate) -> Unit,
    onDayLongClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    emphasized: ClosedRange<LocalDate>? = null
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    var cardWidth by remember { mutableIntStateOf(0) }
    val labelStyle = remember {
        TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CalInk)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height((CAL_LABEL_H + CAL_NUM_H + CAL_CELL_H).dp)
            .clipToBounds()
            .onSizeChanged { cardWidth = it.width }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.Top
        ) {
            blocks.forEach { block ->
                CalBlockView(block, emphasized, onDayClick, onDayLongClick)
            }
        }

        // Липкая подпись месяца (координаты — относительно видимой области)
        val scroll = scrollState.value
        val off98 = with(density) { 98.dp.toPx() }
        var contentCursor = 0
        blocks.forEach { block ->
            val dividerW = with(density) {
                (if (block.withDivider) CAL_BLOCK_W else 0).dp.roundToPx()
            }
            val contentStart = contentCursor + dividerW
            val contentEnd = contentStart + with(density) { block.contentWidth.dp.roundToPx() }
            contentCursor = contentEnd

            val labelWidth = measurer
                .measure(AnnotatedString(block.label), labelStyle)
                .size.width.toFloat()
            val startOnScreen = (contentStart - scroll).toFloat()
            val endOnScreen = (contentEnd - scroll).toFloat()
            val x = minOf(
                maxOf(cardWidth / 2f - labelWidth / 2f, startOnScreen + off98),
                endOnScreen - off98 - labelWidth
            )
            if (x + labelWidth > 0f && x < cardWidth.toFloat()) {
                Text(
                    block.label,
                    style = labelStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset { IntOffset(x.roundToInt(), 0) }
                )
            }
        }
    }
}

// ===================== Данные полосы =====================

/** Названия месяцев над ячейками помесячного вида — как в канвасе. */
private val CalMonthCellLabels = listOf(
    "Янв", "Фев", "Март", "Апр", "Май", "Июнь",
    "Июль", "Авг", "Сен", "Окт", "Ноя", "Дек"
)

private val CalMonthFullNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

/** «Сентябрь 2026» — подпись месяца без запятой, как на экранах канваса. */
internal fun monthLabel(month: YearMonth): String =
    CalMonthFullNames[month.monthValue - 1] + " " + month.year

/** Полные названия месяцев (Январь…Декабрь) — колонка месяцев в пикере периода. */
internal val CalendarMonthNames: List<String> get() = CalMonthFullNames

/** Имя месяца в нижнем регистре — для подписей периодов в панелях. */
internal fun monthNameLower(month1: Int): String =
    CalMonthFullNames[(month1 - 1).coerceIn(0, 11)].lowercase()

/**
 * Блоки календаря карточки объекта.
 *
 * [monthly] = true — «Помесячно»: блоки годов (подпись «2026» + 12 месяцев);
 * иначе «Посуточно»: блоки месяцев (подпись «Сентябрь 2026» + дни месяца).
 * [anchor] — период: посуточно календарь открыт с первого дня месяца,
 * помесячно — на соответствующем месяце (c19_canon.md §4).
 */
internal fun buildCalBlocks(
    item: MyPropertyItem,
    anchor: YearMonth,
    monthly: Boolean,
    selection: ClosedRange<LocalDate>? = null,
    monthsAhead: Int = 4,
    yearsAhead: Int = 2
): List<CalBlock> {
    val blocks = ArrayList<CalBlock>()
    if (monthly) {
        for (index in 0..yearsAhead) {
            val year = anchor.year + index
            val days = (1..12).map { month ->
                item.calDayAt(LocalDate.of(year, month, 1), selection)
            }
            blocks += CalBlock(
                label = year.toString(),
                numbers = CalMonthCellLabels,
                days = days,
                withDivider = index > 0
            )
        }
        return blocks
    }
    for (index in 0..monthsAhead) {
        val month = anchor.plusMonths(index.toLong())
        val lastDay = month.lengthOfMonth()
        val days = (1..lastDay).map { day ->
            item.calDayAt(LocalDate.of(month.year, month.monthValue, day), selection)
        }
        blocks += CalBlock(
            label = monthLabel(month),
            numbers = (1..lastDay).map { it.toString() },
            days = days,
            withDivider = index > 0
        )
    }
    return blocks
}

