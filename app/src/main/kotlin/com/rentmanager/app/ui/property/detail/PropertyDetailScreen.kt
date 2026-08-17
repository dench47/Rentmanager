package com.rentmanager.app.ui.property.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.rentmanager.app.R
import kotlinx.coroutines.launch

// Цвета из макета (node 73:915 / 73:1005), стиль «Моя недвижимость»
private val TextPrimary = Color(0xFF212121)
private val SubtitleGray = Color(0xFF3C3C43)
private val LightYellowSection = Color(0xFFFFFFDA)
private val PremiumYellow = Color(0xFFFEFFBB)
private val OutlineGray = Color(0xFF8A8A8E)

@Composable
fun PropertyDetailScreen(
    propertyId: String,
    onBack: () -> Unit,
    onAddMeter: () -> Unit,
    onCall: () -> Unit,
    onWrite: () -> Unit,
    onAttachTenant: () -> Unit,
    onEdit: () -> Unit,
    onPaymentSchedule: (String) -> Unit,
    viewModel: PropertyDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, propertyId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load(propertyId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            DetailHeader(
                propertyName = uiState.propertyName,
                address = uiState.address,
                rentPrice = uiState.rentPrice,
                onBack = onBack
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    if (uiState.photos.isNotEmpty()) {
                        PhotoCarousel(
                            photos = uiState.photos,
                            area = uiState.area,
                            onEdit = onEdit
                        )
                    } else {
                        PhotoPlaceholder(
                            area = uiState.area,
                            onEdit = onEdit
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (uiState.tenantName.isNotBlank()) {
                            TenantInfoCard(
                                name = uiState.tenantName,
                                phone = uiState.tenantPhone,
                                company = uiState.tenantCompany
                            )
                        }

                        DarkPillButton(
                            text = "Добавить арендатора",
                            iconRes = R.drawable.ic_plus_circle,
                            onClick = onAttachTenant
                        )

                        DetailAccordion(
                            title = "Информация об объекте",
                            subtitle = "Эта информация будет видна арендатору"
                        ) {
                            ReadOnlyField("Номер телефона", uiState.phone)
                            ReadOnlyField("Пароль WiFi", uiState.wifiPassword)
                            ReadOnlyField("Правила объекта", uiState.houseRules)
                        }

                        DetailAccordion(
                            title = "Служебная информация",
                            subtitle = "Эта информация будет видна только вам"
                        ) {
                            ReadOnlyField("Служебная информация", uiState.serviceInfo)
                        }

                        DarkPillButton(
                            text = "График платежей и реквизиты",
                            iconRes = R.drawable.ic_calendar_edit,
                            onClick = { onPaymentSchedule(propertyId) }
                        )

                        OutlinedPillButton(text = "Расходы", onClick = { })
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightYellowSection)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        DarkPillButton(
                            text = "Добавить счетчики",
                            iconRes = R.drawable.ic_plus_circle,
                            onClick = onAddMeter
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun DetailHeader(
    propertyName: String,
    address: String,
    rentPrice: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 5.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    propertyName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    address,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = SubtitleGray,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "Арендная плата",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                letterSpacing = (-0.4).sp
            )
            Text(
                rentPrice,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = SubtitleGray,
                letterSpacing = (-0.4).sp
            )
        }
    }
}
@Composable
private fun PhotoCarousel(
    photos: List<String>,
    area: String,
    onEdit: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = photos[page],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Стрелка влево
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable {
                    if (pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }

        // Стрелка вправо
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable {
                    if (pagerState.currentPage < photos.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }

        // Индикатор страниц (Frame 86)
        if (photos.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                photos.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == pagerState.currentPage) 20.dp else 8.dp,
                                height = 8.dp
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (index == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.6f)
                            )
                    )
                }
            }
        }

        // Бейдж «Площадь»
        if (area.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(PremiumYellow)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Площадь", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Text(area, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF656565))
            }
        }

        // Карандаш (редактирование) — правый верхний угол фото
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .clickable { onEdit() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Изменить",
                modifier = Modifier.size(20.dp),
                tint = TextPrimary
            )
        }
    }
}
@Composable
private fun PhotoPlaceholder(area: String, onEdit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFFF0F0F0))
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp),
            tint = Color(0xFF8E8E93)
        )

        // Бейдж «Площадь»
        if (area.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(PremiumYellow)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Площадь", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Text(area, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF656565))
            }
        }

        // Карандаш (редактирование)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .clickable { onEdit() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Изменить",
                modifier = Modifier.size(20.dp),
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun DarkPillButton(
    text: String,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(TextPrimary)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
private fun OutlinedPillButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(100.dp))
            .border(1.dp, OutlineGray, RoundedCornerShape(100.dp))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = OutlineGray
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
private fun TenantInfoCard(name: String, phone: String, company: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color(0x99EDEDED))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            name,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            letterSpacing = (-0.3).sp
        )
        if (phone.isNotBlank()) {
            Text(phone, fontSize = 14.sp, color = SubtitleGray, letterSpacing = (-0.4).sp)
        }
        if (company.isNotBlank()) {
            Text(company, fontSize = 14.sp, color = SubtitleGray, letterSpacing = (-0.4).sp)
        }
    }
}

@Composable
private fun DetailAccordion(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1D1D1F),
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        letterSpacing = (-0.4).sp
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    null,
                    Modifier.size(18.dp),
                    tint = Color(0xFF1D1D1F)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column { content() }
            }
        }
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 6.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color(0xFF8E8E93),
            letterSpacing = (-0.4).sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value.ifBlank { "—" },
            fontSize = 15.sp,
            color = Color(0xFF1D1D1F),
            letterSpacing = (-0.4).sp
        )
    }
}




