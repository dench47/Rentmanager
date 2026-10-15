package com.rentmanager.app.ui.landlord.myproperties

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
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

/** Шаг пункта пикера периода: пункт 30 + зазор 6 (канвас 4390:95317). */
private const val PickerItemStep = 36f

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
    periodEnabled: Boolean = true,
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
                .clickable(enabled = periodEnabled) { onPeriodClick() }
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_myprops_calendar),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .alpha(if (periodEnabled) 1f else 0.4f),
                tint = Color.Unspecified
            )
            Text(
                "Выбрать период",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = if (periodEnabled) CalInk else CalInk.copy(alpha = 0.4f)
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .height(42.dp)
                // Канвас 4390:94065: обводка #212121 при 40% (1px, r100),
                // отступы 11 по краям, зазор иконка↔подпись 6
                .clip(RoundedCornerShape(100.dp))
                .border(1.dp, CalInk.copy(alpha = 0.40f), RoundedCornerShape(100.dp))
                .clickable { onFilterClick() }
                .padding(horizontal = 11.dp),
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
 * Строка поиска вместо панели календаря (канвас 4390:92591): стрелка назад,
 * плашка #EFEFEF r20 (h50) с лупой 20, введённым текстом 15/600 и крестиком
 * «очистить» в круге 16×16 справа.
 */
@Composable
internal fun CalendarSearchToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(50.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp),
                tint = CalInk
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(shape)
                .background(Color(0xFFEFEFEF))
                .padding(start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_myprops_search),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = CalInk
                ),
                cursorBrush = SolidColor(CalInk),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_myprops_clear),
                        contentDescription = "Очистить",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Unspecified
                    )
                }
            }
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
    onDayClick: (CalPiece) -> Unit,
    onOpenProperty: () -> Unit,
    /** Счётчик применений периода: меняется на каждое нажатие в шите периода */
    periodTick: Int = 0,
    /** Период, по которому открыта карточка: его полоса рисуется 2px (4390:95386) */
    emphasized: ClosedRange<LocalDate>? = null
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
        // Полоса ленивая: блоки (год помесячно, месяц посуточно) считаются по
        // требованию, поэтому вперёд листается без ограничения по годам —
        // CAL_BLOCK_LIMIT только страхует от бесконечно длинной прокрутки.
        val blockAt = remember(property, anchor, selection, daily) {
            { index: Int ->
                buildCalBlock(property, anchor, index, monthly = !daily, selection = selection)
            }
        }
        val labelAt = remember(anchor, daily) {
            { index: Int -> calBlockLabel(anchor, index, monthly = !daily) }
        }
        // Помесячный вид открывается НА выбранном месяце (пояснение 4390:95394:
        // «помесячные — на соответствующем месяце»): первый блок — год якоря,
        // поэтому сдвигаем скролл на (месяц − 1) × шаг месяца. Посуточно первый
        // блок уже начинается с 1-го числа выбранного месяца, сдвиг 0.
        val density = LocalDensity.current
        val offsetPx = remember(anchor, daily, density) {
            if (daily) 0
            else with(density) {
                ((anchor.monthValue - 1) * (CAL_CELL_W + CAL_GAP)).dp.roundToPx()
            }
        }
        // Состояние создаётся на каждый якорь: смена периода всегда открывает
        // ленту на нужном месяце (посуточно — с 1-го числа, помесячный — со своего)
        val listState = remember(anchor, daily, periodTick) { LazyListState(0, offsetPx) }
        PropertyCalendar(
            blockCount = CAL_BLOCK_LIMIT,
            blockAt = blockAt,
            labelAt = labelAt,
            onDayClick = onDayClick,
            listState = listState,
            emphasized = emphasized,
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
            .padding(horizontal = 10.dp, vertical = 5.dp),
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
        Row(
            modifier = Modifier.align(Alignment.Top),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                propertyName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = CalInk,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            // Подпись периода в канвасе приглушена: прозрачность 0.85
            Text(
                periodText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = CalInk.copy(alpha = 0.85f),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
                    // Пока период не закрыт — подпись кнопки серая (прозрачность 0.40 по канвасу)
                    color = if (periodComplete) CalInk else CalInk.copy(alpha = 0.40f)
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
    tenantPhone: String? = null,
    onClose: () -> Unit,
    onEmail: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Color(0xFFEFEFEF))
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
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
            // Плашка долга по канвасу 4390:91653: 336×38, фон #FBEAEC, r10,
            // текст #FF4249 15/600 по центру
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFBEAEC)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    overdueText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = Color(0xFFFF4249),
                    textAlign = TextAlign.Center
                )
            }
        }
        if (tenantName != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                val context = LocalContext.current
                ActionCircle(R.drawable.ic_myprops_call, "Позвонить") {
                    val digits = tenantPhone?.filter { it.isDigit() || it == '+' }
                    if (!digits.isNullOrBlank()) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
                            )
                        }
                    }
                }
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
    // Годы только текущий и вперёд: прошлые месяцы бронировать нельзя
    val currentYear = remember { YearMonth.now().year }
    val years = remember(currentYear) { (currentYear..currentYear + 12).toList() }
    // Подписи годов считаем один раз: новый список на каждую рекомпозицию
    // перезапускал бы подписку на центр колонки
    val yearLabels = remember(years) { years.map { it.toString() } }
    // Обе колонки пишут в один draft: обработчик может сработать со старым
    // замыканием, поэтому всегда применяем к актуальному значению — иначе
    // тап по году после тапа по месяцу возвращал бы прежний месяц.
    val currentDraft by rememberUpdatedState(draft)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = ObjectsWhite,
        // Ручка по канвасу 4390:95317: 32×4, #212121 при 40%
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 32.dp,
                height = 4.dp,
                color = CalInk.copy(alpha = 0.40f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
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
                    .padding(horizontal = 30.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SnapPickerColumn(
                    labels = CalendarMonthNames,
                    selectedIndex = draft.monthValue - 1,
                    looped = true,
                    onSelected = { index ->
                        onDraftChange(currentDraft.withMonth(index + 1))
                    }
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(240.dp)
                        .background(CalInkGray)
                )
                SnapPickerColumn(
                    labels = yearLabels,
                    selectedIndex = years.indexOf(draft.year).coerceAtLeast(0),
                    onSelected = { index ->
                        onDraftChange(currentDraft.withYear(years[index]))
                    }
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

/**
 * Колонка пикера (канвас 4390:95317): 110×246, пункты 110×30 r20 (шаг 36), снап,
 * центр = выбранный. Цвет пункта затухает по расстоянию до центра: у выбранного
 * 100% (чёрная плашка), d2 → 85%, d3 → 40%, дальше светлее.
 * [looped] — бесконечная зацикленная прокрутка (колонка месяцев): список длинный,
 * подпись берётся по модулю, старт по центру на выбранном, наружу уходит логический
 * индекс (0..size-1).
 */
@Composable
private fun SnapPickerColumn(
    labels: List<String>,
    selectedIndex: Int,
    looped: Boolean = false,
    onSelected: (Int) -> Unit
) {
    val size = labels.size.coerceAtLeast(1)
    val repeats = if (looped) 201 else 1
    val count = size * repeats
    val initialIndex = if (looped) (repeats / 2) * size + selectedIndex.coerceIn(0, size - 1)
    else selectedIndex.coerceIn(0, size - 1)

    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    // Колбэк читаем «на момент вызова»: подписка на центр живёт дольше одной
    // рекомпозиции, и старое замыкание перетирало бы уже сделанный выбор
    // (тап по месяцу → потом тап по году возвращал прежний месяц).
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    // Центральный пункт = пункт под центром окна колонки. Считаем по самой прокрутке:
    // при остатке 0 пункт firstVisibleItemIndex стоит ровно по центру (padding 108 dp
    // = (246 − 30) / 2), а при остатке больше половины шага (30 + зазор 6 → 18)
    // к центру ближе уже следующий. Через layoutInfo (координаты смещены на
    // contentPadding) выбор «плавал» на месяц — отсюда «то работает, то нет».
    // Шаг берём в пикселях: раньше offset в px делился на шаг в dp, из-за чего
    // плашка подсвечивала не тот пункт во время прокрутки.
    val itemStepPx = with(LocalDensity.current) { PickerItemStep.dp.toPx() }
    val halfStepPx = (itemStepPx / 2f).toInt()
    fun centeredIndex(): Int = state.firstVisibleItemIndex +
        if (state.firstVisibleItemScrollOffset >= halfStepPx) 1 else 0
    LaunchedEffect(state, labels, looped) {
        snapshotFlow { centeredIndex() }
            .distinctUntilChanged()
            .collect { index ->
                val logical = index.coerceIn(0, count - 1) % size
                if (logical != currentSelectedIndex) currentOnSelected(logical)
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
        items(count) { index ->
            // Непрерывное расстояние до центра: градиент не «прыгает» при прокрутке
            val center = state.firstVisibleItemIndex +
                state.firstVisibleItemScrollOffset / itemStepPx
            val distance = kotlin.math.abs(index - center)
            val alpha = when {
                distance < 1.5f -> 1f
                distance < 2.5f -> 0.85f
                distance < 3.5f -> 0.40f
                else -> 0.15f
            }
            val selected = distance < 0.5f
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 30.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) CalSelected else Color.Transparent)
                    .clickable {
                        currentOnSelected(index % size)
                        // Тап — пункт доезжает до центра: пояснение 4390:95396
                        // «центральные значения становятся выбранными»
                        scope.launch { state.animateScrollToItem(index) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    labels[index % size],
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = if (selected) ObjectsWhite else CalInk.copy(alpha = alpha),
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = ObjectsWhite,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 32.dp,
                height = 4.dp,
                color = CalInk.copy(alpha = 0.40f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
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
                // Канвас 4390:95664: в шапке крестик (24 в боксе 40) — закрывает шит
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onDismiss() },
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
            ObjectsFilter.entries.forEachIndexed { index, option ->
                val isSelected = option == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (index == 0) 48.dp else 46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(option) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        option.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                        // Канвас 4390:95664: подписи чёрные у ВСЕХ пунктов,
                        // серого цвета в шите нет; выбранный помечен галочкой
                        color = CalInk,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_myprops_check),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                }
                if (index != ObjectsFilter.entries.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(1.dp)
                            .background(ObjectsDivider)
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
    LegendEntry("Выбранный период", "Даты новой аренды до сохранения", CalSelectedPeriod, null),
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = ObjectsWhite,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 32.dp,
                height = 4.dp,
                color = CalInk.copy(alpha = 0.40f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
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
        Image(
            painter = painterResource(
                if (hasProperties && filter != ObjectsFilter.MONTHLY) R.drawable.ic_myprops_stub_daily
                else R.drawable.ic_myprops_stub_monthly
            ),
            contentDescription = null,
            modifier = Modifier.size(140.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = StubInk,
            textAlign = TextAlign.Center
        )
        Text(
            hint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.4).sp,
            color = CalInk.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        StubButton(primaryTitle, filled = true, onClick = primaryAction)
        StubButton(secondaryTitle, filled = false, onClick = secondaryAction)
    }
}

/** Тёмный цвет элементов заглушки — Graphite/Icon #010101. */
private val StubInk = Color(0xFF010101)

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
                if (filled) Modifier.background(StubInk)
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
