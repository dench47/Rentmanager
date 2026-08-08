package com.rentmanager.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onNavigateToPhoneVerify: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.uploadAndSetAvatar(uri)
    }

    // Logout dialog
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Выйти из учётной записи", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Выберите способ выхода:", fontSize = 16.sp, color = Color(0x993C3C43))
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.logoutCurrentDevice(onLoggedOut) }, modifier = Modifier.fillMaxWidth()) { Text("С этого устройства", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121)) }
                    TextButton(onClick = { viewModel.logoutAllDevices(onLoggedOut) }, modifier = Modifier.fillMaxWidth()) { Text("Со всех устройств", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935)) }
                }
            },
            confirmButton = {}, containerColor = Color.White, shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Удалить учётную запись", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE53935), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text("Все ваши данные будут безвозвратно удалены.", fontSize = 16.sp, color = Color(0x993C3C43), textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { viewModel.deleteAccount(onLoggedOut) }) { Text("Да, удалить", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { viewModel.dismissDialogs() }) { Text("Отмена", color = Color(0xFF007AFF)) } },
            containerColor = Color.White, shape = RoundedCornerShape(20.dp)
        )
    }

    // Start screen dialog
    if (uiState.showStartScreenDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Начальный экран", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Какой экран открывать при запуске?", fontSize = 14.sp, color = Color(0x993C3C43))
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.setDefaultStartScreen("verify") }, modifier = Modifier.fillMaxWidth()) { Text("Верификация", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = if (uiState.defaultStartScreen == "verify") Color(0xFF007AFF) else Color(0xFF212121)) }
                    TextButton(onClick = { viewModel.setDefaultStartScreen("main") }, modifier = Modifier.fillMaxWidth()) { Text("Главный экран", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = if (uiState.defaultStartScreen == "main") Color(0xFF007AFF) else Color(0xFF212121)) }
                }
            },
            confirmButton = {}, containerColor = Color.White, shape = RoundedCornerShape(20.dp)
        )
    }

    // Phone warning dialog
    if (uiState.showPhoneWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPhoneWarning() },
            title = { Text("Смена номера", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text("Для смены номера необходимо заново его верифицировать.", fontSize = 16.sp, color = Color(0x993C3C43), textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { viewModel.startPhoneVerification { onNavigateToPhoneVerify(uiState.newPhone) } }) { Text("Продолжить", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { viewModel.dismissPhoneWarning() }) { Text("Отмена", color = Color(0x993C3C43)) } },
            containerColor = Color.White, shape = RoundedCornerShape(20.dp)
        )
    }

    // Edit name dialog (три поля ФИО)
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editFirstName by remember { mutableStateOf("") }
    var editLastName by remember { mutableStateOf("") }
    var editMiddleName by remember { mutableStateOf("") }
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("ФИО", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121)) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = editFirstName, onValueChange = { editFirstName = capitalizeFirst(it) }, modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)), placeholder = { Text("Имя", color = Color(0x998E8E93), fontSize = 16.sp) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = editLastName, onValueChange = { editLastName = capitalizeFirst(it) }, modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)), placeholder = { Text("Фамилия", color = Color(0x998E8E93), fontSize = 16.sp) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = editMiddleName, onValueChange = { editMiddleName = capitalizeFirst(it) }, modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)), placeholder = { Text("Отчество", color = Color(0x998E8E93), fontSize = 16.sp) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
                }
            },
            confirmButton = { TextButton(onClick = { val fullName = listOf(editFirstName, editLastName, editMiddleName).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" "); if (fullName.isNotEmpty()) viewModel.updateProfile(name = fullName); showEditNameDialog = false }) { Text("Сохранить", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("Отмена", color = Color(0x993C3C43)) } },
            containerColor = Color.White, shape = RoundedCornerShape(20.dp)
        )
    }

    // Edit email dialog
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var editEmail by remember { mutableStateOf(uiState.email ?: "") }
    if (showEditEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEditEmailDialog = false },
            title = { Text("Почтовый ящик", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121)) },
            text = { OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)), singleLine = true, placeholder = { Text("Введите email", color = Color(0x998E8E93), fontSize = 16.sp) }, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)) },
            confirmButton = { TextButton(onClick = { viewModel.updateProfile(email = editEmail); showEditEmailDialog = false }) { Text("Сохранить", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showEditEmailDialog = false }) { Text("Отмена", color = Color(0x993C3C43)) } },
            containerColor = Color.White, shape = RoundedCornerShape(20.dp)
        )
    }

    // Edit legal name dialog
    var showEditLegalDialog by remember { mutableStateOf(false) }
    var editLegalName by remember { mutableStateOf(uiState.legalName ?: "") }
    if (showEditLegalDialog) {
        AlertDialog(
            onDismissRequest = { showEditLegalDialog = false },
            title = { Text("Юридическое лицо", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121)) },
            text = { OutlinedTextField(value = editLegalName, onValueChange = { editLegalName = it }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)), singleLine = true, placeholder = { Text("Название организации", color = Color(0x998E8E93), fontSize = 16.sp) }, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)) },
            confirmButton = { TextButton(onClick = { viewModel.updateProfile(legalName = editLegalName); showEditLegalDialog = false }) { Text("Сохранить", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showEditLegalDialog = false }) { Text("Отмена", color = Color(0x993C3C43)) } },
            containerColor = Color.White, shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).background(Color.White)) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 12.dp, end = 0.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.clickable { onBack() }.padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Image(painter = painterResource(R.drawable.ic_arrow_left), contentDescription = "Назад", modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
                    Text("Настройки", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121), letterSpacing = (-0.3).sp)
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(80.dp).clickable { imagePicker.launch("image/*") }) {
                            if (!uiState.avatarUrl.isNullOrBlank()) {
                                AsyncImage(model = uiState.avatarUrl, contentDescription = "Аватар", modifier = Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Image(painter = painterResource(R.drawable.ic_default_avatar), contentDescription = "Аватар", modifier = Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            }
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFF6F6F6)).align(Alignment.BottomEnd), Alignment.Center) { Icon(Icons.Default.Edit, "Изменить", Modifier.size(16.dp), tint = Color(0xFF212121)) }
                        }
                        Text(if (uiState.avatarUrl.isNullOrBlank()) "Установить аватар" else "Сменить аватар", fontSize = 14.sp, color = Color(0xFF7AB66A), modifier = Modifier.clickable { imagePicker.launch("image/*") })
                        if (!uiState.avatarUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Удалить аватар", fontSize = 14.sp, color = Color(0xFFE53935), modifier = Modifier.clickable { viewModel.updateProfile(avatarUrl = "") })
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
                }
                item { SettingsField("ФИО", uiState.userName.ifEmpty { "Иван Иванов" }, Icons.Default.Edit, onClick = { showEditNameDialog = true; editFirstName = uiState.userName; editLastName = ""; editMiddleName = "" }) }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().clickable { viewModel.onPhoneChangeRequest(uiState.phone) }.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.Phone, "Телефон", Modifier.size(24.dp), tint = Color(0xFF212121))
                            Column(Modifier.weight(1f)) { Text("Номер телефона", fontSize = 16.sp, color = Color(0xFF212121)); if (uiState.phone.isNotEmpty()) Text(uiState.phone, fontSize = 14.sp, color = Color(0x993C3C43)) }
                            Icon(Icons.Default.Edit, "Редактировать", Modifier.size(20.dp), tint = Color(0x993C3C43))
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
                    }
                }
                item { SettingsField("Почтовый ящик", uiState.email ?: "", Icons.Default.Email, isOptional = true, onClick = { showEditEmailDialog = true; editEmail = uiState.email ?: "" }) }
                item { SettingsField("Название юридического лица", uiState.legalName ?: "", Icons.Default.Business, isOptional = true, onClick = { showEditLegalDialog = true; editLegalName = uiState.legalName ?: "" }) }
                item { SettingsAction("Установить пароль", Icons.Default.Lock) }
                item { SettingsAction("Начальный экран", Icons.Default.Home, subtitle = if (uiState.defaultStartScreen == "verify") "Верификация" else "Главный экран", onClick = { viewModel.showStartScreenDialog() }) }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().clickable { viewModel.showLogoutDialog() }.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, "Выйти из учётной записи", Modifier.size(24.dp), tint = Color(0xFFE53935))
                            Column(Modifier.weight(1f)) { Text("Выйти из учётной записи", fontSize = 16.sp, color = Color(0xFFE53935)); Text("С этого устройства / Со всех устройств", fontSize = 14.sp, color = Color(0x993C3C43)) }
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
                    }
                }
                item { SettingsAction("Удалить учётную запись", Icons.Default.Delete, isDestructive = true, onClick = { viewModel.showDeleteDialog() }) }
                if (uiState.errorMessage != null) {
                    item { Text(uiState.errorMessage!!, color = Color(0xFFE53935), fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, top = 8.dp)) }
                }
            }
        }
    }
}

private fun capitalizeFirst(s: String): String = if (s.isEmpty()) s else s[0].uppercaseChar() + s.substring(1)

@Composable
private fun SettingsField(label: String, value: String, icon: ImageVector, isOptional: Boolean = false, onClick: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, label, Modifier.size(24.dp), tint = Color(0xFF212121))
            Column(Modifier.weight(1f)) { Text(label, fontSize = 16.sp, color = Color(0xFF212121)); if (isOptional) Text("(необязательно)", fontSize = 13.sp, color = Color(0x993C3C43)); if (value.isNotEmpty()) Text(value, fontSize = 14.sp, color = Color(0x993C3C43)) }
            if (label != "Номер телефона") Icon(Icons.Default.Edit, "Редактировать", Modifier.size(20.dp), tint = Color(0x993C3C43))
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
    }
}

@Composable
private fun SettingsAction(text: String, icon: ImageVector, subtitle: String = "", isDestructive: Boolean = false, onClick: () -> Unit = {}) {
    val color = if (isDestructive) Color(0xFFE53935) else Color(0xFF212121)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, text, Modifier.size(24.dp), tint = color)
            Column(Modifier.weight(1f)) { Text(text, fontSize = 16.sp, color = color); if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 14.sp, color = Color(0x993C3C43)) }
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
    }
}