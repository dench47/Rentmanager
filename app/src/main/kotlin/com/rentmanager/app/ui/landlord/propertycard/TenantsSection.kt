package com.rentmanager.app.ui.landlord.propertycard

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.data.model.BookingDto
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.model.TenantDto
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.SectionTitleStyle
import com.rentmanager.app.util.DD_MM_YYYY
import com.rentmanager.app.ui.landlord.myproperties.MonthGenitive
import com.rentmanager.app.util.parseAnyDate

private val TenantTitleStyle = Headline2MobStyle.copy(lineHeight = 18.2.sp)
private val TenantSubtitleStyle = CardSubtitleStyle.copy(lineHeight = 15.7.sp)

/**
 * Секция «Арендаторы» карточки объекта (канвас «17»):
 * пусто — 3801:67571, свёрнуто с шевроном — 3801:67958,
 * раскрыто со списком броней — 3970:83015, аккордеон брони — 3801:67809.
 */
@Composable
fun TenantsSection(
    property: PropertyDto?,
    bookings: List<BookingDto>,
    tenants: Map<String, TenantDto>,
    listExpanded: Boolean,
    onToggleList: () -> Unit,
    expandedBookingId: String?,
    onToggleBooking: (String?) -> Unit,
    onAttachTenant: () -> Unit,
    onDetachTenant: (BookingDto) -> Unit,
    /** Первая загрузка карточки: не показываем пустое состояние, пока данных нет */
    loading: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Заголовок: 18/600; с бронями — строка 40 с каноническим шевроном
        // (поворот 180° при раскрытии), без броней — просто текст
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (bookings.isNotEmpty()) Modifier.height(40.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleList() } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Арендаторы", style = SectionTitleStyle.copy(lineHeight = 22.sp))
            if (bookings.isNotEmpty()) {
                Image(
                    painter = painterResource(R.drawable.ic_card_chevron),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { rotationZ = if (listExpanded) 180f else 0f }
                )
            }
        }
        if (bookings.isEmpty() && !loading) {
            // Пустое состояние (3801:67571): тексты → 20 → CTA
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Арендаторов пока нет", style = TenantTitleStyle)
                Text("Добавьте гостя и укажите даты проживания", style = TenantSubtitleStyle)
            }
        }
        if (!loading || bookings.isNotEmpty()) {
            OutlineCtaButton(
                text = "Прикрепить арендатора",
                iconRes = R.drawable.ic_plus_circle_graphite,
                borderColor = Graphite,
                onClick = onAttachTenant
            )
        }
        if (listExpanded && bookings.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                bookings.forEach { booking ->
                    TenantBookingCard(
                        booking = booking,
                        tenant = booking.tenantId?.let { tenants[it] },
                        property = property,
                        expanded = booking.id == expandedBookingId,
                        onToggle = { onToggleBooking(if (booking.id == expandedBookingId) null else booking.id) },
                        onDetach = onDetachTenant
                    )
                }
            }
        }
    }
}

/**
 * Аккордеон брони (3801:67809): свёрнут — 64, даты + «N гостей · договор №» +
 * шеврон; раскрыт — карандаш в строке заголовка, строка арендатора (аватар 40
 * с зелёной точкой активной брони), Позвонить/Написать 173×55 и чёрная
 * «Открепить арендатора» (только у действующей привязки).
 */
@Composable
private fun TenantBookingCard(
    booking: BookingDto,
    tenant: TenantDto?,
    property: PropertyDto?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDetach: (BookingDto) -> Unit
) {
    val context = LocalContext.current
    // Имя: карточка арендатора по tenant_id; для действующей привязки —
    // tenant_info объекта (прикрепление пишет его туда же)
    val activeLink = property?.tenantId != null && booking.tenantId == property.tenantId
    val name = tenant?.fullName
        ?: (property?.tenantInfo?.takeIf { activeLink && it.isNotBlank() })
    val phone = tenant?.phone?.takeIf { it.isNotBlank() }
        ?: property?.phone?.takeIf { activeLink && it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 10.dp, end = 10.dp)
            // Свёрнутая — 64 (12+40+12); раскрытая — 10 сверху, 20 снизу (3801:67809)
            .then(
                if (expanded) Modifier.padding(top = 10.dp, bottom = 20.dp)
                else Modifier.padding(top = 12.dp, bottom = 12.dp)
            )
    ) {
        // Строка заголовка: период брони | шеврон
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp)
                .height(40.dp)
                .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    bookingRangeTitle(booking.startDate, booking.endDate, property?.rentType == "посуточно"),
                    style = TenantTitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
            )
        }
        if (expanded) {
            BookingDivider(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 11.dp))
            if (name != null) {
                // Строка арендатора: аватар 40 + 8 + имя/компания
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp)
                        .height(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TenantAvatar(url = tenant?.avatarUrl)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            name,
                            style = TenantTitleStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        tenant?.companyName?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = TenantSubtitleStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                BookingDivider(Modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 11.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlineCtaButton(
                        text = "Позвонить",
                        iconRes = R.drawable.ic_tab_call,
                        modifier = Modifier.weight(1f),
                        iconSpacing = 4.dp,
                        borderColor = Graphite.copy(alpha = 0.85f),
                        enabled = phone != null,
                        onClick = {
                            phone?.let {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$it".toUri()))
                                }
                            }
                        }
                    )
                    OutlineCtaButton(
                        text = "Написать",
                        iconRes = R.drawable.ic_chat_message,
                        modifier = Modifier.weight(1f),
                        iconSpacing = 4.dp,
                        borderColor = Graphite.copy(alpha = 0.85f),
                        enabled = phone != null,
                        onClick = {}
                    )
                }
                if (booking.tenantId != null) {   // открепить можно любую бронь
                    com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton(
                        text = "Открепить арендатора",
                        onClick = { onDetach(booking) }
                    )
                }
            }
        }
    }
}

// Figma LINE: 1dp #DBDBDB.
@Composable
private fun BookingDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = Color(0xFFDBDBDB),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    )
}

/**
 * Аватар 40x40: фото карточки арендатора, иначе канонический силуэт на #EFEFEF.
 * Индикаторов активности здесь НЕ рисуем (решение Дениса): в карточке объекта статус виден по датам брони.
 */
@Composable
private fun TenantAvatar(url: String?) {
    Box(modifier = Modifier.size(40.dp)) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            // Без фото — каноническая заглушка канваса 19 (4390:92155):
            // белый круг 40 с контурным человечком #727272
            Image(
                painter = painterResource(R.drawable.ic_myprops_avatar),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}



// Период брони в баяне: помесячный объект — ДД.ММ.ГГГГ («18.09.2026 - 24.09.2026»),
// посуточный — «18-24 сентября» / «18 августа - 24 сентября» (год только при смене года).
internal fun bookingRangeTitle(startIso: String, endIso: String, daily: Boolean): String {
    val s = parseAnyDate(startIso) ?: return startIso
    val e = parseAnyDate(endIso) ?: return endIso
    if (!daily) {
        return if (s == e) s.format(DD_MM_YYYY) else "${s.format(DD_MM_YYYY)} - ${e.format(DD_MM_YYYY)}"
    }
    val sameMonth = s.year == e.year && s.monthValue == e.monthValue
    return when {
        s == e -> "${s.dayOfMonth} ${MonthGenitive[s.monthValue - 1]}"
        sameMonth -> "${s.dayOfMonth}-${e.dayOfMonth} ${MonthGenitive[e.monthValue - 1]}"
        s.year == e.year -> "${s.dayOfMonth} ${MonthGenitive[s.monthValue - 1]} - ${e.dayOfMonth} ${MonthGenitive[e.monthValue - 1]}"
        else -> "${s.dayOfMonth} ${MonthGenitive[s.monthValue - 1]} ${s.year} - ${e.dayOfMonth} ${MonthGenitive[e.monthValue - 1]} ${e.year}"
    }
}

// 1 гость / 2 гостя / 5 гостей
internal fun guestsWord(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "гость"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "гостя"
    else -> "гостей"
}
