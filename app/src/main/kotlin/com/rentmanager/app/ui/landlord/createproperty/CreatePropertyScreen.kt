package com.rentmanager.app.ui.landlord.createproperty

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.RentManagerTheme

private val GradientBackground = Brush.verticalGradient(
    colors = listOf(Color.White, Color(0xFFF5F7FA))
)

@Composable
fun CreatePropertyScreen(
    propertyId: String? = null,
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    val isEdit = propertyId != null
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var tenantInfo by remember { mutableStateOf("") }
    var serviceInfo by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var rulesText by remember { mutableStateOf("") }

    var tenantInfoExpanded by remember { mutableStateOf(false) }
    var serviceInfoExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GradientBackground)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            if (isEdit) "Редактирование" else "Новый объект",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1D1F),
                            letterSpacing = (-0.3).sp
                        )
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Фото — card with dashed border illusion
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clickable { }.padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    null,
                                    Modifier.size(24.dp),
                                    tint = Color(0xFF007AFF)
                                )
                                Text(
                                    "Добавить фото",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF007AFF),
                                    letterSpacing = (-0.4).sp
                                )
                            }
                        }
                    }

                    // 2. Название
                    PremiumTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Название"
                    )

                    // 3. Адрес
                    PremiumTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = "Адрес"
                    )

                    // 4. Площадь
                    PremiumTextField(
                        value = area,
                        onValueChange = { area = it },
                        placeholder = "Площадь (м²)"
                    )

                    // 5. Информация об объекте (accordion)
                    PremiumAccordionCard(
                        title = "Информация об объекте",
                        subtitle = "Эта информация будет видна арендатору",
                        expanded = tenantInfoExpanded,
                        onToggle = { tenantInfoExpanded = !tenantInfoExpanded }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    null,
                                    Modifier.size(20.dp),
                                    tint = Color(0xFF007AFF)
                                )
                                Text(
                                    "Номер телефона",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1D1D1F),
                                    letterSpacing = (-0.4).sp
                                )
                            }
                            PremiumTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                placeholder = "+7 (899) 99-99-99"
                            )

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Wifi,
                                    null,
                                    Modifier.size(20.dp),
                                    tint = Color(0xFF007AFF)
                                )
                                Text(
                                    "Пароль WiFi",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1D1D1F),
                                    letterSpacing = (-0.4).sp
                                )
                            }
                            PremiumTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                placeholder = "Rsjuff6749"
                            )

                            Text(
                                "Правила объекта",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1D1D1F),
                                letterSpacing = (-0.4).sp
                            )
                            PremiumTextField(
                                value = rulesText,
                                onValueChange = { rulesText = it },
                                placeholder = "Использовать помещение исключительно в целях, указанных в договоре"
                            )
                        }
                    }

                    // 6. Служебная информация (accordion)
                    PremiumAccordionCard(
                        title = "Служебная информация",
                        subtitle = "Эта информация будет видна только вам",
                        expanded = serviceInfoExpanded,
                        onToggle = { serviceInfoExpanded = !serviceInfoExpanded }
                    ) {
                        PremiumTextField(
                            value = serviceInfo,
                            onValueChange = { serviceInfo = it },
                            placeholder = ""
                        )
                    }
                }

                // Bottom buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Добавить счетчики — outline button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                Modifier.size(20.dp),
                                tint = Color(0xFF007AFF)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Добавить счетчики",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF1D1D1F),
                                letterSpacing = (-0.4).sp
                            )
                        }
                    }

                    // График платежей — outline button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                Modifier.size(20.dp),
                                tint = Color(0xFF007AFF)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "График платежей и реквизиты",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF1D1D1F),
                                letterSpacing = (-0.4).sp
                            )
                        }
                    }

                    // Сохранить — solid blue button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (name.isNotBlank() && address.isNotBlank())
                                Color(0xFF007AFF)
                            else
                                Color(0xFF007AFF).copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = name.isNotBlank() && address.isNotBlank()) {
                                    onCreated()
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isEdit) "Сохранить" else "Создать",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                letterSpacing = (-0.4).sp
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            placeholder = {
                Text(
                    placeholder,
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93),
                    letterSpacing = (-0.4).sp
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                color = Color(0xFF1D1D1F),
                letterSpacing = (-0.4).sp
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun PremiumAccordionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1D1D1F),
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93),
                        letterSpacing = (-0.4).sp
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    Modifier.size(20.dp),
                    tint = Color(0xFF007AFF)
                )
            }
            AnimatedVisibility(visible = expanded) {
                content()
            }
        }
    }
}

@Preview(showBackground = true, name = "Создание недвижимости")
@Composable
private fun PreviewCreateProperty() {
    RentManagerTheme { CreatePropertyScreen(onBack = {}, onCreated = {}) }
}