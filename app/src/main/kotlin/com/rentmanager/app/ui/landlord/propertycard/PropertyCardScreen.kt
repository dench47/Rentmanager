package com.rentmanager.app.ui.landlord.propertycard

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.PropertyNameStyle
import com.rentmanager.app.ui.theme.TextIconeStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private val GreenIcon = Color(0xFFE5F2E7)
private val GreenText = Color(0xFF2F7D4D)
private val BrandTint = Color(0xFFFFF1CF)
private val White50 = Color(0x80FFFFFF)

// Карточка объекта (Figma 2Y1uc9owPaF7N9jzQhhuIr, node 2574:20588)
@Composable
fun PropertyCardScreen(
    propertyId: String,
    onBack: () -> Unit,
    onPaymentSchedule: (String) -> Unit,
    viewModel: PropertyCardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(propertyId) { viewModel.load(propertyId) }
    val property = uiState.property
    var tenantExpanded by remember { mutableStateOf(false) }
    var serviceExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.clickable(onClickLabel = "Назад") { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_landlord_back),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Карточка объекта", style = ToolbarTitleStyle)
            }
            Image(
                painter = painterResource(R.drawable.ic_toolbar_more),
                contentDescription = "Ещё",
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            PhotoSlider(property)

            // Белая панель (наложение на фото, offset -19dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-19).dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (property != null) {
                    Text(property.name.ifBlank { "Без названия" }, style = PropertyNameStyle)
                    Text(property.address, style = Headline2MobStyle.copy(color = GreyText))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenIcon)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Задолженностей нет", style = Headline2MobStyle.copy(color = GreenText))
                }

                // Аренда и платежи
                SectionHeader("Аренда и платежи")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InfoPair("Арендная плата", property?.let { rentText(it) } ?: "—", Modifier.weight(1f))
                        InfoPair("Срок аренды", property?.rentType ?: "—", Modifier.weight(1f))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlineCtaButton(
                            text = "График платежей и реквизиты",
                            iconRes = R.drawable.ic_payment_schedule,
                            onClick = { onPaymentSchedule(propertyId) }
                        )
                        OutlineCtaButton(
                            text = "Добавить расходы",
                            iconRes = R.drawable.ic_expense_receipt,
                            onClick = {}
                        )
                    }
                }

                // Арендатор и договор
                SectionHeader("Арендатор и договор")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Арендатор", style = CardSubtitleStyle, modifier = Modifier.weight(1f))
                        Text("Договор", style = CardSubtitleStyle, modifier = Modifier.weight(1f))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlineCtaButton(
                                text = "Позвонить",
                                iconRes = R.drawable.ic_call_phone,
                                modifier = Modifier.weight(1f),
                                onClick = {}
                            )
                            OutlineCtaButton(
                                text = "Написать",
                                iconRes = R.drawable.ic_chat_message,
                                modifier = Modifier.weight(1f),
                                onClick = {}
                            )
                        }
                        BlackCtaButton(text = "Прикрепить арендатора", onClick = {})
                    }
                }
// Об объекте
                SectionHeader("Об объекте")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ValueCard("Название", property?.name ?: "", Modifier.fillMaxWidth())
                    ValueCard("Адрес", property?.address ?: "", Modifier.fillMaxWidth())
                    ValueCard("Кол-во комнат", property?.rooms ?: "", Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ValueCard(
                            "Площадь, м2",
                            property?.area?.let {
                                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            } ?: "",
                            Modifier.weight(1f)
                        )
                        ValueCard("Спальные места", property?.sleepingPlaces ?: "", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ValueCard("Этаж", property?.floor ?: "", Modifier.weight(1f))
                        ValueCard("Этажей в доме", property?.floorsInHouse ?: "", Modifier.weight(1f))
                    }
                    ValueCard2Lines(
                        "Описание объявления",
                        "Эта информация будет видна в объявлении",
                        property?.description.orEmpty()
                    )
                    ValueCard(
                        if (property?.rentType == "длительно") "Стоимость за месяц, ₽" else "Стоимость за сутки, ₽",
                        property?.rentAmount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "",
                        Modifier.fillMaxWidth()
                    )
                }

                // Публикация
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GradientFullButton("Опубликовать объявление") {}
                    Text(
                        "После публикации объявление станет доступно для просмотра арендаторам",
                        style = TextIconeStyle.copy(color = GreyText)
                    )
                }

                // Аккордеоны (Figma 20741: между ними gap 12, до «Счетчиков» — вплотную, без отступа)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccordionCard(
                        "Информация об объекте",
                        "Эта информация будет видна арендатору",
                        tenantExpanded,
                        onToggle = { tenantExpanded = !tenantExpanded }
                    ) {
                        DetailsContent(property)
                    }
                    AccordionCard(
                        "Служебная информация",
                        "Эта информация видна только вам",
                        serviceExpanded,
                        onToggle = { serviceExpanded = !serviceExpanded }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp)) {
                            Text(property?.serviceInfo?.ifBlank { "—" } ?: "—", style = CardSubtitleStyle.copy(color = Graphite))
                        }
                    }
                }
            }

            // Счетчики
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFFFF1CF),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("Счетчики", pencilRes = R.drawable.ic_edit_pencil_white)
                BlackCtaButton(text = "Добавить счетчики", iconRes = R.drawable.ic_plus_circle, onClick = {})
            }
        }

        // Таббар — статичный (закреплён внизу, не скроллится).
        // ОДИН слой rgba(237,237,237,0.6) (Figma 2574:20590) на весь блок — и на строку таббара,
        // и на зону системной навигации ниже, поэтому они всегда «в один цвет» (Figma 2574:20589);
        // скругление верхних углов 30dp — через clip на Box.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color(0x99EDEDED))
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(196.dp)
                        .height(55.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Graphite),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Финансовый отчет объекта", style = CardSubtitleStyle.copy(color = Color.White))
                }
                TabButton("Позвонить", R.drawable.ic_tab_call) {}
                TabButton("Написать", R.drawable.ic_tab_email) {}
            }
        }
    }
}
// ---------- вспомогательные ----------

@Composable
private fun PhotoSlider(property: PropertyDto?) {
    val photos = property?.photos?.mapNotNull { it.url } ?: emptyList()
    val pagerState = rememberPagerState { photos.size }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(316.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
    ) {
        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет фотографий", style = Headline2MobStyle.copy(color = GreyText))
            }
        } else {
            // Свайп фотографий (Figma 2574:20665): горизонтальный пейджер на всю площадь фото
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { idx ->
                AsyncImage(
                    model = photos[idx],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Pager (Figma 20667): активная точка 20x8 #FFFFFF, неактивные 8x8 белые 50%, снизу 39
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 39.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                photos.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (i == pagerState.currentPage) 20.dp else 8.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (i == pagerState.currentPage) Color.White else White50)
                    )
                }
            }
            // Стрелки (Figma 20673/20674): белый шеврон 40x40, отступ 20 от краёв, по центру вертикали;
            // подложка — лёгкое затемнение 8% (в макете подложки нет; компромисс для читаемости на светлых фото)
            Image(
                painter = painterResource(R.drawable.ic_slider_arrow_left),
                contentDescription = "Назад",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x14000000))
                    .clickable {
                        val current = pagerState.currentPage
                        if (current > 0) scope.launch { pagerState.animateScrollToPage(current - 1) }
                    }
            )
            Image(
                painter = painterResource(R.drawable.ic_slider_arrow_right),
                contentDescription = "Вперёд",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x14000000))
                    .clickable {
                        val current = pagerState.currentPage
                        if (current < photos.size - 1) scope.launch { pagerState.animateScrollToPage(current + 1) }
                    }
            )
        }

        // Плашка «Не опубликовано» и карандаш — ПОВЕРХ фото (Figma 20675/20676/20679):
        // отступы top/start/end = 20, плашка на белом фоне radius 10, карандаш на белом круге.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF212121))
                )
                Text("Не опубликовано", style = Headline2MobStyle.copy(color = Color(0xFF212121)))
            }
            Image(
                painter = painterResource(R.drawable.ic_edit_pencil_white),
                contentDescription = "Редактировать",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, pencilRes: Int = R.drawable.ic_edit_pencil) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = ToolbarTitleStyle)
        Image(
            painter = painterResource(pencilRes),
            contentDescription = "Редактировать",
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun InfoPair(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = CardSubtitleStyle)
        Text(value, style = Headline2MobStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ValueCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, style = CardSubtitleStyle)
        Text(value.ifBlank { "—" }, style = Headline2MobStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ValueCard2Lines(label: String, sub: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (value.isBlank()) label else value, style = Headline2MobStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = CardSubtitleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable
private fun TabButton(label: String, iconRes: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .height(55.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Image(painter = painterResource(iconRes), contentDescription = label, modifier = Modifier.size(24.dp))
        Text(label, style = TextIconeStyle)
    }
}

private fun rentText(p: PropertyDto): String {
    val amount = p.rentAmount ?: 0.0
    val formatted = if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
    return "$formatted ₽ / ${if (p.rentType == "длительно") "месяц" else "сутки"}"
}

@Composable
private fun AccordionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onToggle)
                .padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = Headline2MobStyle)
                Text(subtitle, style = CardSubtitleStyle)
            }
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        if (expanded) content()
    }
}

@Composable
private fun DetailsContent(property: PropertyDto?) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailRow("Номер телефона", property?.phone.orEmpty())
        DetailRow("Пароль WiFi", property?.wifiPassword.orEmpty())
        DetailRow("Правила объекта", property?.houseRules.orEmpty())
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = CardSubtitleStyle)
        Text(value.ifBlank { "—" }, style = Headline2MobStyle.copy(color = Graphite))
    }
}

@Composable
private fun GradientFullButton(text: String, onClick: () -> Unit) {
    val density = LocalDensity.current
    val brush = remember(density) {
        with(density) {
            val w = 372.dp.toPx()
            val h = 55.dp.toPx()
            val angle = Math.toRadians(136.0)
            val dx = sin(angle).toFloat()
            val dy = (-cos(angle)).toFloat()
            val len = abs(w * dx) + abs(h * dy)
            val cc = Offset(w / 2f, h / 2f)
            val half = Offset(dx * len / 2f, dy * len / 2f)
            Brush.linearGradient(
                colorStops = arrayOf(
                    0.19f to Color(0xFFF6D85E),
                    0.60f to Color(0xFFE89B5A),
                    1.00f to Color(0xFFD97D5D)
                ),
                start = cc - half,
                end = cc + half
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(brush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = Headline2MobStyle)
    }
}