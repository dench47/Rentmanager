package com.rentmanager.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowInsets as PlatformWindowInsets
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
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

private val WeekdayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private val PickerYears = (2024..2035).toList()

/**
 * Календарный шит «Дата следующей поверки» (Figma 2755-37947).
 * Дропдаун «Месяц Год» открывает панель с двумя колонками (месяцы | годы):
 * тап сразу обновляет календарь, список остаётся открытым — повторный тап
 * по тому же значению закрывает панель. Стрелки ‹ › листают месяцы
 * (неактивны при открытой панели). Тап по дню только выбирает значение;
 * «Готово» применяет его к полю; свайп/фон — отмена.
 *
 * markedDates — уже сохранённые даты: отмечены точкой 7dp в правом верхнем
 * углу дня (Figma 2872-34370/34371: смещение ~26×11 от круга 40dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    title: String,
    initialDate: LocalDate?,
    onDone: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    ctaText: String = "Готово",
    markedDates: Set<LocalDate> = emptySet()
) {
    var displayMonth by remember {
        mutableStateOf(YearMonth.from(initialDate ?: LocalDate.now()))
    }
    var picked by remember { mutableStateOf(initialDate) }
    var panelOpen by remember { mutableStateOf(false) }
    // Прошедшие даты в календаре поверки выбрать нельзя
    val today = LocalDate.now()

    // Низ шита (Figma 2872-34233): под CTA 36dp белого, затем кромка с тенью
    // и прозрачный зазор до низа экрана. Зазор — высота системной навигации,
    // чтобы кромка лежала над ней, а CTA не проваливался под неё; поэтому
    // поверхность шита прозрачная, белый блок и тень рисуем сами
    val navGap = navigationBarGap()
    val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RectangleShape,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0.dp, 0.dp, 0.dp, 0.dp) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, sheetShape)
                .background(Color.White, sheetShape)
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp)
        ) {
            SheetDragHandle(color = Graphite.copy(alpha = 0.4f))
            Text(title, style = ToolbarTitleStyle)
            Spacer(Modifier.height(6.dp))

            // Дропдаун «Месяц Год» + стрелки соседнего месяца (Figma 2872-34238):
            // строка 48dp, дропдаун 36dp прозрачный с паддингом 12 и сдвигом 6,
            // стрелки — иконки 24dp в кнопках 48dp вплотную к правому краю
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { panelOpen = !panelOpen }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${MonthNames[displayMonth.monthValue - 1]} ${displayMonth.year}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Graphite
                    )
                    Spacer(Modifier.width(4.dp))
                    Image(
                        painter = painterResource(R.drawable.ic_calendar_arrow_down),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                // Стрелки неактивны при открытом выборе месяца/года;
                // назад раньше текущего месяца листать нельзя (прошедшее не выбирается)
                MonthArrow(
                    iconRes = R.drawable.ic_calendar_arrow_left,
                    enabled = !panelOpen && displayMonth > YearMonth.from(today)
                ) { displayMonth = displayMonth.minusMonths(1) }
                MonthArrow(
                    iconRes = R.drawable.ic_calendar_arrow_right,
                    enabled = !panelOpen
                ) { displayMonth = displayMonth.plusMonths(1) }
            }

            if (panelOpen) {
                MonthYearPanel(
                    displayMonth = displayMonth,
                    onPick = { ym ->
                        // Тап обновляет календарь сразу; повторный тап по тому же
                        // значению закрывает список. Месяц раньше текущего —
                        // игнорируем: прошедшие даты выбирать нельзя
                        when {
                            ym < YearMonth.from(today) -> Unit
                            ym == displayMonth -> panelOpen = false
                            else -> displayMonth = ym
                        }
                    }
                )
            } else {
                CalendarGrid(
                    displayMonth = displayMonth,
                    picked = picked,
                    today = today,
                    markedDates = markedDates,
                    onPickDay = { picked = it }
                )
            }

            Spacer(Modifier.height(20.dp))
            BlackCtaButton(
                text = ctaText,
                enabled = picked != null,
                onClick = { picked?.let(onDone) }
            )
        }
        // Прозрачный зазор под кромкой шита (Figma: 849→855, но не меньше навигации)
        Spacer(Modifier.height(navGap))
    }
}

@Composable
private fun MonthArrow(iconRes: Int, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(if (enabled) Graphite else Color(0xFFC6C6C6))
        )
    }
}

/**
 * Нижний зазор шита: высота системной навигации. Внутри окна ModalBottomSheet
 * композиционный WindowInsets.navigationBars на части устройств (MIUI,
 * трёхкнопочная навигация) отдаёт 0 — окно диалога получает уже обработанные
 * инсеты. Поэтому сверяемся ещё и с корневыми инсётами окна самой активности,
 * где они гарантированно живые (по ним же стоит таббар экрана).
 */
@Composable
private fun navigationBarGap(): Dp {
    val density = LocalDensity.current
    fun Int.pxToDp(): Dp = with(density) { toDp() }
    val composeInset = WindowInsets.navigationBars.getBottom(density).pxToDp()
    val activityInset = LocalContext.current.findActivity()
        ?.window?.decorView?.rootWindowInsets
        ?.let { root ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                maxOf(
                    root.getInsets(PlatformWindowInsets.Type.navigationBars()).bottom,
                    root.getInsets(PlatformWindowInsets.Type.tappableElement()).bottom
                )
            } else {
                @Suppress("DEPRECATION")
                root.systemWindowInsetBottom
            }
        } ?: 0
    return maxOf(6.dp, composeInset, activityInset.pxToDp())
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Панель барабанов месяца/года (Figma 2768-52717): белая 352×280 r20 с тенью;
 * внутри две скролл-колонки 110dp (отступ 30, между ними разделитель),
 * строки-пилюли 30dp r20 с зазором 6, текст 15/600 по центру,
 * выбранная строка — #212121 с белым текстом.
 */
@Composable
private fun MonthYearPanel(
    displayMonth: YearMonth,
    onPick: (YearMonth) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Spacer(Modifier.width(30.dp))
        ScrollColumn(
            items = MonthNames,
            selectedIndex = displayMonth.monthValue - 1,
            modifier = Modifier.width(110.dp),
            scrollTo = displayMonth.monthValue - 1
        ) { index ->
            onPick(YearMonth.of(displayMonth.year, index + 1))
        }
        // Разделитель между колонками (в 72dp-зазоре по центру)
        Box(Modifier.width(72.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(1.dp)
                    .height(240.dp)
                    .background(Color(0xFFD9D9D9))
            )
        }
        ScrollColumn(
            items = PickerYears.map { it.toString() },
            selectedIndex = PickerYears.indexOf(displayMonth.year).coerceAtLeast(0),
            modifier = Modifier.width(110.dp),
            scrollTo = PickerYears.indexOf(displayMonth.year).coerceAtLeast(0)
        ) { index ->
            onPick(YearMonth.of(PickerYears[index], displayMonth.monthValue))
        }
        Spacer(Modifier.width(30.dp))
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
        modifier = modifier.height(240.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(items) { index, label ->
            val selected = index == selectedIndex
            // Строка-пилюля 30dp r20 (2768-52717), текст 15/600 по центру
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .then(if (selected) Modifier.background(Graphite) else Modifier.background(Color.Transparent))
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = if (selected) Color.White else Graphite
                )
            }
        }
    }
}

/** Сетка календаря (Figma 2872-34247): строка дней недели 40dp (13/400, графит 85%),
 *  зазор 6, строки дней 44dp без зазоров; дни 13/400: текущий месяц — графит 85%,
 *  соседние/прошедшие — графит 40%, выбранный — тёмный круг с белой цифрой;
 *  сохранённые даты — точка 7dp в правом верхнем углу круга дня. */
@Composable
private fun CalendarGrid(
    displayMonth: YearMonth,
    picked: LocalDate?,
    today: LocalDate,
    markedDates: Set<LocalDate>,
    onPickDay: (LocalDate) -> Unit
) {
    val firstDay = displayMonth.atDay(1)
    val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
    val leadingOffset = firstDay.dayOfWeek.value - 1
    val weeks = (leadingOffset + displayMonth.lengthOfMonth() + 6) / 7
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            WeekdayNames.forEach { wd ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        wd,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = InterFontFamily,
                        color = Graphite.copy(alpha = 0.85f)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        repeat(weeks) { weekIndex ->
            Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                repeat(7) { dayIndex ->
                    val date = gridStart.plusDays((weekIndex * 7 + dayIndex).toLong())
                    val inMonth = date.month == displayMonth.month
                    // Прошедшие дни выбрать нельзя: приглушены, без реакции на тап
                    val isPast = date.isBefore(today)
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
                                .clickable { if (inMonth && !isPast) onPickDay(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = InterFontFamily,
                                color = when {
                                    selected -> Color.White
                                    inMonth && !isPast -> Graphite.copy(alpha = 0.85f)
                                    else -> Graphite.copy(alpha = 0.4f)
                                }
                            )
                            if (inMonth && date in markedDates) {
                                // align(TopStart) обязателен: без него offset
                                // применяется от центра (contentAlignment) и
                                // точка уходит за пределы круга — clip её съедает
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(x = 26.dp, y = 11.dp)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Color.White else Graphite)
                                )
                            }
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
    onDismiss: () -> Unit,
    subtitle: String = "Выберите число, до которого нужно передавать показания каждый месяц"
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
                subtitle,
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

/** Ручка шита 32×4 r100 (по умолчанию #212121; в некоторых шитах серая #79747E). */
@Composable
fun SheetDragHandle(color: Color = Graphite) {
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
                .background(color)
        )
    }
}
