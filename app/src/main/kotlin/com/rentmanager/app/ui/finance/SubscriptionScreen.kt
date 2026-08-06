package com.rentmanager.app.ui.finance

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R

private val GradientBackground = Brush.verticalGradient(
    colors = listOf(Color.White, Color(0xFFF5F7FA))
)

private val CreamColor = Color(0xFFFAF8F5)

data class SubscriptionProperty(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val rate: String,   // "300 ₽/мес" or "10 ₽/день"
    val dueDate: String // "15.08.2026"
)

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    properties: List<SubscriptionProperty> = emptyList(),
    onAddProperty: () -> Unit = {},
    onRenew: (String) -> Unit = {}
) {
    val activeCount = properties.count { it.isActive }

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GradientBackground)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onBack() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_arrow_left),
                            contentDescription = "Назад",
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "Управление подпиской",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1D1F),
                            letterSpacing = (-0.3).sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CreamColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Активные объекты",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93)
                                )
                                Text(
                                    "$activeCount из ${properties.size}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1D1F)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                                Text(
                                    "Тариф",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93)
                                )
                                Text(
                                    "300 ₽/мес / объект",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1D1D1F)
                                )
                                Text(
                                    "10 ₽/день / объект",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Property list
                    if (properties.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CreamColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Нет объектов",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1D1D1F)
                                )
                                Text(
                                    "Добавьте объект для активации подписки",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93)
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .width(200.dp)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFF212121))
                                        .clickable { onAddProperty() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Добавить объект", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    } else {
                        properties.forEach { prop ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CreamColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(
                                                        if (prop.isActive) Color(0xFF4CAF50)
                                                        else Color(0xFFBDBDBD)
                                                    )
                                            )
                                            Text(
                                                prop.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF1D1D1F)
                                            )
                                        }
                                        Text(
                                            prop.rate,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1D1D1F)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (prop.isActive) {
                                            Text(
                                                "До ${prop.dueDate}",
                                                fontSize = 13.sp,
                                                color = Color(0xFF8E8E93)
                                            )
                                        } else {
                                            Text(
                                                "Не оплачен",
                                                fontSize = 13.sp,
                                                color = Color(0xFFE53935)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(
                                                    if (prop.isActive) Color(0xFF212121)
                                                    else Color(0xFF4CAF50)
                                                )
                                                .clickable { onRenew(prop.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                if (prop.isActive) "Продлить" else "Активировать",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}