package com.rentmanager.app.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.local.DeviceIdManager
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * «Не помните код доступа?» → «Войти в аккаунт»: запускаем восстановление PIN.
 * Дальше граф сам уводит на вход по номеру (гейт pinRecovery в NavGraph), после
 * входа клиент удаляет PIN (setPassword с пустым паролем) и открывает создание.
 */
@HiltViewModel
class PinForgotViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val deviceIdManager: DeviceIdManager
) : ViewModel() {

    fun beginRecovery() {
        viewModelScope.launch {
            // Есть ли другое доверенное устройство — от него можно одобрить вход
            val otherDevice = withTimeoutOrNull(3000) {
                try {
                    authApi.listDevices(deviceIdManager.deviceId).body()
                        ?.any { !it.currentDevice } == true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                }
            } ?: false
            tokenManager.beginPinRecovery(otherDevice)
        }
    }
}
