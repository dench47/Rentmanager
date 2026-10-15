package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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

/** Выбранный период — даты новой аренды до сохранения: #727272 (канвас 4390:94462). */
internal val CalSelectedPeriod = Color(0xFF727272)

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

/**
 * Практический предел полосы: сколько блоков вперёд от якоря строит LazyRow.
 * Блоки считаются по требованию, поэтому «бесконечность» ничего не стоит:
 * 600 месяцев (50 лет) в посуточном виде и 600 лет в помесячном — дальше якоря
 * (пикер периода даёт +12 лет) уехать всё равно нельзя.
 */
internal const val CAL_BLOCK_LIMIT = 600

/** Состояние дня в полосе. */
enum class CalState { FREE, BUSY, PLANNED, OVERDUE, SELECTED }

/**
 * Кусок дня: половинки 22/26 нужны в день передачи — выезд одного арендатора
 * и заезд другого в одну дату (пояснение дизайнера к 15-му числу).
 */
/**
 * Кусок ячейки: половинка дня или месяца на границе аренды. Канвас 4393:107371:
 * полоса 228 = ячейки 51,51,51,51,22 — день выезда отдан половинкой 22, а его
 * вторая половина нарисована отдельной ячейкой.
 * [range] — период, который представляет ИМЕННО этот кусок (у свободной половины —
 * только она), [key] — бронь, чтобы полосы разных арендаторов не сливались,
 * [debt] — в периоде есть непогашенный платёж (2px #FF4249).
 */
data class CalPiece(
    val state: CalState,
    val width: Int,
    val range: ClosedRange<LocalDate>,
    val debt: Boolean = false,
    val key: String = ""
)

/**
 * Ячейка полосы: одна ячейка 48 или две половинки (22 + 26).
 * [range] — период ячейки целиком (день или месяц): нужен свободным ячейкам,
 * выбору периода и проверке «прошлое не бронируем».
 */
data class CalDay(
    val date: LocalDate,
    val pieces: List<CalPiece>,
    val range: ClosedRange<LocalDate> = date..date
)

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

/**
 * Состояние ячейки по данным объекта (см. c19_canon.md, §3):
 * свободно → занято (аренда идёт) → запланировано (бронь в будущем).
 * Долг (`debt`) живёт отдельным флагом на куске полосы: красная 2px обводка
 * рисуется вокруг всего периода аренды (канвас 4390:91570), а не по месяцам —
 * иначе период рвётся на несколько сегментов.
 */
internal fun MyPropertyItem.bookingAt(date: LocalDate): BookingRange? =
    bookings.firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }

/** Бронь, пересекающая период ячейки (в помесячном виде ячейка — весь месяц). */
internal fun MyPropertyItem.bookingIn(range: ClosedRange<LocalDate>): BookingRange? =
    bookings.firstOrNull { !it.start.isAfter(range.endInclusive) && !it.end.isBefore(range.start) }

internal fun stateOfBooking(b: BookingRange, today: LocalDate = LocalDate.now()): CalState =
    if (b.start.isAfter(today)) CalState.PLANNED else CalState.BUSY

internal fun MyPropertyItem.calStateAt(date: LocalDate): CalState =
    bookingAt(date)?.let { stateOfBooking(it) } ?: CalState.FREE

/**
 * Выделение новых дат: чёрной плашкой становится КАЖДЫЙ свободный кусок, целиком
 * попавший в период, — поэтому у дня с половинками подсвечивается только та
 * половинка, по которой тапнули (занятая остаётся зелёной, вторая свободная —
 * серой). Раньше вся ячейка подменялась одним куском 48 — выделение выглядело
 * целым, даже если половина дня уже занята.
 */
private fun List<CalPiece>.withSelection(selected: ClosedRange<LocalDate>?): List<CalPiece> {
    if (selected == null) return this
    return map { piece ->
        if (piece.state == CalState.FREE &&
            !piece.range.start.isBefore(selected.start) &&
            !piece.range.endInclusive.isAfter(selected.endInclusive)
        ) {
            piece.copy(state = CalState.SELECTED)
        } else {
            piece
        }
    }
}

/**
 * День с половинками. Границы аренды — как в канвасе 4393:107371 (пояснение
 * 4390:95388: 16-е — выезд одного арендатора, но этот же день можно выбрать для
 * заезда другого): день выезда — половинка в полосе + отдельная свободная
 * половинка, день заезда — свободная половинка + половинка в полосе, день
 * передачи — две половинки. Зазор 4px между ячейками действует и на половинки.
 */
internal fun MyPropertyItem.calDayAt(date: LocalDate, selected: ClosedRange<LocalDate>?): CalDay {
    val range = date..date
    val own = bookings.filter { !date.isBefore(it.start) && !date.isAfter(it.end) }
    val today = LocalDate.now()
    val ending = own.filter { it.end == date && it.start != date }
    val starting = own.filter { it.start == date && it.end != date }
    val pieces = when {
        own.isEmpty() -> listOf(CalPiece(CalState.FREE, CAL_CELL_W, range))
        ending.isNotEmpty() && starting.isNotEmpty() -> {
            val out = ending.first()
            val arrive = starting.first()
            listOf(
                CalPiece(stateOfBooking(out, today), 22, range, out.debtAmount > 0, out.bookingKey()),
                CalPiece(stateOfBooking(arrive, today), 22, range, arrive.debtAmount > 0, arrive.bookingKey())
            )
        }
        starting.isNotEmpty() -> {
            val arrive = starting.first()
            listOf(
                CalPiece(CalState.FREE, 22, range),
                CalPiece(stateOfBooking(arrive, today), 22, range, arrive.debtAmount > 0, arrive.bookingKey())
            )
        }
        ending.isNotEmpty() -> {
            val out = ending.first()
            listOf(
                CalPiece(stateOfBooking(out, today), 22, range, out.debtAmount > 0, out.bookingKey()),
                CalPiece(CalState.FREE, 22, range)
            )
        }
        else -> {
            val actual = own.maxByOrNull { it.start }!!
            listOf(
                CalPiece(
                    stateOfBooking(actual, today),
                    CAL_CELL_W,
                    range,
                    actual.debtAmount > 0,
                    actual.bookingKey()
                )
            )
        }
    }
    return CalDay(date, pieces.withSelection(selected), range)
}

/** Идентификатор брони: по нему куски одной аренды склеиваются, а разные — нет. */
private fun BookingRange.bookingKey(): String = id ?: "$start..$end"

/**
 * Ячейка месяца помесячного вида: ячейка представляет ВЕСЬ месяц, бронь ищется по
 * пересечению (иначе месяц начала аренды, например 22.09, остаётся серым), а на
 * границах аренды месяц рисуется половинкой. У каждой половинки СВОЙ период —
 * поэтому серая часть ноября после выезда 21.11 открывает свободные дни,
 * а не карточку уехавшего арендатора.
 */
internal fun MyPropertyItem.calMonthAt(month: YearMonth, selected: ClosedRange<LocalDate>?): CalDay {
    val range = month.atDay(1)..month.atEndOfMonth()
    val b = bookingIn(range)
    val pieces = if (b == null) {
        listOf(CalPiece(CalState.FREE, CAL_CELL_W, range))
    } else {
        val today = LocalDate.now()
        val state = stateOfBooking(b, today)
        val key = b.bookingKey()
        when {
            b.start.isAfter(range.start) -> listOf(
                CalPiece(CalState.FREE, 22, range.start..b.start.minusDays(1)),
                CalPiece(state, 22, b.start..range.endInclusive, b.debtAmount > 0, key)
            )
            b.end.isBefore(range.endInclusive) -> listOf(
                CalPiece(state, 22, range.start..b.end, b.debtAmount > 0, key),
                CalPiece(CalState.FREE, 22, b.end.plusDays(1)..range.endInclusive)
            )
            else -> listOf(CalPiece(state, CAL_CELL_W, range, b.debtAmount > 0, key))
        }
    }
    return CalDay(range.start, pieces.withSelection(selected), range)
}

// ===================== Отрисовка полосы =====================

/** Кусок строки статусов: кусок ячейки, часть полосы аренды или прокладка gap. */
private class CalSeg(
    val day: CalDay,
    val state: CalState,
    val width: Int,
    val range: ClosedRange<LocalDate>,
    val gap: Boolean = false,
    val debt: Boolean = false,
    val key: String = ""
)

private fun isBarState(s: CalState) =
    s == CalState.BUSY || s == CalState.PLANNED || s == CalState.OVERDUE

/**
 * Раскладка строки статусов. Шаг дня всегда 52 (ячейка 48 + gap 4), поэтому
 * полосы аренды не сдвигают шкалу: полный день внутри полосы занимает 52,
 * половинки на границе (день выезда/заезда, месяц на границе аренды) — 22/26,
 * прокладка добирает шаг.
 */
private fun layoutStatusRow(block: CalBlock): List<CalSeg> {
    val out = ArrayList<CalSeg>()
    block.days.forEach { day ->
        val pieces = day.pieces
        if (pieces.size == 1) {
            val p = pieces[0]
            if (isBarState(p.state)) {
                out += CalSeg(day, p.state, p.width + CAL_GAP, p.range, debt = p.debt, key = p.key)
            } else {
                out += CalSeg(day, p.state, p.width, p.range)
                out += CalSeg(day, p.state, CAL_GAP, p.range, gap = true)
            }
        } else {
            // Две половинки: день заезда/выезда или месяц на границе аренды.
            // Зазор 4px сворачиваем в ширину той половинки, которая ПРОДОЛЖАЕТ
            // бронь (день заезда, месяц начала) — тогда соседний день той же аренды
            // приклеивается, и полоса остаётся одним пузырём. Явный зазор 4px
            // рисуем после половинки, которая бронь ЗАКРЫВАЕТ (день выезда,
            // месяц окончания), и после свободных половинок.
            pieces.forEachIndexed { index, p ->
                val last = index == pieces.lastIndex
                if (isBarState(p.state)) {
                    out += CalSeg(
                        day,
                        p.state,
                        p.width + if (last) CAL_GAP else 0,
                        p.range,
                        debt = p.debt,
                        key = p.key
                    )
                    if (!last) out += CalSeg(day, p.state, CAL_GAP, p.range, gap = true)
                } else {
                    out += CalSeg(day, p.state, p.width, p.range)
                    out += CalSeg(day, p.state, CAL_GAP, p.range, gap = true)
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

/** Свободная ячейка (в том числе половинка) или выбранный период (чёрная плашка). */
@Composable
private fun CalPlainCell(
    seg: CalSeg,
    onClick: (CalPiece) -> Unit
) {
    val bg = if (seg.state == CalState.SELECTED) CalSelectedPeriod else CalFree
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .width(seg.width.dp)
            .height(CAL_CELL_H.dp)
            .clip(shape)
            .background(bg)
            .clickable { onClick(CalPiece(seg.state, seg.width, seg.range, seg.debt, seg.key)) }
    )
}

/**
 * Полоса аренды: рамка вокруг подряд идущих кусков ОДНОЙ брони.
 * Толщина: 1px — обычная полоса и долг (#FF4249, как в легенде календаря),
 * 2px — когда по области открыта карточка: пояснение Вики (узел 4390:95386)
 * «при нажатии на область аренды stroke увеличиваем с 1px на 2px».
 */
@Composable
private fun CalBarView(
    cells: List<CalSeg>,
    state: CalState,
    debt: Boolean,
    emphasized: Boolean,
    onClick: (CalPiece) -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val strokeColor = if (debt) CalOverdueStroke else barStroke(state)
    val strokeWidth = if (emphasized) 2.dp else 1.dp
    Box(
        Modifier
            .width(cells.sumOf { it.width }.dp)
            .height(CAL_CELL_H.dp)
            .clip(shape)
            .border(strokeWidth, strokeColor, shape)
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
                        .clickable {
                            onClick(CalPiece(state, cell.width, cell.range, cell.debt, cell.key))
                        }
                )
            }
        }
    }
}

@Composable
private fun CalStatusRow(
    block: CalBlock,
    emphasized: ClosedRange<LocalDate>?,
    onClick: (CalPiece) -> Unit
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
                    CalPlainCell(seg, onClick)
                    i++
                }
                else -> {
                    val cells = ArrayList<CalSeg>()
                    while (
                        i < segs.size && !segs[i].gap &&
                        isBarState(segs[i].state) && segs[i].state == seg.state &&
                        segs[i].debt == seg.debt && segs[i].key == seg.key
                    ) {
                        cells += segs[i]
                        i++
                    }
                    if (cells.isEmpty()) {
                        CalPlainCell(seg, onClick)
                    } else {
                        // 2px — если карточка открыта именно по этому периоду
                        val emph = emphasized != null &&
                            !cells.first().range.start.isBefore(emphasized.start) &&
                            !cells.last().range.endInclusive.isAfter(emphasized.endInclusive)
                        CalBarView(cells, seg.state, seg.debt, emph, onClick)
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
    onClick: (CalPiece) -> Unit
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
            CalStatusRow(block, emphasized, onClick)
        }
    }
}

/**
 * Полоса-календарь карточки объекта: «подпись месяца → номера дней → статусы».
 *
 * Полоса ленивая ([LazyRow]): блоки строятся по требованию, поэтому вперёд можно
 * листать без ограничения по годам — [blockCount] задаёт практический предел
 * ([CAL_BLOCK_LIMIT]).
 *
 * Подпись месяца липкая и двигается по формуле из пояснения дизайнера:
 * labelX = min(max(cardCenterX − labelWidth/2, monthStartX + 98),
 *              monthEndX − 98 − labelWidth).
 */
@Composable
internal fun PropertyCalendar(
    /** Сколько блоков вперёд от якоря доступно для прокрутки */
    blockCount: Int,
    /** Блок по его номеру в полосе: считается по требованию при прокрутке */
    blockAt: (Int) -> CalBlock,
    /** Подпись блока по его номеру — без расчёта дней блока */
    labelAt: (Int) -> String,
    onDayClick: (CalPiece) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    /** Период, по которому открыта карточка card_inf: его полоса рисуется 2px */
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
        LazyRow(
            Modifier.fillMaxWidth(),
            state = listState,
            verticalAlignment = Alignment.Top
        ) {
            items(count = blockCount, key = { it }) { index ->
                CalBlockView(blockAt(index), emphasized, onDayClick)
            }
        }

        // Липкая подпись месяца (координаты — относительно видимой области).
        // Позиции блоков читаем из layoutInfo, но перечитываем их на каждый кадр
        // прокрутки (пара примитивов — сравнение по значению) и на первое измерение
        // ленты: иначе подписи не появлялись до первого скролла, а с одним только
        // derivedStateOf переставали двигаться при прокрутке.
        val scrollTick by remember(listState) {
            derivedStateOf { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
        }
        val laidOut by remember(listState) {
            derivedStateOf { listState.layoutInfo.totalItemsCount > 0 }
        }
        val visibleItems = remember(scrollTick, laidOut) { listState.layoutInfo.visibleItemsInfo }
        visibleItems.forEach { item ->
            val dividerPx = with(density) {
                (if (item.index > 0) CAL_BLOCK_W else 0).dp.roundToPx()
            }
            val contentStart = (item.offset + dividerPx).toFloat()
            val contentEnd = (item.offset + item.size).toFloat()
            val label = labelAt(item.index)
            val labelWidth = measurer
                .measure(AnnotatedString(label), labelStyle)
                .size.width.toFloat()
            // Уходящий блок выталкивает подпись влево ровно своим дивайдером
            // (по направлению движения ячеек), подпись следующего уже по центру.
            val x = minOf(
                maxOf(cardWidth / 2f - labelWidth / 2f, contentStart),
                contentEnd - labelWidth
            )
            if (x + labelWidth > 0f && x < cardWidth.toFloat()) {
                Text(
                    label,
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
 * Прошлое не бронируется: якорь календаря не раньше текущего месяца.
 */
internal fun safeCalAnchor(anchor: YearMonth): YearMonth =
    if (anchor.isBefore(YearMonth.now())) YearMonth.now() else anchor

/**
 * Блок календаря карточки объекта по его номеру в полосе (0 — блок якоря).
 *
 * [monthly] = true — «Помесячно»: блоки годов (подпись «2026» + 12 месяцев);
 * иначе «Посуточно»: блоки месяцев (подпись «Сентябрь 2026» + дни месяца).
 * [anchor] — период: посуточно календарь открыт с первого дня месяца,
 * помесячно — на соответствующем месяце (c19_canon.md §4).
 * Блок считается по требованию: полоса ленивая (см. [PropertyCalendar]).
 */
internal fun buildCalBlock(
    item: MyPropertyItem,
    anchor: YearMonth,
    index: Int,
    monthly: Boolean,
    selection: ClosedRange<LocalDate>? = null
): CalBlock {
    val safeAnchor = safeCalAnchor(anchor)
    if (monthly) {
        val year = safeAnchor.year + index
        // Ячейка помесячного вида — весь месяц: состояние считаем по пересечению
        // месяца с бронью, иначе месяц начала аренды (например 22.09) оставался серым
        val days = (1..12).map { month -> item.calMonthAt(YearMonth.of(year, month), selection) }
        return CalBlock(
            label = year.toString(),
            numbers = CalMonthCellLabels,
            days = days,
            withDivider = index > 0
        )
    }
    val month = safeAnchor.plusMonths(index.toLong())
    val lastDay = month.lengthOfMonth()
    val days = (1..lastDay).map { day ->
        item.calDayAt(LocalDate.of(month.year, month.monthValue, day), selection)
    }
    return CalBlock(
        label = monthLabel(month),
        numbers = (1..lastDay).map { it.toString() },
        days = days,
        withDivider = index > 0
    )
}

/**
 * Подпись блока полосы («2026» помесячно, «Сентябрь 2026» посуточно) — считается
 * отдельно от данных блока, чтобы прокрутка не пересчитывала дни ради текста.
 */
internal fun calBlockLabel(anchor: YearMonth, index: Int, monthly: Boolean): String {
    val safeAnchor = safeCalAnchor(anchor)
    return if (monthly) {
        (safeAnchor.year + index).toString()
    } else {
        monthLabel(safeAnchor.plusMonths(index.toLong()))
    }
}

