package com.rentmanager.app.ui.auth.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.CallCheckAddResponse
import com.rentmanager.app.data.api.RegisterDeviceRequest
import com.rentmanager.app.data.api.SendCodeRequest
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerifyUiState(
    val phone: String = "+7",
    val selectedCountry: CountryPhone = CountryPhone.defaultCountry,
    val isLoading: Boolean = false,
    val isCalling: Boolean = false,
    val callPhone: String = "",
    val callPhonePretty: String = "",
    val isVerified: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class VerifyViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerifyUiState())
    val uiState: StateFlow<VerifyUiState> = _uiState.asStateFlow()

    private var callCheckJob: Job? = null

    /**
     * Принимает чистые цифры (только 0-9), максимум из selectedCountry.maxDigits.
     * Сохраняет phone = prefix + digits.
     */
    fun onDigitsChange(digits: String) {
        val state = _uiState.value
        val maxDigits = state.selectedCountry.maxDigits
        val clean = digits.filter { it.isDigit() }.take(maxDigits)
        _uiState.update { it.copy(phone = state.selectedCountry.phonePrefix + clean, errorMessage = null) }
    }

    fun onCountrySelected(country: CountryPhone) {
        _uiState.update { it.copy(selectedCountry = country, phone = country.phonePrefix, errorMessage = null) }
    }

    fun onContinue(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        val phone = state.phone
        val digits = phone.removePrefix(state.selectedCountry.phonePrefix)
        if (digits.length != state.selectedCountry.maxDigits) {
            _uiState.update { it.copy(errorMessage = "Введите номер полностью") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // 1. Проверяем, есть ли пользователь в БД
                val loginResp = authApi.login(SendCodeRequest(phone))
                if (loginResp.isSuccessful) {
                    val body = loginResp.body()
                    if (body?.exists == true) {
                        // Пользователь уже зарегистрирован — сохраняем токен и идём дальше
                        // Сохраняем токены (новый формат access_token/refresh_token)
                        body.accessToken?.let { tokenManager.accessToken = it }
                        body.refreshToken?.let { tokenManager.refreshToken = it }
                        // Обратная совместимость со старым форматом token
                        body.token?.let { tokenManager.accessToken = it }
                        body.user?.name?.let { tokenManager.userName = it }
                        body.user?.defaultStartScreen?.let { tokenManager.defaultStartScreen = it }
                        body.user?.passwordHash?.let { tokenManager.hasPassword = it.isNotEmpty() }
                        tokenManager.phone = phone
                        // Регистрируем FCM-токен
                        tokenManager.fcmToken?.let { fcm ->
                            launch { try { authApi.registerDevice(RegisterDeviceRequest(fcm)) } catch (_: Exception) {} }
                        }
                        _uiState.update { it.copy(isLoading = false, isVerified = true) }
                        onSuccess(phone)
                        return@launch
                    }
                }

                // 2. Пользователя нет — инициируем звонок
                val callResp = authApi.callCheckAdd(SendCodeRequest(phone))
                if (callResp.isSuccessful) {
                    val callBody = callResp.body()
                    if (callBody?.status == "OK") {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isCalling = true,
                                callPhone = callBody.callPhone,
                                callPhonePretty = callBody.callPhonePretty
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка сервиса звонков") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Сервер недоступен") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun startCallChecking(onSuccess: (String) -> Unit) {
        val phone = _uiState.value.phone
        callCheckJob?.cancel()
        callCheckJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < 30) {
                delay(3000)
                attempts++
                try {
                    val resp = authApi.callCheckStatus(SendCodeRequest(phone))
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (body?.verified == true) {
                            body.accessToken?.let { tokenManager.accessToken = it }
                            body.refreshToken?.let { tokenManager.refreshToken = it }
                            body.token?.let { tokenManager.accessToken = it }
                            body.user?.name?.let { tokenManager.userName = it }
                            body.user?.defaultStartScreen?.let { tokenManager.defaultStartScreen = it }
                            tokenManager.phone = phone
                            // Регистрируем FCM-токен
                            tokenManager.fcmToken?.let { fcm ->
                                launch { try { authApi.registerDevice(RegisterDeviceRequest(fcm)) } catch (_: Exception) {} }
                            }
                            _uiState.update { it.copy(isVerified = true, isCalling = false) }
                            onSuccess(phone)
                            return@launch
                        }
                    }
                } catch (_: Exception) { }
            }
            _uiState.update { it.copy(isCalling = false, errorMessage = "Время истекло. Попробуйте снова.") }
        }
    }

    fun reset() {
        callCheckJob?.cancel()
        callCheckJob = null
        _uiState.value = VerifyUiState()
    }
}
