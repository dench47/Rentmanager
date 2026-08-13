package com.rentmanager.app.ui.property.detail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R

// Цвета из макета (node 73:915), стиль «Моя недвижимость»
private val TextPrimary = Color(0xFF212121)
private val SubtitleGray = Color(0xFF3C3C43)
private val CardGray = Color(0xFFF3F3F3)
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
    viewModel: PropertyDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(propertyId) { viewModel.load(propertyId) }

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
                onBack = onBack
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { PhotoSection(photoUrl = uiState.photoUrl, area = uiState.area) }

                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DarkPillButton(
                            text = "Добавить арендатора",
                            iconRes = R.drawable.ic_plus_circle,
                            onClick = onAttachTenant
                        )
                        InfoCard(text = "Информация об объекте")
                        InfoCard(text = "Служебная информация")
                        DarkPillButton(
                            text = "График платежей и реквизиты",
                            iconRes = R.drawable.ic_calendar_edit,
                            onClick = { }
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
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 5.dp),
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = SubtitleGray,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable { /* TODO: редактирование */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Редактировать",
                modifier = Modifier.size(24.dp),
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun PhotoSection(photoUrl: String?, area: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFFBDBDBD))
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "📷",
                fontSize = 48.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Стрелка влево
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
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
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }

        // Точки-индикаторы карусели
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            )
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                )
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
                Text(
                    "Площадь",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    area,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF656565)
                )
            }
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
private fun InfoCard(text: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardGray)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF151515),
            letterSpacing = (-0.4).sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF151515)
        )
    }
}


