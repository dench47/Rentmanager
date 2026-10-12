package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.filter
import com.rentmanager.app.R
import com.rentmanager.app.util.hasPropertyName
import com.rentmanager.app.util.propertyTitle
import java.time.LocalDate
import java.time.YearMonth

// =====================================================================
//  «Моя недвижимость», канвас 19 (node 4390:91245): панель календаря,
//  карточка объекта с полосой-календарём и нижние панели.
//  Замеры и правила — c19_canon.md
// =====================================================================

private val ObjectsDivider = Color(0xFFDBDBDB)
private val ObjectsWhite = Color(0xFFFFFFFF)

/** Фильтр списка объектов: подпись в пилюле и правило отбора. */
internal enum class ObjectsFilter(val label: String) {
    ALL("Все объекты"),
    DAILY("Посуточно"),
    MONTHLY("Помесячно");

    fun matches(rentType: String): Boolean = when (this) {
        ALL -> true
        DAILY -> rentType == "посуточно"
        MONTHLY -> rentType != "посуточно"
    }
}

/**
 * Панель календаря: слева «📅 Выбрать период» 15/600 (144×42), справа пилюля
 * 127×42 r100 с иконкой-слайдерами и подписью фильтра 13/500 (c19_canon.md §1).
 */
@Composable
internal fun CalendarToolbar(
    filter: ObjectsFilter,
    onPeriodClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onPeriodClick() }
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_myprops_calendar),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Text(
                "Выбрать период",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = CalInk
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .height(42.dp)
                .clip(RoundedCornerShape(100.dp))
                .clickable { onFilterClick() }
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_myprops_sliders),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Text(
                filter.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp,
                color = CalInk
            )
        }
    }
}

/**
 * Карточка объекта: 372×173 r20 (обводка #DBDBDB), пад top 10 / bottom 16,
 * шапка `Namecard_2` 370×51 (пад 10/5) и полоса-календарь 82 (c19_canon.md §1, §3).
 */
@Composable
internal fun CalendarObjectCard(
    property: MyPropertyItem,
    anchor: YearMonth,
    selection: ClosedRange<LocalDate>?,
    onDayClick: (LocalDate) -> Unit,
    onDayLongClick: (LocalDate) -> Unit,
    onOpenProperty: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val daily = property.rentType == "посуточно"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, ObjectsDivider, shape)
            .background(ObjectsWhite)
            .clickable { onOpenProperty() }
            .padding(top = 10.dp, bottom = 16.dp)
    ) {
        NamecardRow(property)
        Spacer(Modifier.height(12.dp))
        val blocks = remember(property, anchor, selection, daily) {
            buildCalBlocks(property, anchor, monthly = !daily, selection = selection)
        }
        PropertyCalendar(
            blocks = blocks,
            onDayClick = onDayClick,
            onDayLongClick = onDayLongClick,
            scrollState = rememberScrollState(),
            modifier = Modifier.padding(start = 1.dp)
        )
    }
}

/** Шапка карточки: фото 40×40 r8, название 15/600, адрес 13/400, тип аренды 13/400. */
@Composable
private fun NamecardRow(property: MyPropertyItem) {
    val daily = property.rentType == "посуточно"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            Text(
                propertyTitle(property.name, property.address),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = CalInk,
                maxLines = 1
            )
            if (hasPropertyName(property.name)) {
                Spacer(Modifier.height(4.dp))
                Text(
                    property.address,
                    fontSize = 13.sp,
                    letterSpacing = (-0.4).sp,
                    color = CalInkGray,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(8.dp))
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
                color = CalInkGray
            )
        }
    }
}

/** Фирменный градиент CTA: #F6D85E → #E89B5A → #D97D5D (канвас, панель `payments`). */
private val CtaGradient = Brush.linearGradient(
    0f to Color(0xFFF6D85E),
    0.5f to Color(0xFFE89B5A),
    1f to Color(0xFFD97D5D)
)

/**
 * Панель `payments` — выбор периода: 412×212, фон #EDEDED, скругление сверху r30,
 * пад 20/16/20/10, gap 12. CTA «Добавить арендатора»: контурная, пока период не
 * закрыт («выберите день выезда»), и с градиентом, когда период готов.
 */
@Composable
internal fun SelectionPanel(
    propertyName: String,
    periodText: String,
    periodComplete: Boolean,
    onAddTenant: () -> Unit,
    onCancel: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFEDEDED))
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                propertyName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = CalInk,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Text(
                periodText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = CalInk,
                maxLines = 1
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val ctaShape = RoundedCornerShape(100.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(ctaShape)
                    .then(
                        if (periodComplete) Modifier.background(CtaGradient)
                        else Modifier.border(1.dp, CalInk, ctaShape)
                    )
                    .clickable(enabled = periodComplete) { onAddTenant() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Добавить арендатора",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = CalInk
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(ctaShape)
                    .border(1.dp, CalInk, ctaShape)
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Отменить",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = CalInk
                )
            }
        }
    }
}

/**
 * Панель `card_inf` — по тапу на занятую дату: 376×122/166 r20 #EFEFEF,
 * пад 10/10/10/6, gap 6, тени 0/1 r3 и 0/4 r8 sp3 (c19_canon.md §2).
 */
@Composable
internal fun PeriodInfoPanel(
    periodText: String,
    overdueText: String?,
    tenantName: String?,
    onClose: () -> Unit,
    onEmail: () -> Unit,
    onCall: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Color(0xFFEFEFEF))
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                periodText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = CalInk,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_myprops_close),
                    contentDescription = "Закрыть",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ObjectsDivider)
        )
        if (overdueText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFBEAEC))
                    .padding(10.dp)
            ) {
                Text(
                    overdueText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = Color(0xFFFF4249)
                )
            }
        }
        if (tenantName != null) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_myprops_avatar),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    tenantName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = CalInk,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                ActionCircle(R.drawable.ic_myprops_email, "Написать", onEmail)
                Spacer(Modifier.width(8.dp))
                ActionCircle(R.drawable.ic_myprops_call, "Позвонить", onCall)
            }
        }
    }
}

/** Круглая кнопка действия арендатора: 40×40 r30, фон #FFFFFF, иконка 24×24. */
@Composable
private fun ActionCircle(iconRes: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(ObjectsWhite)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
    }
}

/**
 * Шит «Показать период» (412×532): заголовок 20/600 + подпись 13/400, пикер
 * 352×280 r20 (колонки месяцев и годов 110×30 r20, шаг 36; выбранный пункт —
 * плашка #212121 с белым текстом 15/600), кнопки «Показать» и «Текущий месяц».
 * Колонки снапятся к пункту (c19_canon.md §4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarPeriodSheet(
    draft: YearMonth,
    onDraftChange: (YearMonth) -> Unit,
    onApply: () -> Unit,
    onCurrentMonth: () -> Unit,
    onDismiss: () -> Unit
) {
    val years = remember { (2024..2036).toList() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = ObjectsWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Показать период",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalInk
                )
                Text(
                    "Выбранный период откроется в календарях всех объектов",
                    fontSize = 13.sp,
                    letterSpacing = (-0.2).sp,
                    color = CalInkGray
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, ObjectsDivider, RoundedCornerShape(20.dp))
                    .padding(horizontal = 30.dp, vertical = 17.dp),
                horizontalArrangement = Arrangement.spacedBy(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SnapPickerColumn(
                    labels = CalendarMonthNames,
                    selectedIndex = draft.monthValue - 1,
                    onSelected = { index -> onDraftChange(draft.withMonth(index + 1)) }
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(240.dp)
                        .background(CalInkGray)
                )
                SnapPickerColumn(
                    labels = years.map { it.toString() },
                    selectedIndex = years.indexOf(draft.year).coerceAtLeast(0),
                    onSelected = { index -> onDraftChange(draft.withYear(years[index])) }
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val shape = RoundedCornerShape(100.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(shape)
                        .background(CalSelected)
                        .clickable { onApply() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Показать",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                        color = ObjectsWhite
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(shape)
                        .border(1.dp, CalInk, shape)
                        .clickable { onCurrentMonth() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Текущий месяц",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                        color = CalInk
                    )
                }
            }
        }
    }
}

/** Колонка пикера: 110×246, пункты 110×30 r20 (шаг 36), снап, центр = выбранный. */
@Composable
private fun SnapPickerColumn(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, labels.lastIndex.coerceAtLeast(0))
    )
    LaunchedEffect(state, labels) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .collect {
                val index = state.firstVisibleItemIndex.coerceIn(0, labels.lastIndex.coerceAtLeast(0))
                if (index != selectedIndex) onSelected(index)
            }
    }
    LazyColumn(
        state = state,
        modifier = Modifier
            .width(110.dp)
            .height(246.dp),
        contentPadding = PaddingValues(vertical = 108.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        flingBehavior = rememberSnapFlingBehavior(state)
    ) {
        itemsIndexed(labels) { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 30.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) CalSelected else Color.Transparent)
                    .clickable { onSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = if (selected) ObjectsWhite else CalInk,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Шит «Показать объекты» (412×288): заголовок 20/600 с иконкой-слайдерами,
 * пункты «Все объекты» / «Посуточно» / «Помесячно» 18/600 — применяется сразу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObjectsFilterSheet(
    selected: ObjectsFilter,
    onSelect: (ObjectsFilter) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = ObjectsWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Показать объекты",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalInk,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_myprops_sliders),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.Unspecified
                )
            }
            ObjectsFilter.entries.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        option.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                        color = if (isSelected) CalInk else CalInkGray
                    )
                }
            }
        }
    }
}

/** Строка легенды календаря: плашка 48×38, название 15/600, пояснение 13/400. */
private data class LegendEntry(
    val title: String,
    val hint: String,
    val fill: Color,
    val stroke: Color?
)

private val LegendEntries = listOf(
    LegendEntry("Объект занят", "Аренда идет сейчас", CalBusyBg, CalBusyStroke),
    LegendEntry("Аренда запланирована", "На этот период добавлена аренда", CalPlannedBg, CalPlannedStroke),
    LegendEntry("Свободно", "На эту дату аренда не добавлена", CalFree, null),
    LegendEntry("Выбранный период", "Даты новой аренды до сохранения", CalSelected, null),
    LegendEntry(
        "Есть задолженность",
        "Красная обводка обозначает просроченную оплату",
        CalBusyBg,
        CalOverdueStroke
    )
)

/**
 * Шит «Как устроен календарь» (412×488): «Обозначения» 15/600 + 5 строк по 40
 * и «Как пользоваться» 15/600 + 2 подсказки (c19_canon.md §4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarLegendSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = ObjectsWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Как устроен календарь",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = CalInk
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Обозначения",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalInk
                )
                LegendEntries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shape = RoundedCornerShape(4.dp)
                        Box(
                            Modifier
                                .size(width = 48.dp, height = 38.dp)
                                .clip(shape)
                                .background(entry.fill)
                                .then(
                                    if (entry.stroke != null) Modifier.border(1.dp, entry.stroke, shape)
                                    else Modifier
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                entry.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.4).sp,
                                color = CalInk
                            )
                            Text(
                                entry.hint,
                                fontSize = 13.sp,
                                letterSpacing = (-0.2).sp,
                                color = CalInkGray
                            )
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Как пользоваться",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalInk
                )
                LegendHint("Нажмите на занятую дату, чтобы увидеть арендатора, срок аренды и задолженность")
                LegendHint("Для новой аренды выберите начало и окончание периода")
            }
        }
    }
}

/** Подсказка «Как пользоваться»: буллет-стрелка 24×24 + текст 15/600. */
@Composable
private fun LegendHint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_myprops_bullet),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = CalInk
        )
    }
}

/**
 * Заглушки списка (c19_canon.md §5): «Помесячных/Посуточных объектов пока нет»
 * («Показать все объекты» + «Создать объект») либо «Добавьте первый объект»
 * («Добавить первый объект» + «Управление подпиской»).
 */
@Composable
internal fun PropertiesStub(
    filter: ObjectsFilter,
    hasProperties: Boolean,
    onShowAll: () -> Unit,
    onCreateProperty: () -> Unit,
    onManageSubscription: () -> Unit
) {
    val title: String
    val hint: String
    val primaryTitle: String
    val primaryAction: () -> Unit
    val secondaryTitle: String
    val secondaryAction: () -> Unit

    if (hasProperties) {
        title = if (filter == ObjectsFilter.MONTHLY) "Помесячных объектов пока нет"
        else "Посуточных объектов пока нет"
        hint = "Посмотрите все объекты или добавьте новый"
        primaryTitle = "Показать все объекты"
        primaryAction = onShowAll
        secondaryTitle = "Создать объект"
        secondaryAction = onCreateProperty
    } else {
        title = "Добавьте первый объект"
        hint = "Ведите аренду, платежи, договоры и показания счетчиков в одном месте"
        primaryTitle = "Добавить первый объект"
        primaryAction = onCreateProperty
        secondaryTitle = "Управление подпиской"
        secondaryAction = onManageSubscription
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = CalInk,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            hint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.2).sp,
            color = CalInkGray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        StubButton(primaryTitle, filled = true, onClick = primaryAction)
        Spacer(Modifier.height(6.dp))
        StubButton(secondaryTitle, filled = false, onClick = secondaryAction)
    }
}

/** Кнопка заглушки: 372×55 r100, текст 15/600. */
@Composable
private fun StubButton(title: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(shape)
            .then(
                if (filled) Modifier.background(CalSelected)
                else Modifier.border(1.dp, CalInk, shape)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = if (filled) ObjectsWhite else CalInk
        )
    }
}

// ===================== Подписи периодов (как в канвасе) =====================

private val MonthGenitive = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
)

private val MonthNominative = listOf(
    "январь", "февраль", "март", "апрель", "май", "июнь",
    "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
)

/** «15 - 16 сентября 2026» (сутки) или «август - декабрь 2026» (месяцы). */
internal fun formatCalendarPeriod(start: LocalDate, end: LocalDate, daily: Boolean): String {
    if (!daily) {
        val from = YearMonth.from(start)
        val to = YearMonth.from(end)
        return if (from == to) {
            "${MonthNominative[from.monthValue - 1]} ${from.year}"
        } else {
            "${MonthNominative[from.monthValue - 1]} - ${MonthNominative[to.monthValue - 1]} ${to.year}"
        }
    }
    return if (start == end) {
        "${start.dayOfMonth} ${MonthGenitive[start.monthValue - 1]} ${start.year}"
    } else {
        "${start.dayOfMonth} - ${end.dayOfMonth} ${MonthGenitive[end.monthValue - 1]} ${end.year}"
    }
}

private fun nightsLabel(nights: Int): String {
    val last = nights % 10
    val lastTwo = nights % 100
    return when {
        last == 1 && lastTwo != 11 -> "$nights ночь"
        last in 2..4 && lastTwo !in 12..14 -> "$nights ночи"
        else -> "$nights ночей"
    }
}

/**
 * Подпись выбора в панели `payments`: «2 - 4 октября 2026, 2 ночи», а пока
 * не выбрана дата выезда — «16 сентября 2026 - выберите день выезда».
 */
internal fun formatSelectionPeriod(start: LocalDate?, end: LocalDate?, daily: Boolean): String {
    if (start == null) return "Выберите дату"
    if (end == null) {
        val base = "${start.dayOfMonth} ${MonthGenitive[start.monthValue - 1]} ${start.year}"
        return if (daily) "$base - выберите день выезда" else base
    }
    val base = formatCalendarPeriod(start, end, daily)
    if (!daily) return base
    val nights = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(0)
    return if (nights > 0) "$base, ${nightsLabel(nights)}" else base
}
