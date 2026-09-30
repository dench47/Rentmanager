package com.rentmanager.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.InterFontFamily

// Общие части списков контактов (арендодатели / арендаторы) по макету канваса 13:
// строка = аватар 40 + имя 15/600 + вторая строка 13/400 + две круглые кнопки 40.

private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF727272)
private val CircleFill = Color(0xFFEFEFEF)
private val DividerColor = Color(0xFFDBDBDB)
/** Зелёный активной аренды (канвас 17): обводка аватара и вторая строка. */
val ActiveGreen = Color(0xFF2F7D4D)
private val ActiveDot = ActiveGreen

/**
 * Состояние аренды в фильтре списка (канвас 17): панель 202×188 #EFEFEF r20,
 * строки 182×28 r10 внутри. Подписи отличаются тем, кто смотрит:
 * арендодатель (экран «Арендаторы», 3919:74391) или арендатор
 * (экран «Арендодатели», 3925:82898).
 */
enum class RentFilter {
    ALL,
    ACTIVE,
    FINISHED,
    BOOKING,
    NONE;

    fun label(asLandlord: Boolean): String = if (asLandlord) {
        when (this) {
            ALL -> "Все"
            ACTIVE -> "Арендуют сейчас"
            FINISHED -> "Арендовали ранее"
            BOOKING -> "Аренда запланирована"
            NONE -> "Без аренды"
        }
    } else {
        when (this) {
            ALL -> "Все"
            ACTIVE -> "Арендую сейчас"
            FINISHED -> "Арендовал(а) ранее"
            BOOKING -> "Аренда запланирована"
            NONE -> "Аренды не было"
        }
    }

    /** Подходит ли контакт под выбранное состояние (статус приходит с сервера). */
    fun matches(status: String?): Boolean = when (this) {
        ALL -> true
        ACTIVE -> status == "active"
        FINISHED -> status == "finished"
        BOOKING -> status == "booking"
        NONE -> status == "none"
    }

    companion object {
        /**
         * Набор состояний для фильтра. У арендатора (экран «Арендодатели»)
         * состояний на одно меньше: владельца нельзя добавить себе вручную —
         * он появляется только если ты у него арендуешь/арендовал/запланировал,
         * поэтому «аренды не было» там не бывает.
         */
        fun options(asLandlord: Boolean): List<RentFilter> =
            if (asLandlord) entries.toList() else listOf(ALL, ACTIVE, FINISHED, BOOKING)
    }
}

/** ISO-дата (YYYY-MM-DD) → «23.12.2027»; уже отображаемый вид отдаём как есть. */
fun formatRentDate(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return ""
    val iso = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(value) ?: return value
    val (y, m, d) = iso.destructured
    return "$d.$m.$y"
}

/**
 * Вторая строка строки-контакта (канвас 17: 3919:74282 — арендаторы,
 * 3108:54896 — арендодатели). Порядок: состояние + дата → « · » → объект.
 * У состояния «без аренды» второй строки нет вовсе.
 *
 * Смотрит арендодатель: «Арендует до 23.12.2027 · БЦ Легенда»,
 * «Завершено 01.02.2024 · Апартаменты 24», «Аренда с 25.09.2026 · Апартаменты 24».
 * Смотрит арендатор: «Арендую до … · Кухня», «Арендовал до … · …», «Аренда с …».
 */
fun rentSubtitle(
    asLandlord: Boolean,
    status: String?,
    propertyTitle: String?,
    rentStartRaw: String?,
    rentEndRaw: String?
): String {
    val title = propertyTitle?.trim().orEmpty()
    val end = formatRentDate(rentEndRaw)
    val start = formatRentDate(rentStartRaw)
    fun withTitle(state: String): String =
        listOf(state, title).filter { it.isNotEmpty() }.joinToString(" · ")

    return when (status) {
        "active" -> withTitle(
            when {
                asLandlord && end.isNotEmpty() -> "Арендует до $end"
                asLandlord -> "Арендует"
                end.isNotEmpty() -> "Арендую до $end"
                else -> "Арендую"
            }
        )

        "finished" -> withTitle(
            when {
                asLandlord && end.isNotEmpty() -> "Завершено $end"
                asLandlord -> "Завершено"
                end.isNotEmpty() -> "Арендовал до $end"
                else -> "Арендовал"
            }
        )

        "booking" -> withTitle(
            if (start.isNotEmpty()) "Аренда с $start" else "Аренда запланирована"
        )

        // «без аренды» — второй строки нет (кадр 3919:74282, «Лебедев Павел»)
        else -> ""
    }
}

/**
 * Аватар 40: фото с аккаунта, иначе силуэт. Активная аренда помечается
 * зелёной обводкой 2 #2F7D4D (канвас 17, 3919:74295) — зелёной точки больше нет.
 */
@Composable
fun ContactAvatar(
    avatarUrl: String?,
    activeRing: Boolean = false,
    size: Dp = 40.dp
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (activeRing) Modifier.border(2.dp, ActiveDot, CircleShape) else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(CircleFill),
            contentAlignment = Alignment.Center
        ) {
            val url = avatarUrl?.trim().orEmpty()
            if (url.isNotEmpty()) {
                coil.compose.AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Силуэт вместо буквы (канвас 13: Avatar Placeholder 28×26 #727272)
                Image(
                    painter = painterResource(R.drawable.ic_avatar_placeholder),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size * 0.7f)
                )
            }
        }
    }
}

/** Круглая кнопка 40×40 #EFEFEF с иконкой 24 внутри. */
@Composable
fun ContactActionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CircleFill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Звонок через системный набор номера. */
fun dialPhone(context: Context, phone: String?) {
    val digits = phone?.filter(Char::isDigit).orEmpty()
    val normalized = if (digits.startsWith("8")) "7" + digits.drop(1) else digits
    if (normalized.length < 10) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$normalized")))
    }
}

/**
 * Две кнопки связи в строке (канвас 13): «письмо» и «звонок».
 * Письмо пока заглушка — решение по действию будет позже, но кнопка
 * обязана быть в обоих списках.
 */
@Composable
fun ContactActionsRow(phone: String?) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ContactActionButton(iconRes = R.drawable.ic_tab_email, contentDescription = "Написать") { }
        ContactActionButton(iconRes = R.drawable.ic_tab_call, contentDescription = "Позвонить") {
            dialPhone(context, phone)
        }
    }
}

/**
 * Строка списка контактов (канвас 13): 72 высотой, аватар 40 + имя 15/600
 * + вторая строка 13/400 #727272 + две круглые кнопки справа.
 */
@Composable
fun ContactRow(
    title: String,
    subtitle: String?,
    avatarUrl: String?,
    activeRing: Boolean,
    phone: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    // У активной аренды вторая строка зелёная (канвас 17, 3919:74282)
    subtitleColor: Color = TextSecondary
) {
    val tapModifier = if (onLongClick != null) {
        Modifier.pointerInput(title, subtitle) {
            detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
        }
    } else {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(tapModifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactAvatar(avatarUrl = avatarUrl, activeRing = activeRing)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    letterSpacing = (-0.4).sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = InterFontFamily,
                        letterSpacing = (-0.4).sp,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        ContactActionsRow(phone = phone)
    }
}

/**
 * Заголовок группы в списке (канвас 17, 3919:74295: «Арендуют сейчас · 3»):
 * 13/500 #727272, отступы 20 слева, 4 сверху, 6 снизу. Показывается только
 * при выбранном фильтре — при поиске заголовки не нужны.
 */
@Composable
fun ContactGroupHeader(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = InterFontFamily,
        letterSpacing = (-0.4).sp,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp)
    )
}

/** Разделитель строки списка контактов: #DBDBDB, отступы 20. */
@Composable
fun ContactRowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(DividerColor)
    )
}

/**
 * Тулбар списка контактов. Обычный вид: назад + заголовок + лупа + фильтр.
 * Режим поиска (канвас 13, 3122:60036): назад + поле 340×50 #EFEFEF r20
 * с запросом и крестиком: пустой запрос — крестик закрывает поиск.
 */
@Composable
fun ContactsToolbar(
    title: String,
    searchActive: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onBack: () -> Unit,
    showActions: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        // Лупа и фильтр — справа (макет: 24 + 8 + 24 на x336/x368, отступы 20/20)
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (searchActive) {
            Image(
                painter = painterResource(R.drawable.ic_landlord_back),
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CircleFill)
                    .padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Поиск",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            letterSpacing = (-0.4).sp,
                            color = TextSecondary
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        cursorBrush = SolidColor(TextPrimary),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            letterSpacing = (-0.4).sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (query.isEmpty()) onBack() else onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_toolbar_close),
                        contentDescription = "Очистить",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
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
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
            }
            if (showActions) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "Поиск",
                        modifier = Modifier.size(24.dp).clickable { onSearchClick() }
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_filter),
                        contentDescription = "Фильтр",
                        modifier = Modifier.size(24.dp).clickable { onFilterClick() }
                    )
                }
            }
        }
    }
}

/**
 * Окошко фильтра (канвас 17, 3919:74391 / 3925:82898): панель 202×188 #EFEFEF,
 * r20, отступы 10/12/10/12, строки 182×28 r10 с зазором 6, подписи 13/400,
 * выбранная — белая плашка и галочка 16 справа.
 *
 * Панель прижимается к ВЕРХУ своей области — её нужно вкладывать в область списка
 * (Box вокруг LazyColumn), тогда верх окошка совпадает с верхом первой строки,
 * как в макете, и не зависит от высоты тулбара. Правый отступ — 20.
 */
@Composable
fun RentFilterPopup(
    selected: RentFilter,
    onSelect: (RentFilter) -> Unit,
    onDismiss: () -> Unit,
    asLandlord: Boolean
) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp)
                .width(202.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CircleFill)
                .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RentFilter.options(asLandlord).forEach { filter ->
                val isSelected = filter == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(filter) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        filter.label(asLandlord),
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        // в макете текст пункта #212121 с прозрачностью 0.85
                        color = TextPrimary.copy(alpha = 0.85f),
                        letterSpacing = (-0.4).sp
                    )
                    if (isSelected) {
                        Image(
                            painter = painterResource(R.drawable.ic_check_black),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
