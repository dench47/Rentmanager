package com.rentmanager.app.ui.pin

import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.data.local.BiometricAuthManager
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BiometricEntryPoint {
    fun biometricAuthManager(): BiometricAuthManager
}

@Composable
fun PinEntryScreen(
    onPinVerified: () -> Unit,
    onForgot: () -> Unit,
    tokenManager: TokenManager,
    viewModel: PinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val biometricManager = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, BiometricEntryPoint::class.java).biometricAuthManager()
    }
    val biometricEnabled = tokenManager.useBiometric && biometricManager.hasBiometricHardware()
    BackHandler { (context as? FragmentActivity)?.moveTaskToBack(true) }

    fun launchBiometric() {
        if (!biometricManager.canAuthenticate()) {
            viewModel.onBiometricError("Биометрия временно недоступна. Введите код доступа")
            return
        }
        var activityContext = context
        while (activityContext is ContextWrapper && activityContext !is FragmentActivity) {
            activityContext = activityContext.baseContext
        }
        val activity = activityContext as? FragmentActivity ?: return
        biometricManager.authenticate(
            activity, "Вход в приложение", "Приложите палец для входа",
            onSuccess = viewModel::onBiometricSuccess,
            onError = viewModel::onBiometricError,
            onFallback = {}
        )
    }
    LaunchedEffect(uiState.isVerified) { if (uiState.isVerified) onPinVerified() }
    Box(Modifier.fillMaxSize()) {
        PinContent(
            title = if (uiState.lockSeconds > 0) pinLockText(uiState.lockSeconds) else "Введите код доступа",
            // 20.sp — заголовок входа и блокировки (замеры макета 18)
            titleSize = 20.sp,
            // «Биометрия временно недоступна…» — подстрока про биометрию исчезает (макет)
            subtitle = if (biometricEnabled &&
                uiState.errorMessage?.startsWith("Биометрия временно недоступна") != true
            ) "или используйте биометрию" else null,
            pin = uiState.pin, errorMessage = uiState.errorMessage, errorId = uiState.errorId,
            isErrorAnimating = uiState.isErrorAnimating,
            enabled = !uiState.isLoading && !uiState.isErrorAnimating && uiState.lockSeconds == 0L,
            onDigit = viewModel::onDigitEntered, onDelete = viewModel::onDeleteDigit,
            onBiometric = if (biometricEnabled) ({ launchBiometric() }) else null,
            onForgot = onForgot, loading = uiState.isLoading
        )
    }
}