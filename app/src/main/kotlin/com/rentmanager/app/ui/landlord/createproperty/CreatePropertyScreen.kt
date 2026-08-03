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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.RentManagerTheme

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

    var tenantInfoExpanded by remember { mutableStateOf(false) }
    var serviceInfoExpanded by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 8.dp, end = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onBack() }.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = "Назад",
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        if (isEdit) "Редактирование" else "Создание объекта",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        letterSpacing = (-0.3).sp
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Фото
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)).clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(20.dp), tint = Color(0xFF757575))
                        Text("Добавить фото", fontSize = 15.sp, color = Color(0xFF757575), letterSpacing = (-0.4).sp)
                    }
                }

                // 2. Название
                UnderlineTextField(value = name, onValueChange = { name = it }, placeholder = "Название")

                // 3. Адрес
                UnderlineTextField(value = address, onValueChange = { address = it }, placeholder = "Адрес")

                // 4. Площадь
                UnderlineTextField(value = area, onValueChange = { area = it }, placeholder = "Площадь (м²)")

                // 5. Информация об объекте (accordion)
                AccordionCard(
                    title = "Информация об объекте",
                    subtitle = "Укажите информацию для арендатора: правила пользования, wi-fi и др.",
                    expanded = tenantInfoExpanded,
                    onToggle = { tenantInfoExpanded = !tenantInfoExpanded }
                ) {
                    UnderlineTextField(value = tenantInfo, onValueChange = { tenantInfo = it }, placeholder = "")
                }

                // 6. Служебная информация (accordion)
                AccordionCard(
                    title = "Служебная информация",
                    subtitle = "Эта информация будет видна только вам",
                    expanded = serviceInfoExpanded,
                    onToggle = { serviceInfoExpanded = !serviceInfoExpanded }
                ) {
                    UnderlineTextField(value = serviceInfo, onValueChange = { serviceInfo = it }, placeholder = "")
                }

                // Push buttons to bottom
                Spacer(Modifier.weight(1f))

                // 7. График платежей
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(100.dp)).background(Color(0xFF212121)).clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Row(Modifier.padding(horizontal = 32.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_calendar_edit),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(Color(0xFFA6A6A6))
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("График платежей и реквизиты", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White, letterSpacing = (-0.4).sp)
                    }
                }

                // Сохранить
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(100.dp))
                        .background(if (name.isNotBlank() && address.isNotBlank()) Color(0xFF212121) else Color(0xFF212121).copy(alpha = 0.5f))
                        .clickable(enabled = name.isNotBlank() && address.isNotBlank()) { onCreated() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isEdit) "Сохранить" else "Создать", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UnderlineTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Column(Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF212121), letterSpacing = (-0.4).sp),
            cursorBrush = SolidColor(Color(0xFF212121)),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box { if (value.isEmpty()) Text(placeholder, fontSize = 16.sp, color = Color(0xFFBDBDBD), letterSpacing = (-0.4).sp); innerTextField() }
            }
        )
        HorizontalDivider(thickness = 1.dp, color = Color.Black.copy(alpha = 0.08f))
    }
}

@Composable
private fun AccordionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF3F3F3))
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xE5151515), letterSpacing = (-0.4).sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = Color(0x993C3C43), letterSpacing = (-0.4).sp)
            }
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    Modifier.size(24.dp),
                    tint = Color(0xFF151515)
                )
            }
        }
        AnimatedVisibility(visible = expanded) { content() }
    }
}

@Preview(showBackground = true, name = "Создание недвижимости")
@Composable
private fun PreviewCreateProperty() {
    RentManagerTheme { CreatePropertyScreen(onBack = {}, onCreated = {}) }
}