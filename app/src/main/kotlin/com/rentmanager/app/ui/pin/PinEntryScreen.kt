package com.rentmanager.app.ui.pin

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.data.local.BiometricAuthManager
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Calendar

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BiometricEntryPoint {
    fun biometricAuthManager(): BiometricAuthManager
}

@Composable
fun PinEntryScreen(
    onPinVerified: () -> Unit,
    tokenManager: TokenManager,
    viewModel: PinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    val biometricManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            BiometricEntryPoint::class.java
        ).biometricAuthManager()
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..16 -> "Добрый день"
            in 17..22 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
    }
    val userName = remember { tokenManager.userName ?: "" }

    val canUseBiometric = remember {
        tokenManager.useBiometric && biometricManager.canAuthenticate()
    }

    fun findActivity(): androidx.fragment.app.FragmentActivity? {
        var c = view.context
        while (c is android.content.ContextWrapper) {
            if (c is androidx.fragment.app.FragmentActivity) return c
            c = c.baseContext
        }
        return null
    }

    fun launchBiometric() {
        val activity = findActivity() ?: return
        biometricManager.authenticate(
            activity = activity,
            title = "Вход в приложение",
            subtitle = "Приложите палец для входа",
            onSuccess = { pin -> viewModel.onBiometricSuccess(pin) },
            onError = { },
            onFallback = { }
        )
    }

    // Автозапуск биометрии при входе на экран
    LaunchedEffect(Unit) {
        if (canUseBiometric) {
            launchBiometric()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (userName.isNotEmpty()) "$greeting, $userName" else "Введите код доступа",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF212121)
        )
        Spacer(Modifier.height(32.dp))

        // Точки индикаторы
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < uiState.pin.length) Color(0xFF007AFF)
                            else Color(0xFFE0E0E0)
                        )
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // Цифровая клавиатура 3x4
        val haptic = LocalHapticFeedback.current
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            if (canUseBiometric) listOf("fingerprint", "0", "⌫") else listOf("", "0", "⌫")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(72.dp))
                        "fingerprint" -> {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType(HapticFeedbackConstants.LONG_PRESS))
                                    launchBiometric()
                                },
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF007AFF)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = "Войти по отпечатку", modifier = Modifier.size(32.dp))
                            }
                        }
                        "⌫" -> {
                            KeyButton(key) {
                                haptic.performHapticFeedback(HapticFeedbackType(HapticFeedbackConstants.LONG_PRESS))
                                viewModel.onDeleteDigit()
                            }
                        }
                        else -> {
                            KeyButton(key) {
                                haptic.performHapticFeedback(HapticFeedbackType(HapticFeedbackConstants.LONG_PRESS))
                                viewModel.onDigitEntered(key)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (uiState.errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                uiState.errorMessage!!,
                color = Color(0xFFE53935),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { viewModel.logout(onPinVerified) }) {
            Text("Выйти из аккаунта", color = Color(0x993C3C43), fontSize = 14.sp)
        }
    }

    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) onPinVerified()
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF212121)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            label,
            fontSize = if (label == "⌫") 18.sp else 24.sp,
            fontWeight = if (label == "⌫") FontWeight.Normal else FontWeight.Medium
        )
    }
}