package com.rentmanager.app.ui.pin

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

    if (uiState.isSuccess) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
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

        Spacer(Modifier.height(32.dp))

        when (uiState.step) {
            PinSetupStep.VERIFY_CURRENT -> {
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
