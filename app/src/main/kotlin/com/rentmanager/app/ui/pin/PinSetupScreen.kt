package com.rentmanager.app.ui.pin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PinSetupScreen(
    isOnboarding: Boolean = false,
    isRecovery: Boolean = false,
    onForgot: () -> Unit = {},
    onBack: () -> Unit,
    onSuccess: () -> Unit = onBack,
    viewModel: PinSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    BackHandler { if (!state.isLoading && (!isOnboarding || isRecovery)) onBack() }
    LaunchedEffect(state.isSuccess) { if (state.isSuccess) onSuccess() }
    val current = state.step == PinSetupStep.VERIFY_CURRENT
    val confirm = state.step == PinSetupStep.CONFIRM
    Box(Modifier.fillMaxSize()) {
        PinContent(
            // Шапка со стрелкой — только создание и подтверждение:
            // у входных экранов тулбара нет (макет 18, ветка «после правок»)
            toolbarTitle = when {
                current -> null
                confirm -> "Подтвердите код доступа"
                else -> "Создайте код доступа"
            },
            title = when {
                current && state.lockSeconds > 0 -> pinLockText(state.lockSeconds)
                current -> "Введите код доступа"
                confirm -> "Введите тот же код ещё раз"
                else -> "Придумайте 4-значный код для быстрого входа"
            },
            // 20.sp — вход/блокировка, 18.sp — создание и подтверждение (замеры макета)
            titleSize = if (current) 20.sp else 18.sp,
            pin = when { current -> state.currentPin; confirm -> state.confirmPin; else -> state.pin },
            errorMessage = state.errorMessage, errorId = state.errorId, isErrorAnimating = state.isErrorAnimating,
            enabled = !state.isLoading && !state.isErrorAnimating && (!current || state.lockSeconds == 0L),
            onDigit = if (current) viewModel::onCurrentPinDigit else viewModel::onDigitEntered,
            onDelete = if (current) viewModel::onCurrentPinDelete else viewModel::onDeleteDigit,
            onBack = if ((!isOnboarding || isRecovery) && !state.isLoading) onBack else null,
            onForgot = if (current) onForgot else null,
            loading = state.isLoading
        )
    }
}

enum class PinSetupStep { VERIFY_CURRENT, ENTER, CONFIRM }