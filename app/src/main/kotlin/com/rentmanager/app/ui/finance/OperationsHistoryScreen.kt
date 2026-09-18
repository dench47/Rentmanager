package com.rentmanager.app.ui.finance

import androidx.compose.foundation.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.data.api.SubscriptionOperationDto
import com.rentmanager.app.ui.landlord.createproperty.GradientCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.SectionTitleStyle
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardGrey = Color(0xFFEFEFEF)
private val GreenOk = Color(0xFF2F7D4D)
private val RedError = Color(0xFFFF4249)

private val RuMonthGenitive = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

/**
 * «История операций» подписки (канвас 2990:50668): фильтры-чипы
 * «Все / Зачисления / Списания», группы по месяцам, операции-карточки
 * (иконка чека, заголовок/подпись/дата, сумма и статус справа);
 * пустое состояние — иллюстрация + «Пополнить баланс».
 */
@Composable
fun OperationsHistoryScreen(
    onBack: () -> Unit,
    onAddProperty: () -> Unit = {},
    viewModel: OperationsHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Операции приходят в фоне (списания в полночь) — обновляем на возврате
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Шапка по шаблону экрана «Арендодатель»: часы → заголовок = 42dp
        // (Spacer 27 + строка заголовка), а не впритык к статус-бару
        Spacer(Modifier.height(27.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_landlord_back),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "История операций",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Graphite,
                    letterSpacing = (-0.3).sp
                )
            }
        }

        // Чипы через 6 после шапки (замер 2996:42138 низ → 2999:43177 верх)
        Spacer(Modifier.height(6.dp))
        // ---- Фильтры-чипы: активный залит графитом, текст по ширине ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip("Все", state.filter == 0, minWidth = 87.dp) { viewModel.setFilter(0) }
            FilterChip("Зачисления", state.filter == 1) { viewModel.setFilter(1) }
            FilterChip("Списания", state.filter == 2) { viewModel.setFilter(2) }
        }
        // Чипы → список: 20 (замер 2999:43177 низ → «Сентябрь 2026» верх)
        Spacer(Modifier.height(20.dp))

        val filtered = remember(state.operations, state.filter) {
            when (state.filter) {
                1 -> state.operations.filter { it.amount > 0 }
                2 -> state.operations.filter { it.amount < 0 }
                else -> state.operations
            }
        }

        if (filtered.isEmpty()) {
            EmptyOperations(
                filter = state.filter,
                objects = state.objects,
                balance = state.balance,
                // «Пополнить баланс» — возвращаем к экрану подписки,
                // «Показать все операции» — сбрасываем фильтр
                onTopUp = onBack,
                onShowAll = { viewModel.setFilter(0) },
                onAddProperty = onAddProperty
            )
        } else {
            // ---- Группы по месяцам ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val grouped: Map<LocalDate, List<SubscriptionOperationDto>> =
                    filtered.groupBy { op -> viewModel.monthOf(op) }
                        .toSortedMap(compareByDescending { date -> date })
                grouped.forEach { (month, ops) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
                                .replaceFirstChar { it.uppercase(Locale("ru")) },
                            style = SectionTitleStyle
                        )
                        ops.forEachIndexed { index, op ->
                            // внутри месяца операции идут через 6
                            if (index > 0) Spacer(Modifier.height(6.dp))
                            OperationRow(op)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/** Чип-фильтр 44: r100, активный #212121 с белым текстом; ширина —
 *  текст + 40 полей, у короткого «Все» в макете минимум 87 */
@Composable
private fun FilterChip(
    text: String,
    active: Boolean,
    minWidth: androidx.compose.ui.unit.Dp? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .then(minWidth?.let { Modifier.widthIn(min = it) } ?: Modifier)
            .clip(RoundedCornerShape(100.dp))
            .then(if (active) Modifier.background(Graphite) else Modifier.background(CardGrey))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = Headline2MobStyle.copy(color = if (active) Color.White else GreyText)
        )
    }
}

/** Карточка операции 78: иконка в квадрате 42 r10 + тексты + сумма/статус. */
@Composable
private fun OperationRow(op: SubscriptionOperationDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardGrey)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Контейнер иконки 42×42 с внутренними отступами 10/4/8/9 (2990:49766):
        // слева от края пузыря до иконки 10, справа до текста 8+4
        Box(
            modifier = Modifier
                .size(42.dp)
                .padding(start = 10.dp, top = 4.dp, end = 8.dp, bottom = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_receipt),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                op.title ?: when {
                    op.amount < 0 -> "Ежедневное списание"
                    else -> "Пополнение баланса"
                },
                style = Headline2MobStyle.copy(lineHeight = 18.2.sp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                op.subtitle ?: "",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
                lineHeight = 15.7.sp,
                color = GreyText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                runCatching {
                    val dt = java.time.Instant.parse(op.createdAt)
                        .atZone(java.time.ZoneId.systemDefault())
                    dt.toLocalDate().format(RuMonthGenitive) + ", " +
                        dt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                }.getOrDefault(""),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
                lineHeight = 15.7.sp,
                color = GreyText
            )
        }
        Spacer(Modifier.width(17.dp))
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val amountStr = when {
                // Неудачное списание: «30 ₽» без знака (макет 3178-51159)
                op.status == "failed" ->
                    String.format(java.util.Locale.US, "%,.0f ₽", kotlin.math.abs(op.amount)).replace(',', ' ')
                op.amount > 0 -> "+" +
                    String.format(java.util.Locale.US, "%,.0f ₽", kotlin.math.abs(op.amount)).replace(',', ' ')
                else -> "−" +
                    String.format(java.util.Locale.US, "%,.0f ₽", kotlin.math.abs(op.amount)).replace(',', ' ')
            }
            val amountColor = when {
                op.status == "failed" -> GreyText
                op.amount > 0 -> GreenOk
                else -> Graphite
            }
            Text(
                amountStr,
                style = Headline2MobStyle.copy(color = amountColor, lineHeight = 18.2.sp)
            )
            val statusText = when (op.status) {
                "credited" -> "Зачислено"
                // неудачное списание — «Не выполнено» (3178-51159),
                // неудачное пополнение — «Не зачислено» (макет «Все операции»)
                "failed" -> if (op.amount > 0) "Не зачислено" else "Не выполнено"
                else -> if (op.amount > 0) "Начислено" else "Выполнено"
            }
            val statusColor = when (op.status) {
                "credited" -> GreenOk
                "failed" -> RedError
                else -> if (op.amount > 0) GreenOk else GreyText
            }
            Text(
                statusText,
                fontSize = 13.sp,
                // Статус — 600 (замер 2990:49767: 'Зачислено' w=600)
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                lineHeight = 15.7.sp,
                color = statusColor
            )
        }
    }
}

/** Пустое состояние: карточка r30 с иллюстрацией; тексты и кнопка
 *  свои для каждого фильтра (макеты «Все операции»/«Зачисления»/«Списания») */
@Composable
private fun EmptyOperations(
    filter: Int,
    objects: Int,
    balance: Double,
    onTopUp: () -> Unit,
    onShowAll: () -> Unit,
    onAddProperty: () -> Unit
) {
    // «Деньги на балансе есть, объекта ещё нет» (2996:42128): добавляем
    // CTA «Добавить объект» над «Пополнить баланс»
    val noObjectState = objects == 0 && balance > 0
    val title = when (filter) {
        1 -> "Зачислений пока нет"
        2 -> "Списаний пока нет"
        else -> "Операций пока нет"
    }
    // Переносы строк как у дизайнера (файл 12, правка Вики): «здесь»
    // больше не висит одно на второй строке
    val subtitle = when (filter) {
        1 -> "После первого пополнения информация\nпоявится здесь"
        2 -> "После первого списания информация\nпоявится здесь"
        else -> "После первого пополнения или списания\nинформация появится здесь"
    }
    // Карточка пустого состояния — на всю ширину экрана (412, r30),
    // контент внутри с полем 20: левый край CTA совпадает с чипами фильтров
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_empty_operations),
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = Graphite
            )
            Spacer(Modifier.height(12.dp))
            Text(
                subtitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = Graphite,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            // CTA пустого состояния — градиентная (замер: GRADIENT_LINEAR);
            // редкое «нет объекта» — градиент на «Добавить объект»,
            // «Пополнить баланс» рядом контурной (2996:42128)
            if (noObjectState) {
                GradientCtaButton(text = "Добавить объект", onClick = onAddProperty)
                Spacer(Modifier.height(6.dp))
                OutlineCtaButton(text = "Пополнить баланс", borderColor = Graphite, onClick = onTopUp)
            } else if (filter == 2) {
                GradientCtaButton(text = "Показать все операции", onClick = onShowAll)
            } else {
                GradientCtaButton(text = "Пополнить баланс", onClick = onTopUp)
            }
        }
    }
}
