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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onSecurityClick: () -> Unit,
    onNavigateToPhoneVerify: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Обновляем профиль при возврате на экран (например, почту подтвердили в «Безопасности»)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadProfile()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.uploadAndSetAvatar(uri)
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editFirstName by remember { mutableStateOf("") }
    var editLastName by remember { mutableStateOf("") }
    var editMiddleName by remember { mutableStateOf("") }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var editEmail by remember { mutableStateOf(uiState.email ?: "") }
    var showEmailVerifyDialog by remember { mutableStateOf(false) }
    var emailVerifyCode by remember { mutableStateOf("") }
    var emailVerifyError by remember { mutableStateOf<String?>(null) }
    var emailVerifyLoading by remember { mutableStateOf(false) }
    var showEditLegalDialog by remember { mutableStateOf(false) }
    var editLegalName by remember { mutableStateOf(uiState.legalName ?: "") }

    var emailVerifyStep by remember { mutableIntStateOf(0) } // 0 = инфо, 1 = код
    Scaffold(containerColor = Color.White) { paddingValues ->
        // Канон шапки (AppScreenHeader): статус-инсет → 27 → строка(20/13)
        Column(Modifier.fillMaxSize().padding(paddingValues).background(Color.White)) {
            Spacer(Modifier.height(27.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.clickable { onBack() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painter = painterResource(R.drawable.ic_landlord_back), contentDescription = "Назад", modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
                    Text("Настройки", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = com.rentmanager.app.ui.theme.InterFontFamily, color = Color(0xFF212121), letterSpacing = (-0.3).sp)
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Аватар
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clickable { imagePicker.launch("image/*") }
                        ) {
                            if (!uiState.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = uiState.avatarUrl,
                                    contentDescription = "Аватар",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.ic_default_avatar),
                                    contentDescription = "Аватар",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF6F6F6))
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    "Изменить",
                                    Modifier.size(16.dp),
                                    tint = Color(0xFF212121)
                                )
                            }
                        }

                        // Отступ после аватарки (всегда одинаковый)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Текст действия
                        when {
                            uiState.avatarUrl.isNullOrBlank() -> {
                                Text(
                                    text = "Установить аватар",
                                    fontSize = 14.sp,
                                    color = Color(0xFF007AFF),
                                    modifier = Modifier.clickable { imagePicker.launch("image/*") }
                                )
                            }
                            else -> {
                                Text(
                                    text = "Удалить аватар",
                                    fontSize = 14.sp,
                                    color = Color(0xFFE53935),
                                    modifier = Modifier.clickable { viewModel.updateProfile(avatarUrl = "") }
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 1.dp,
                        color = Color.Black.copy(alpha = 0.1f)
                    )
                }
                item {
                    val displayFio = uiState.fullName.ifBlank { uiState.userName.ifEmpty { "" } }
                    val initialFirstName = uiState.userName
                    val full = uiState.fullName
                    val (initLastName, initMiddleName) = if (full.isNotBlank() && initialFirstName.isNotEmpty() && full != initialFirstName) {
                        val clean = full.trim()
                        if (clean.startsWith(initialFirstName)) {
                            val rest = clean.removePrefix(initialFirstName).trim().split(" ").filter { it.isNotEmpty() }
                            when (rest.size) {
                                2 -> rest[0] to rest[1]
                                1 -> rest[0] to ""
                                else -> "" to ""
                            }
                        } else "" to ""
                    } else "" to ""
                    SettingsField("ФИО", displayFio.ifEmpty { "Иван Иванов" }, Icons.Default.Edit, onClick = {
                        showEditNameDialog = true
                        editFirstName = initialFirstName
                        editLastName = initLastName
                        editMiddleName = initMiddleName
                    })
                }
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
                item { SettingsField("Почтовый ящик", uiState.email ?: "", Icons.Default.Email, warning = !uiState.email.isNullOrBlank() && !uiState.emailVerified, onWarningClick = { emailVerifyStep = 0; emailVerifyCode = ""; emailVerifyError = null; showEmailVerifyDialog = true }, onClick = { showEditEmailDialog = true; editEmail = uiState.email ?: "" }) }
                item { SettingsField("Название юридического лица", uiState.legalName ?: "", Icons.Default.Business, isOptional = true, onClick = { showEditLegalDialog = true; editLegalName = uiState.legalName ?: "" }) }
                item {
                    SettingsAction(
                        "Безопасность",
                        Icons.Default.Lock,
                        subtitle = if (uiState.localPinEnabled) "PIN-код, вход по отпечатку" else "PIN отключён на этом устройстве",
                        onClick = onSecurityClick
                    )
                }
                item { SettingsAction("Начальный экран", Icons.Default.Home, subtitle = when (uiState.defaultStartScreen) { "landlord" -> "Арендодатель"; "tenant" -> "Арендатор"; else -> "Главный экран" }, onClick = { viewModel.showStartScreenDialog() }) }
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
    // ---- Диалоги ПОСЛЕ Scaffold: CanonicalDialog — оверлей в окне экрана;
    // скомпозированный ДО белого экрана он рисовался ПОД ним и открывался
    // невидимо («поля не реагируют») ----
    // Logout dialog
    if (uiState.showLogoutDialog) {
        com.rentmanager.app.ui.components.CanonicalDialog(
            onDismiss = { viewModel.dismissDialogs() },
            title = "Выйти из учётной записи",
            text = "Выберите способ выхода"
        ) {
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "С этого устройства",
                container = Color(0xFF212121),
                textColor = Color.White,
                onClick = { viewModel.logoutCurrentDevice(onLoggedOut) }
            )
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Со всех устройств",
                container = Color(0xFFFF4249),
                textColor = Color.White,
                onClick = { viewModel.logoutAllDevices(onLoggedOut) }
            )
        }
    }

    // Delete dialog
    if (uiState.showDeleteDialog) {
        com.rentmanager.app.ui.components.CanonicalDialog(
            onDismiss = { viewModel.dismissDialogs() },
            title = "Удалить учётную запись",
            text = "Все ваши данные будут безвозвратно удалены"
        ) {
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Да, удалить",
                container = Color(0xFFFF4249),
                textColor = Color.White,
                onClick = { viewModel.deleteAccount(onLoggedOut) }
            )
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Отменить",
                stroke = Color(0xFF212121),
                textColor = Color(0xFF212121),
                onClick = { viewModel.dismissDialogs() }
            )
        }
    }

    // Start screen dialog
    if (uiState.showStartScreenDialog) {
        com.rentmanager.app.ui.components.CanonicalDialog(
            onDismiss = { viewModel.dismissDialogs() },
            title = "Начальный экран",
            text = "Какой экран открывать при запуске?"
        ) {
            val current = uiState.defaultStartScreen
            listOf(
                "Арендодатель" to "landlord",
                "Арендатор" to "tenant",
                "Главный экран" to ""
            ).forEach { (label, value) ->
                val selected = current == value || (value.isEmpty() && (current.isEmpty() || current == "main"))
                if (selected) {
                    com.rentmanager.app.ui.components.CanonicalDialogButton(
                        text = label,
                        container = Color(0xFF212121),
                        textColor = Color.White,
                        onClick = { viewModel.setDefaultStartScreen(value) }
                    )
                } else {
                    com.rentmanager.app.ui.components.CanonicalDialogButton(
                        text = label,
                        stroke = Color(0xFF212121),
                        textColor = Color(0xFF212121),
                        onClick = { viewModel.setDefaultStartScreen(value) }
                    )
                }
            }
        }
    }

    // Phone warning dialog
    if (uiState.showPhoneWarning) {
        com.rentmanager.app.ui.components.CanonicalDialog(
            onDismiss = { viewModel.dismissPhoneWarning() },
            title = "Смена номера",
            text = "Для смены номера необходимо заново его верифицировать"
        ) {
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Продолжить",
                container = Color(0xFF212121),
                textColor = Color.White,
                onClick = { viewModel.startPhoneVerification { onNavigateToPhoneVerify(uiState.newPhone) } }
            )
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Отменить",
                stroke = Color(0xFF212121),
                textColor = Color(0xFF212121),
                onClick = { viewModel.dismissPhoneWarning() }
            )
        }
    }

    // Edit name dialog (три поля ФИО)
    if (showEditNameDialog) {
        com.rentmanager.app.ui.components.CanonicalContentDialog(
            onDismiss = { showEditNameDialog = false },
            title = "Как вас зовут?"
        ) {
            OutlinedTextField(value = editFirstName, onValueChange = { editFirstName = capitalizeEach(it) }, modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)), placeholder = { Text("Имя", color = Color(0x998E8E93), fontSize = 16.sp) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = editLastName, onValueChange = { editLastName = capitalizeEach(it) }, modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)), placeholder = { Text("Фамилия", color = Color(0x998E8E93), fontSize = 16.sp) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = editMiddleName, onValueChange = { editMiddleName = capitalizeEach(it) }, modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)), placeholder = { Text("Отчество", color = Color(0x998E8E93), fontSize = 16.sp) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
            Spacer(Modifier.height(20.dp))
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Сохранить",
                container = Color(0xFF212121),
                textColor = Color.White,
                onClick = {
                    val f = editFirstName.trim()
                    val l = editLastName.trim()
                    val m = editMiddleName.trim()
                    val displayFull = if (l.isNotEmpty()) listOf(f, l, m).filter { it.isNotEmpty() }.joinToString(" ") else f
                    viewModel.updateProfile(name = f, fullName = displayFull)
                    showEditNameDialog = false
                }
            )
        }
    }

    // Edit email dialog
    if (showEditEmailDialog) {
        com.rentmanager.app.ui.components.CanonicalContentDialog(
            onDismiss = { showEditEmailDialog = false },
            title = "Почтовый ящик"
        ) {
            OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)), singleLine = true, placeholder = { Text("Введите email", color = Color(0x998E8E93), fontSize = 16.sp) }, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
            Spacer(Modifier.height(20.dp))
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Сохранить",
                container = Color(0xFF212121),
                textColor = Color.White,
                onClick = { viewModel.updateProfile(email = editEmail); showEditEmailDialog = false }
            )
            Spacer(Modifier.height(6.dp))
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Очистить",
                stroke = Color(0xFF212121),
                textColor = Color(0xFF212121),
                onClick = { editEmail = ""; viewModel.updateProfile(email = ""); showEditEmailDialog = false }
            )
        }
    }

    // Email verification dialog
    if (showEmailVerifyDialog) {
        com.rentmanager.app.ui.components.CanonicalContentDialog(
            onDismiss = { if (!emailVerifyLoading) showEmailVerifyDialog = false },
            title = if (emailVerifyStep == 0) "Email не подтверждён" else "Код подтверждения"
        ) {
            if (emailVerifyStep == 0) {
                Text("Подтвердите почту, чтобы активировать вход через Email. Мы отправим код на ${uiState.email}.", fontSize = 15.sp, color = Color(0x993C3C43))
            } else {
                OutlinedTextField(value = emailVerifyCode, onValueChange = { raw -> emailVerifyCode = raw.filter { it.isDigit() }.take(6) }, label = { Text("Код из письма") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Если письмо не пришло — проверьте папку «Спам»", fontSize = 12.sp, color = Color(0x993C3C43))
            }
            if (emailVerifyError != null) {
                Spacer(Modifier.height(8.dp))
                Text(emailVerifyError!!, color = Color(0xFFE53935), fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = if (emailVerifyStep == 0) "Отправить код" else "Подтвердить",
                container = Color(0xFF212121),
                textColor = Color.White,
                onClick = {
                    if (emailVerifyStep == 0) {
                        emailVerifyLoading = true
                        emailVerifyError = null
                        viewModel.onEmailSendCode { ok, err ->
                            emailVerifyLoading = false
                            if (ok) { emailVerifyStep = 1; emailVerifyCode = "" } else emailVerifyError = err
                        }
                    } else {
                        if (emailVerifyCode.length != 6) emailVerifyError = "Введите 6 цифр кода"
                        else {
                            emailVerifyLoading = true
                            emailVerifyError = null
                            viewModel.onEmailVerify(emailVerifyCode) { ok, err ->
                                emailVerifyLoading = false
                                if (ok) showEmailVerifyDialog = false else emailVerifyError = err
                            }
                        }
                    }
                }
            )
            Spacer(Modifier.height(6.dp))
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = if (emailVerifyStep == 0) "Отмена" else "Назад",
                stroke = Color(0xFF212121),
                textColor = Color(0xFF212121),
                onClick = { if (emailVerifyStep == 1) { emailVerifyStep = 0; emailVerifyError = null } else showEmailVerifyDialog = false }
            )
        }
    }

    // Edit legal name dialog
    if (showEditLegalDialog) {
        com.rentmanager.app.ui.components.CanonicalContentDialog(
            onDismiss = { showEditLegalDialog = false },
            title = "Юридическое лицо"
        ) {
            OutlinedTextField(value = editLegalName, onValueChange = { editLegalName = it }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)), singleLine = true, placeholder = { Text("Название организации", color = Color(0x998E8E93), fontSize = 16.sp) }, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F7), unfocusedContainerColor = Color(0xFFF2F2F7), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
            Spacer(Modifier.height(20.dp))
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Сохранить",
                container = Color(0xFF212121),
                textColor = Color.White,
                onClick = { viewModel.updateProfile(legalName = editLegalName); showEditLegalDialog = false }
            )
            Spacer(Modifier.height(6.dp))
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Очистить",
                stroke = Color(0xFF212121),
                textColor = Color(0xFF212121),
                onClick = { editLegalName = ""; viewModel.updateProfile(legalName = ""); showEditLegalDialog = false }
            )
        }
    }
}

private fun capitalizeEach(s: String): String = s.split(" ").joinToString(" ") { w -> if (w.isEmpty()) w else w[0].uppercaseChar() + w.substring(1) }

@Composable
private fun SettingsField(label: String, value: String, icon: ImageVector, isOptional: Boolean = false, warning: Boolean = false, onWarningClick: () -> Unit = {}, onClick: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, label, Modifier.size(24.dp), tint = Color(0xFF212121))
            Column(Modifier.weight(1f)) { Text(label, fontSize = 16.sp, color = Color(0xFF212121)); if (isOptional) Text("(необязательно)", fontSize = 13.sp, color = Color(0x993C3C43)); if (value.isNotEmpty()) Text(value, fontSize = 14.sp, color = Color(0x993C3C43)) }
            if (warning) Icon(Icons.Outlined.ErrorOutline, "Требует подтверждения", Modifier.size(20.dp).clickable { onWarningClick() }, tint = Color(0x993C3C43))
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
