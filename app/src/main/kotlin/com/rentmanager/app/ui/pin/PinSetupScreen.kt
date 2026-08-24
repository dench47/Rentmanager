package com.rentmanager.app.ui.pin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.view.HapticFeedbackConstants
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    isOnboarding: Boolean = false,
    onBack: () -> Unit,
    viewModel: PinSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // BottomSheet со списком доверенных устройств
    var showDevicesSheet by remember { mutableStateOf(false) }

    // Fallback-синхронизация списка устройств: обновляем при каждом возврате на экран
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadTrustedDevices()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.isSuccess) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Стрелка «назад» + заголовок в одном ряду
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 60.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isOnboarding) {
                Image(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp).clickable { onBack() },
                    contentScale = ContentScale.Fit
                )
            } else {
                Spacer(Modifier.size(24.dp))
            }
            Text(
                when (uiState.step) {
                    PinSetupStep.VERIFY_CURRENT -> "Введите текущий код"
                    PinSetupStep.CONFIRM -> "Повторите код"
                    else -> "Придумайте новый код"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121),
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        if (!isOnboarding) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Требовать PIN на этом устройстве", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
                    Text(
                        if (uiState.localPinEnabled) "PIN запрашивается при каждом открытии" else "Приложение открывается без PIN-кода",
                        fontSize = 12.sp,
                        color = Color(0x993C3C43)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = uiState.localPinEnabled,
                    onCheckedChange = { requirePin -> viewModel.onTogglePinWithoutPin(!requirePin) },
                    enabled = !uiState.isLoading,
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF007AFF))
                )
            }

            // Вход по отпечатку — доступен только при установленном PIN и включённом запросе PIN
            if (uiState.isPasswordSet) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color(0xFF212121))
                        Column(Modifier.weight(1f)) {
                            Text("Вход по отпечатку", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
                            Text(
                                if (uiState.useBiometric) "Вход выполняется по отпечатку" else "Использовать отпечаток вместо кода",
                                fontSize = 12.sp,
                                color = Color(0x993C3C43)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = uiState.useBiometric,
                        onCheckedChange = { viewModel.toggleBiometric(it) },
                        enabled = uiState.localPinEnabled && !uiState.isLoading,
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF007AFF))
                    )
                }
            }

            // Доверенные устройства — компактная строка, полный список в BottomSheet
            if (uiState.trustedDevices.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { showDevicesSheet = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Smartphone,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF007AFF)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Доверенные устройства", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
                        Text(deviceCountLabel(uiState.trustedDevices.size), fontSize = 12.sp, color = Color(0x993C3C43))
                    }
                    Text("›", fontSize = 20.sp, color = Color(0x993C3C43))
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        when (uiState.step) {
            // Шаг: ввод текущего кода (смена)
            PinSetupStep.VERIFY_CURRENT -> {
                // Точки индикаторы
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier.size(16.dp).clip(CircleShape)
                                .background(if (index < uiState.currentPin.length) Color(0xFF007AFF) else Color(0xFFE0E0E0))
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))

                if (uiState.errorMessage != null) {
                    Text(uiState.errorMessage!!, color = Color(0xFFE53935), fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                }

                PinKeyboard(
                    pin = uiState.currentPin,
                    onDigit = { viewModel.onCurrentPinDigit(it) },
                    onDelete = { viewModel.onCurrentPinDelete() }
                )
            }

            // Шаг: ввод нового кода или подтверждение
            PinSetupStep.ENTER, PinSetupStep.CONFIRM -> {
                val isConfirm = uiState.step == PinSetupStep.CONFIRM
                Text(
                    when {
                        isConfirm -> "Введите код ещё раз для подтверждения"
                        else -> "Придумайте 4-значный код для входа"
                    },
                    fontSize = 14.sp, color = Color(0x993C3C43)
                )
                Spacer(Modifier.height(32.dp))

                // Точки индикаторы
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val activeLen = if (uiState.step == PinSetupStep.CONFIRM) uiState.confirmPin.length else uiState.pin.length
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier.size(16.dp).clip(CircleShape)
                                .background(if (index < activeLen) Color(0xFF007AFF) else Color(0xFFE0E0E0))
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (uiState.errorMessage != null) {
                    Text(uiState.errorMessage!!, color = Color(0xFFE53935), fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                }

                PinKeyboard(
                    pin = if (uiState.step == PinSetupStep.CONFIRM) uiState.confirmPin else uiState.pin,
                    onDigit = { viewModel.onDigitEntered(it) },
                    onDelete = { viewModel.onDeleteDigit() }
                )
            }
        }
    }

    // ===== Доверенные устройства: полный список в BottomSheet =====
    if (showDevicesSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showDevicesSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            TrustedDevicesSheetContent(
                devices = uiState.trustedDevices,
                onRevoke = { viewModel.revokeTrustedDevice(it) }
            )
        }
    }
}

/** Русская плюрализация: 1 устройство / 2 устройства / 5 устройств. */
private fun deviceCountLabel(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "$n устройство"
    n % 10 in 2..4 && (n % 100 < 12 || n % 100 > 14) -> "$n устройства"
    else -> "$n устройств"
}

@Composable
private fun TrustedDevicesSheetContent(
    devices: List<com.rentmanager.app.data.api.TrustedDeviceDto>,
    onRevoke: (String) -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
    ) {
        Text(
            "Доверенные устройства",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF212121),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "Этим устройствам не требуется подтверждение входа по звонку",
            fontSize = 13.sp,
            color = Color(0x993C3C43)
        )
        Spacer(Modifier.height(12.dp))
        devices.forEach { device ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Smartphone,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color(0xFF212121)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        device.name?.ifBlank { "Устройство" } ?: "Устройство",
                        fontSize = 15.sp,
                        color = Color(0xFF212121)
                    )
                    Text(
                        buildString {
                            append("Добавлено ")
                            append(dateFormat.format(java.util.Date(device.createdAt)))
                            if (device.currentDevice) append(" · это устройство")
                        },
                        fontSize = 12.sp,
                        color = Color(0x993C3C43)
                    )
                }
                if (!device.currentDevice) {
                    TextButton(onClick = { onRevoke(device.id) }) {
                        Text("Отозвать", color = Color(0xFFE53935), fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PinKeyboard(pin: String, onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    keys.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            row.forEach { key ->
                if (key.isEmpty()) {
                    Spacer(Modifier.size(72.dp))
                } else {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType(HapticFeedbackConstants.LONG_PRESS))
                            if (key == "⌫") onDelete() else onDigit(key)
                        },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF212121)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(key, fontSize = if (key == "⌫") 18.sp else 24.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

enum class PinSetupStep {
    VERIFY_CURRENT,
    ENTER,
    CONFIRM
}