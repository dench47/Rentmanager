package com.rentmanager.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.data.api.TrustedDeviceDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onBack: () -> Unit,
    onChangePin: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDevicesSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 60.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp).clickable { onBack() },
                contentScale = ContentScale.Fit
            )
            Text(
                "Настройки безопасности",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121),
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            // ===== Защита входа: тумблеры =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        ToggleRow(
                            "PIN-код при входе",
                            if (uiState.localPinEnabled) "PIN запрашивается при каждом открытии" else "Приложение открывается без PIN-кода",
                            uiState.localPinEnabled,
                            onToggle = { viewModel.onTogglePin(it) }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.06f))
                        ToggleRow(
                            "Вход по отпечатку",
                            if (uiState.useBiometric) "Вход выполняется по отпечатку" else "Использовать отпечаток вместо кода",
                            uiState.useBiometric,
                            enabled = uiState.localPinEnabled,
                            onToggle = { viewModel.onToggleBiometric(it) }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    ActionRow("Изменить PIN-код", Icons.Default.Lock, onClick = onChangePin)
                }
            }

            // ===== Доверенные устройства =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    ActionRow(
                        "Доверенные устройства",
                        Icons.Default.Smartphone,
                        subtitle = deviceCountLabel(uiState.trustedDevices.size),
                        onClick = { showDevicesSheet = true }
                    )
                }
            }
// ===== Двухфакторная аутентификация =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 20.dp, bottom = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Двухфакторная аутентификация",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0x993C3C43),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "Информация",
                        modifier = Modifier.size(20.dp).clickable { showInfoDialog = true },
                        tint = Color(0x998E8E93)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        StatusRow(
                            VerificationMethod.EMAIL.label, Icons.Default.Email,
                            when { uiState.emailVerified -> "Подтверждён"; !uiState.email.isNullOrBlank() -> "Не подтверждён"; else -> "Не задан" },
                            if (uiState.emailVerified) "Отвязать" else "Активировать",
                            onAction = {}
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.06f))
                        StatusRow(
                            VerificationMethod.TELEGRAM.label, Icons.Default.Send,
                            if (uiState.telegramLinked) "Привязан" else "Не привязан",
                            if (uiState.telegramLinked) "Отвязать" else "Активировать",
                            onAction = {
                                if (uiState.telegramLinked) viewModel.onUnlinkTelegram()
                                else viewModel.onLinkTelegram { url -> context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.06f))
                        StatusRow(
                            VerificationMethod.MAX.label, Icons.Default.Chat,
                            "Скоро", null, onAction = {}
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showDevicesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDevicesSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            TrustedDevicesSheetContent(devices = uiState.trustedDevices, onRevoke = { viewModel.revokeTrustedDevice(it) })
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Двухфакторная аутентификация", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121)) },
            text = { Text("Дополнительный способ подтверждения, что аккаунт принадлежит вам. При входе с нового устройства мы отправим код на выбранный канал: электронную почту, Telegram или Макс. Это защищает аккаунт от входа посторонних.", fontSize = 15.sp, color = Color(0x993C3C43)) },
            confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("Понятно", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold) } },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/** Русская плюрализация: 1 устройство / 2 устройства / 5 устройств. */
private fun deviceCountLabel(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "$n устройство"
    n % 10 in 2..4 && (n % 100 < 12 || n % 100 > 14) -> "$n устройства"
    else -> "$n устройств"
}

@Composable
private fun ToggleRow(label: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, color = Color(0xFF212121))
            Text(subtitle, fontSize = 12.sp, color = Color(0x993C3C43))
        }
        Switch(checked = checked, onCheckedChange = onToggle, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF007AFF)))
    }
}

@Composable
private fun ActionRow(label: String, icon: ImageVector, subtitle: String = "", onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, label, Modifier.size(24.dp), tint = Color(0xFF212121))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, color = Color(0xFF212121))
            if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = Color(0x993C3C43))
        }
        Icon(Icons.Default.ChevronRight, ">", Modifier.size(20.dp), tint = Color(0x993C3C43))
    }
}

@Composable
private fun StatusRow(label: String, icon: ImageVector, status: String, actionLabel: String?, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, label, Modifier.size(24.dp), tint = Color(0xFF212121))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, color = Color(0xFF212121))
            Text(status, fontSize = 12.sp, color = Color(0x993C3C43))
        }
        if (actionLabel != null) {
            TextButton(onClick = if (status != "Скоро") onAction else {{}}) {
                Text(actionLabel, color = if (status == "Скоро") Color(0x993C3C43) else Color(0xFF007AFF), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TrustedDevicesSheetContent(devices: List<TrustedDeviceDto>, onRevoke: (String) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
        Text("Доверенные устройства", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121), modifier = Modifier.padding(bottom = 4.dp))
        Text("Этим устройствам не требуется подтверждение входа по звонку", fontSize = 13.sp, color = Color(0x993C3C43))
        Spacer(Modifier.height(12.dp))
        devices.forEach { device ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color(0xFF212121))
                Column(Modifier.weight(1f)) {
                    Text(device.name?.ifBlank { "Устройство" } ?: "Устройство", fontSize = 15.sp, color = Color(0xFF212121))
                    Text(buildString { append("Добавлено "); append(dateFormat.format(Date(device.createdAt))); if (device.currentDevice) append(" · это устройство") }, fontSize = 12.sp, color = Color(0x993C3C43))
                }
                if (!device.currentDevice) TextButton(onClick = { onRevoke(device.id) }) { Text("Отозвать", color = Color(0xFFE53935), fontSize = 13.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}