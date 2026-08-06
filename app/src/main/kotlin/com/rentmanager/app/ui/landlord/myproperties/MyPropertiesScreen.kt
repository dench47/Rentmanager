package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rentmanager.app.R

// Цвета из Figma
private val White = Color.White
private val Black = Color.Black
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0x993C3C43)
private val TextMuted = Color(0x99151515) // rgba(21, 21, 21, 0.5)
private val CardBg = Color(0xFFEFEFEF)
private val ButtonBorder = Color(0xFFCFCFCF)
private val TabBg = Color(0xFFF3F3F3)
private val TabText = Color(0xFF404040)
private val TabTextActive = Color(0xFF151515)
private val DividerColor = Color(0x1A151515)

@Composable
fun MyPropertiesScreen(
    onPropertyClick: (String) -> Unit,
    onCreateProperty: () -> Unit,
    onBack: () -> Unit,
    onFinanceClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onHomeClick: () -> Unit,
    viewModel: MyPropertiesViewModel = viewModel()
) {
    val properties by viewModel.properties.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    Scaffold(
        containerColor = White,
        bottomBar = {
            BottomTabBar(
                onFinanceClick = onFinanceClick,
                onCreateProperty = onCreateProperty,
                onNotificationsClick = onNotificationsClick,
                onHomeClick = onHomeClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(White)
        ) {
            // Navigation Bar
            MyPropertiesNavBar(onBack = onBack)

            // Переключатель Месяцы / Сутки
            ViewModeToggle(
                currentMode = viewMode,
                onModeChange = { viewModel.setViewMode(it) }
            )

            // Список объектов
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(properties) { index, property ->
                    Column {
                        // Карточка объекта
                        PropertyCard(
                            property = property,
                            onClick = { onPropertyClick(property.id) },
                            onGrantAccess = { /* TODO: Дать доступ */ }
                        )

                        // Шахматка загруженности
                        ScheduleRow(
                            schedule = property.schedule,
                            viewMode = viewMode
                        )

                        // Разделитель между объектами
                        if (index < properties.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 1.dp,
                                color = DividerColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPropertiesNavBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Левая часть: стрелка + заголовок
        Row(
            modifier = Modifier.clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp),
                tint = TextPrimary
            )
            Text(
                "Моя недвижимость",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.3).sp
            )
        }

        // Правая часть: поиск + transfer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Поиск
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* TODO: поиск */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Поиск",
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }

            // Transfer
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* TODO: transfer */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Transfer",
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Фон — светло-серая капсула
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(51.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(CardBg)
        )

        // Кнопки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(51.dp)
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Сутки
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(47.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .then(
                        if (currentMode == ViewMode.DAYS)
                            Modifier.background(TextPrimary)
                        else
                            Modifier.background(Color.Transparent)
                    )
                    .clickable { onModeChange(ViewMode.DAYS) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Сутки",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.4).sp,
                    color = if (currentMode == ViewMode.DAYS) White else TextMuted
                )
            }

            // Месяцы
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(47.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .then(
                        if (currentMode == ViewMode.MONTHS)
                            Modifier.background(TextPrimary)
                        else
                            Modifier.background(Color.Transparent)
                    )
                    .clickable { onModeChange(ViewMode.MONTHS) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Месяцы",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.4).sp,
                    color = if (currentMode == ViewMode.MONTHS) White else TextMuted
                )
            }
        }
    }
}

@Composable
private fun PropertyCard(
    property: MyPropertyItem,
    onClick: () -> Unit,
    onGrantAccess: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Аватар 40×40 с borderRadius 8 (из Figma)
            Image(
                painter = painterResource(R.drawable.mock_avatar_legend),
                contentDescription = property.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            // Название + адрес
            Column {
                Text(
                    property.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Black,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    property.address,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary,
                    letterSpacing = (-0.4).sp
                )
            }
        }

        // Кнопка «Дать доступ»
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .border(1.dp, ButtonBorder, RoundedCornerShape(100.dp))
                .clickable { onGrantAccess() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Дать доступ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
                color = Color(0xCC000000)
            )
        }
    }
}

@Composable
private fun ScheduleRow(
    schedule: List<String>,
    viewMode: ViewMode
) {
    val monthsLabels = listOf("СЕН", "ОКТ", "НОЯ", "ДЕК", "ЯНВ", "ФЕВ", "МАР", "АПР")
    val daysLabels = (1..31).map { it.toString() }

    val labels = when (viewMode) {
        ViewMode.MONTHS -> monthsLabels.take(schedule.size)
        ViewMode.DAYS -> daysLabels.take(schedule.size)
    }

    val scrollState = androidx.compose.foundation.rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Строка месяцев/дней с фоном #EFEFEF
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .height(20.dp),
                horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                labels.forEach { label ->
                    Text(
                        label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.4).sp,
                        color = Color(0xE6151515),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Индикаторы — синхронный скролл
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            schedule.forEach { state ->
                val resId = when (state) {
                    "fullness" -> R.drawable.ic_indicator_fullness
                    "expired" -> R.drawable.ic_indicator_expired
                    else -> R.drawable.ic_indicator_free
                }
                Image(
                    painter = painterResource(resId),
                    contentDescription = state,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillBounds
                )
            }
        }
    }
}

@Composable
private fun BottomTabBar(
    onFinanceClick: () -> Unit,
    onCreateProperty: () -> Unit,
    onNotificationsClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TabBg.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Финансы
        TabBarItem(
            iconRes = R.drawable.ic_card_finance,
            label = "Финансы",
            onClick = onFinanceClick,
            textColor = TabText
        )

        // Создать объект
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onCreateProperty() }
                .padding(horizontal = 15.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus_circle),
                contentDescription = "Создать объект",
                modifier = Modifier.size(30.dp),
                tint = TabTextActive
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Создать объект",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.4).sp,
                color = TabTextActive
            )
        }

        // Уведомления
        TabBarItem(
            iconRes = R.drawable.ic_card_messages_no_badge,
            label = "Уведомления",
            onClick = onNotificationsClick,
            textColor = TabText
        )
    }
}

@Composable
private fun TabBarItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = textColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.4).sp,
            color = textColor
        )
    }
}