package com.rentmanager.app.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.RentManagerTheme

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Выйти из учётной записи",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Выберите способ выхода:",
                        fontSize = 16.sp,
                        color = Color(0x993C3C43),
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showLogoutDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "С этого устройства",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF212121)
                        )
                    }
                    TextButton(
                        onClick = { showLogoutDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Со всех устройств",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).background(Color.White)) {
            // Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 12.dp, end = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onBack() }.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(painter = painterResource(R.drawable.ic_arrow_left), contentDescription = "Назад", modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
                    Text("Настройки", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121), letterSpacing = (-0.3).sp)
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                // Avatar section
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(80.dp)) {
                            Image(painter = painterResource(R.drawable.ic_default_avatar), contentDescription = "Аватар", modifier = Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFF6F6F6)).align(Alignment.BottomEnd), Alignment.Center) {
                                Icon(Icons.Default.Edit, "Изменить", Modifier.size(16.dp), tint = Color(0xFF212121))
                            }
                        }
                        Text("Установить аватар", fontSize = 14.sp, color = Color(0xFF7AB66A), letterSpacing = (-0.4).sp)
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
                }

                // Profile fields
                item { SettingsField("ФИО", "Иван Иванов", Icons.Default.Edit) }
                item { SettingsField("Номер телефона", "+7 (900) 000-00-00", Icons.Default.Phone) }
                item { SettingsField("Почтовый ящик", "example@mail.ru", Icons.Default.Email, isOptional = true) }
                item { SettingsField("Название юридического лица", "", Icons.Default.Business, isOptional = true) }

                // Actions
                item { SettingsAction("Установить пароль", Icons.Default.Lock) }
                item { SettingsAction("Начальный экран", Icons.Default.Home, "Арендую / Сдаю") }

                // Destructive actions
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clickable { showLogoutDialog = true }.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, "Выйти из учётной записи", Modifier.size(24.dp), tint = Color(0xFFE53935))
                            Column(Modifier.weight(1f)) {
                                Text("Выйти из учётной записи", fontSize = 16.sp, color = Color(0xFFE53935), letterSpacing = (-0.4).sp)
                                Text("С этого устройства / Со всех устройств", fontSize = 14.sp, color = Color(0x993C3C43), letterSpacing = (-0.4).sp)
                            }
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
                    }
                }
                item { SettingsAction("Удалить учётную запись", Icons.Default.Delete, isDestructive = true) }
            }
        }
    }
}

@Composable
private fun SettingsField(label: String, value: String, icon: ImageVector, isOptional: Boolean = false) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { }.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, label, Modifier.size(24.dp), tint = Color(0xFF212121))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 16.sp, color = Color(0xFF212121), letterSpacing = (-0.4).sp)
                if (isOptional) Text("(необязательно)", fontSize = 13.sp, color = Color(0x993C3C43), letterSpacing = (-0.4).sp)
                if (value.isNotEmpty()) Text(value, fontSize = 14.sp, color = Color(0x993C3C43), letterSpacing = (-0.4).sp)
            }
            Icon(Icons.Default.Edit, "Редактировать", Modifier.size(20.dp), tint = Color(0x993C3C43))
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
    }
}

@Composable
private fun SettingsAction(text: String, icon: ImageVector, subtitle: String = "", isDestructive: Boolean = false) {
    val color = if (isDestructive) Color(0xFFE53935) else Color(0xFF212121)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { }.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, text, Modifier.size(24.dp), tint = color)
            Column(Modifier.weight(1f)) {
                Text(text, fontSize = 16.sp, color = color, letterSpacing = (-0.4).sp)
                if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 14.sp, color = Color(0x993C3C43), letterSpacing = (-0.4).sp)
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
    }
}

@Preview(showBackground = true, name = "Настройки")
@Composable
private fun PreviewSettings() {
    RentManagerTheme { SettingsScreen(onBack = {}) }
}