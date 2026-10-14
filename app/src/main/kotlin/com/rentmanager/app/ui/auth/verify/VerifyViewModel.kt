package com.rentmanager.app.ui.auth.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.EmailLoginCodeRequest
import com.rentmanager.app.data.api.EmailLoginVerifyRequest
import com.rentmanager.app.data.api.RegisterDeviceRequest
import com.rentmanager.app.data.api.RequestApprovalRequest
import com.rentmanager.app.data.api.SendCodeRequest
import com.rentmanager.app.data.api.SetPasswordRequest
import com.rentmanager.app.data.api.TelegramCodeRequest
import com.rentmanager.app.data.api.TelegramVerifyRequest
import com.rentmanager.app.data.local.DeviceIdManager
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import javax.inject.Inject

data class VerifyUiState(
    val phone: String = "+7",
    val selectedCountry: CountryPhone = CountryPhone.defaultCountry,
    val isLoading: Boolean = false,
    val isCalling: Boolean = false,
    val callPhone: String = "",
    val callPhonePretty: String = "",
    val isVerified: Boolean = false,
    val isNewUser: Boolean = false,
    // ===== Device Trust: ожидание push-одобрения с доверенного устройства =====
    val awaitingApproval: Boolean = false,
    // ===== Telegram-вход =====
    val canTelegram: Boolean = false,
    val telegramCodeSent: Boolean = false,
    val telegramAttemptsLeft: Int = 5,
    val telegramCodeError: String? = null,
    // ===== Email-вход =====
    val canEmail: Boolean = false,
    val emailCodeSent: Boolean = false,
    val emailAttemptsLeft: Int = 5,
    val emailCodeError: String? = null,
    val errorMessage: String? = null,
    val recovery: Boolean = false
)

@HiltViewModel
class VerifyViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val deviceIdManager: DeviceIdManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VerifyUiState(
            phone = tokenManager.recoveryPhone ?: "+7",
            selectedCountry = CountryPhone.availableCountries.firstOrNull {
                tokenManager.recoveryPhone?.startsWith(it.phonePrefix) == true
            } ?: CountryPhone.defaultCountry,
            recovery = tokenManager.pinRecovery
        )
    )
    val uiState: StateFlow<VerifyUiState> = _uiState.asStateFlow()

    private var callCheckJob: Job? = null
    private var approvalJob: Job? = null
    private var codeJob: Job? = null
    private var callRequestJob: Job? = null

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
        if (state.isLoading) return
        val phone = state.phone
        val digits = phone.removePrefix(state.selectedCountry.phonePrefix)
        if (digits.length != state.selectedCountry.maxDigits) {
            _uiState.update { it.copy(errorMessage = "Введите номер полностью") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // 1. Проверяем пользователя в БД и доверено ли это устройство
                val loginResp = authApi.login(
                    // Без идентификатора доверенного устройства login не выдаёт
                    // recovery-токены и возвращает способы нового подтверждения.
                    SendCodeRequest(phone, tokenManager.fcmToken,
                        if (state.recovery) null else deviceIdManager.deviceId, deviceIdManager.deviceName)
                )
                coroutineContext.ensureActive()
                if (loginResp.isSuccessful) {
                    val body = loginResp.body()
                    if (body?.exists == true) {
                        tokenManager.phone = phone
                        body.name?.let { tokenManager.userName = it }
                        body.defaultStartScreen?.let { tokenManager.defaultStartScreen = it }

                        val recovery = _uiState.value.recovery

                        // В recovery доступны все подтверждения, объявленные сервером.
                        // Доверенное устройство не может завершить recovery автоматически.
                        _uiState.update { it.copy(
                            canTelegram = body.canTelegram == true,
                            canEmail = body.canEmail == true
                        ) }

                        // Доверенное устройство без PIN — сервер сразу выдал токены
                        if (!recovery && body.accessToken != null) {
                            tokenManager.accessToken = body.accessToken
                            body.refreshToken?.let { tokenManager.refreshToken = it }
                            body.token?.let { tokenManager.accessToken = it }
                            body.user?.name?.let { tokenManager.userName = it }
                            body.user?.defaultStartScreen?.let { tokenManager.defaultStartScreen = it }
                            // Сервер выдаёт токены сразу только аккаунтам без PIN,
                            // но берём значение из ответа, а не предполагаем:
                            // безусловный false однажды затирал факт «PIN есть»
                            tokenManager.hasPassword = body.hasPassword ?: false
                            if (finishConfirmation(isNewUser = false)) onSuccess(phone)
                            return@launch
                        }

                        // Новое устройство + есть доверенные → push-подтверждение входа
                        if (body.canPush == true &&
                            ((!recovery && body.isTrustedDevice == false) ||
                                (recovery && tokenManager.recoveryHasOtherDevice && phone == tokenManager.recoveryPhone))) {
                            tokenManager.hasPassword = body.hasPassword ?: true
                            startApprovalFlow(phone)
                            return@launch
                        }

                        // Новое устройство, PIN не установлен, подтверждать нечем —
                        // единственный путь: звонок (владение SIM). Иначе пользователь
                        // попал бы на экран PIN, который заведомо не пройдёт.
                        if ((body.isTrustedDevice == false && body.hasPassword != true) ||
                            (recovery && !tokenManager.recoveryHasOtherDevice && body.canPush != true)) {
                            fallbackToCall()
                            return@launch
                        }

                        // Обычный доверенный вход с PIN переходит к PinEntry.
                        // Recovery не имеет ветки «ввести старый PIN».
                        if (recovery) {
                            fallbackToCall()
                            return@launch
                        }
                        tokenManager.hasPassword = body.hasPassword ?: true
                        _uiState.update { it.copy(isLoading = false, isVerified = true, isNewUser = false, canTelegram = false) }
                        onSuccess(phone)
                        return@launch
                    }
                }

                // 2. Пользователя нет (или login упал) — инициируем звонок
                val callResp = authApi.callCheckAdd(SendCodeRequest(phone))
                coroutineContext.ensureActive()
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
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    /**
     * Запрос подтверждения входа на доверенные устройства + поллинг статуса.
     * Push уходит на все доверенные устройства; пользователь тапает «Подтвердить».
     */
    fun startApprovalFlow(phone: String) {
        if (_uiState.value.recovery &&
            (!tokenManager.recoveryHasOtherDevice || phone != tokenManager.recoveryPhone)) {
            fallbackToCall()
            return
        }
        viewModelScope.launch {
            try {
                val resp = authApi.requestLoginApproval(
                    RequestApprovalRequest(phone, deviceIdManager.deviceId, deviceIdManager.deviceName)
                )
                coroutineContext.ensureActive()
                val requestId = resp.body()?.requestId
                if (!resp.isSuccessful || requestId == null) {
                    // Не удалось запросить одобрение — откатываемся на звонок
                    fallbackToCall()
                    return@launch
                }
                _uiState.update { it.copy(awaitingApproval = true, isLoading = false) }
                pollApprovalStatus(requestId, phone)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    private fun pollApprovalStatus(requestId: String, phone: String) {
        approvalJob?.cancel()
        approvalJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < 100 && _uiState.value.awaitingApproval) { // ~5 мин, как TTL на сервере
                delay(3000)
                attempts++
                try {
                    val resp = authApi.loginStatus(requestId, deviceIdManager.deviceId)
                    coroutineContext.ensureActive()
                    val body = resp.body()
                    when (body?.status) {
                        "approved" -> {
                            body.accessToken?.let { tokenManager.accessToken = it }
                            body.refreshToken?.let { tokenManager.refreshToken = it }
                            body.user?.name?.let { tokenManager.userName = it }
                            body.user?.defaultStartScreen?.let { tokenManager.defaultStartScreen = it }
                            tokenManager.phone = phone
                            tokenManager.hasPassword = body.hasPassword ?: true
                            finishConfirmation(isNewUser = false)
                            return@launch
                        }
                        "denied" -> {
                            _uiState.update { it.copy(awaitingApproval = false, errorMessage = "Вход отклонён на другом устройстве") }
                            return@launch
                        }
                        "expired" -> {
                            _uiState.update { it.copy(awaitingApproval = false, errorMessage = "Время подтверждения истекло") }
                            return@launch
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { /* сеть мигнула — продолжаем поллинг */ }
            }
            if (_uiState.value.awaitingApproval) {
                _uiState.update { it.copy(awaitingApproval = false, errorMessage = "Время подтверждения истекло") }
            }
        }
    }

    /** Отмена ожидания одобрения — возврат к вводу номера. */
    fun cancelApproval() {
        approvalJob?.cancel()
        approvalJob = null
        _uiState.update { it.copy(awaitingApproval = false) }
    }

    /** Останавливает фоновый поллинг push-одобрения, не трогая состояние экрана. */
    private fun stopApprovalPolling() {
        approvalJob?.cancel()
        approvalJob = null
    }

    /**
     * Fallback: push недоступен/отклонён запрос невозможен — верификация звонком,
     * если SIM при пользователе.
     */
    fun fallbackToCall() {
        cancelApproval()
        callCheckJob?.cancel()
        codeJob?.cancel()
        callRequestJob?.cancel()
        _uiState.update { it.copy(isLoading = true, isCalling = false, telegramCodeSent = false, emailCodeSent = false) }
        callRequestJob = viewModelScope.launch {
            try {
                val callResp = authApi.callCheckAdd(SendCodeRequest(_uiState.value.phone))
                coroutineContext.ensureActive()
                val callBody = callResp.body()
                if (callResp.isSuccessful && callBody?.status == "OK") {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            awaitingApproval = false,
                            isCalling = true,
                            callPhone = callBody.callPhone,
                            callPhonePretty = callBody.callPhonePretty
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка сервиса звонков") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Сервер недоступен") }
            }
        }
    }

    private fun registerFcm() {
        tokenManager.fcmToken?.let { fcm ->
            viewModelScope.launch {
                try { authApi.registerDevice(RegisterDeviceRequest(fcm, deviceIdManager.deviceId)) } catch (_: Exception) {}
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
                    val resp = authApi.callCheckStatus(
                        SendCodeRequest(phone, tokenManager.fcmToken, deviceIdManager.deviceId, deviceIdManager.deviceName)
                    )
                    coroutineContext.ensureActive()
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (body?.verified == true) {
                            body.accessToken?.let { tokenManager.accessToken = it }
                            body.refreshToken?.let { tokenManager.refreshToken = it }
                            body.token?.let { tokenManager.accessToken = it }
                            body.user?.name?.let { tokenManager.userName = it }
                            body.user?.defaultStartScreen?.let { tokenManager.defaultStartScreen = it }
                            tokenManager.phone = phone
                            tokenManager.hasPassword = body.hasPassword ?: false
                            if (finishConfirmation(body.isNewUser ?: false)) onSuccess(phone)
                            return@launch
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
            }
            _uiState.update { it.copy(isCalling = false, errorMessage = "Время истекло. Попробуйте снова.") }
        }
    }

    // ===== Telegram-вход =====

    /** Отправляет код входа в Telegram (для недоверенного устройства). */
    fun onTelegramLogin() {
        val phone = _uiState.value.phone
        callCheckJob?.cancel()
        callRequestJob?.cancel()
        codeJob?.cancel()
        // Уходим с экрана ожидания push-одобрения: останавливаем поллинг,
        // но держим экран, пока идёт запрос кода (без мигания).
        stopApprovalPolling()
        _uiState.update { it.copy(telegramCodeError = null, telegramCodeSent = false, emailCodeSent = false, isCalling = false) }
        codeJob = viewModelScope.launch {
            try {
                val resp = authApi.telegramCode(TelegramCodeRequest(phone))
                coroutineContext.ensureActive()
                val body = resp.body()
                if (resp.isSuccessful && body != null) {
                    _uiState.update { it.copy(telegramCodeSent = true, telegramAttemptsLeft = body.attemptsLeft, awaitingApproval = false) }
                } else {
                    val err = resp.errorBody()?.string()
                    _uiState.update { it.copy(telegramCodeError = err ?: "Не удалось отправить код", awaitingApproval = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(telegramCodeError = "Нет связи с сервером", awaitingApproval = false) }
            }
        }
    }

    /** Проверяет введённый код из Telegram. */
    fun onVerifyTelegramCode(code: String, onSuccess: (String) -> Unit) {
        if (codeJob?.isActive == true || _uiState.value.isVerified) return
        val phone = _uiState.value.phone
        if (code.length != 8) {
            _uiState.update { it.copy(telegramCodeError = "Введите 8 цифр кода") }
            return
        }
        _uiState.update { it.copy(telegramCodeError = null) }
        codeJob = viewModelScope.launch {
            try {
                val resp = authApi.telegramVerify(
                    TelegramVerifyRequest(
                        phone = phone,
                        code = code,
                        fcmToken = tokenManager.fcmToken,
                        deviceId = deviceIdManager.deviceId,
                        deviceName = deviceIdManager.deviceName
                    )
                )
                coroutineContext.ensureActive()
                val body = resp.body()
                if (resp.isSuccessful && body?.verified == true) {
                    body.accessToken?.let { tokenManager.accessToken = it }
                    body.refreshToken?.let { tokenManager.refreshToken = it }
                    body.token?.let { tokenManager.accessToken = it }
                    body.user?.name?.let { tokenManager.userName = it }
                    body.user?.defaultStartScreen?.let { tokenManager.defaultStartScreen = it }
                    tokenManager.phone = phone
                    tokenManager.hasPassword = body.hasPassword ?: false
                    if (finishConfirmation(isNewUser = false)) onSuccess(phone)
                } else {
                    _uiState.update { it.copy(telegramCodeError = "Неверный или истёкший код") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(telegramCodeError = "Нет связи с сервером") }
            }
        }
    }

    /** Закрывает окно ввода Telegram-кода. */
    fun cancelTelegramCode() {
        reset()
    }

    // ===== Email-вход =====

    /** Отправляет код входа на подтверждённую почту (для недоверенного устройства). */
    fun onEmailLogin() {
        val phone = _uiState.value.phone
        callCheckJob?.cancel()
        callRequestJob?.cancel()
        codeJob?.cancel()
        // Уходим с экрана ожидания push-одобрения: останавливаем поллинг,
        // но держим экран, пока идёт запрос кода (без мигания).
        stopApprovalPolling()
        _uiState.update { it.copy(emailCodeError = null, emailCodeSent = false, telegramCodeSent = false, isCalling = false) }
        codeJob = viewModelScope.launch {
            try {
                val resp = authApi.emailLoginCode(EmailLoginCodeRequest(phone))
                coroutineContext.ensureActive()
                val body = resp.body()
                if (resp.isSuccessful && body != null) {
                    _uiState.update { it.copy(emailCodeSent = true, emailAttemptsLeft = body.attemptsLeft, awaitingApproval = false) }
                } else {
                    val err = resp.errorBody()?.string()
                    _uiState.update { it.copy(emailCodeError = err ?: "Не удалось отправить код", awaitingApproval = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(emailCodeError = "Нет связи с сервером", awaitingApproval = false) }
            }
        }
    }

    /** Проверяет введённый код из письма. */
    fun onVerifyEmailCode(code: String, onSuccess: (String) -> Unit) {
        if (codeJob?.isActive == true || _uiState.value.isVerified) return
        val phone = _uiState.value.phone
        if (code.length != 6) {
            _uiState.update { it.copy(emailCodeError = "Введите 6 цифр кода") }
            return
        }
        _uiState.update { it.copy(emailCodeError = null) }
        codeJob = viewModelScope.launch {
            try {
                val resp = authApi.emailLoginVerify(
                    EmailLoginVerifyRequest(
                        phone = phone,
                        code = code,
                        fcmToken = tokenManager.fcmToken,
                        deviceId = deviceIdManager.deviceId,
                        deviceName = deviceIdManager.deviceName
                    )
                )
                coroutineContext.ensureActive()
                val body = resp.body()
                if (resp.isSuccessful && body?.verified == true) {
                    body.accessToken?.let { tokenManager.accessToken = it }
                    body.refreshToken?.let { tokenManager.refreshToken = it }
                    body.token?.let { tokenManager.accessToken = it }
                    body.user?.name?.let { tokenManager.userName = it }
                    body.user?.defaultStartScreen?.let { tokenManager.defaultStartScreen = it }
                    tokenManager.phone = phone
                    tokenManager.hasPassword = body.hasPassword ?: false
                    if (finishConfirmation(isNewUser = false)) onSuccess(phone)
                } else {
                    _uiState.update { it.copy(emailCodeError = "Неверный или истёкший код") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(emailCodeError = "Нет связи с сервером") }
            }
        }
    }

    /** Закрывает окно ввода Email-кода. */
    fun cancelEmailCode() {
        reset()
    }

    fun reset() {
        viewModelScope.coroutineContext.cancelChildren()
        callCheckJob?.cancel()
        callCheckJob = null
        approvalJob?.cancel()
        approvalJob = null
        codeJob = null
        callRequestJob = null
        if (tokenManager.pinRecovery) tokenManager.cancelRecoverySession()
        _uiState.value = VerifyUiState(recovery = tokenManager.pinRecovery)
    }

    /** Вызывается только после звонка, кода или одобрения другого устройства. */
    private suspend fun finishConfirmation(isNewUser: Boolean): Boolean {
        coroutineContext.ensureActive()
        if (_uiState.value.recovery) {
            _uiState.update { it.copy(isLoading = true) }
            val reset = try {
                tokenManager.accessToken != null && authApi.setPassword(SetPasswordRequest("")).isSuccessful
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            coroutineContext.ensureActive()
            if (!reset) {
                tokenManager.cancelRecoverySession()
                _uiState.update { it.copy(
                    isLoading = false, isCalling = false, awaitingApproval = false,
                    telegramCodeSent = false, emailCodeSent = false,
                    errorMessage = "Не удалось сбросить код. Подтвердите вход ещё раз"
                ) }
                return false
            }
            tokenManager.hasPassword = false
            tokenManager.pinLockedUntil = 0L
            tokenManager.requirePin = false
            tokenManager.recoveryPasswordReset = true
        }
        registerFcm()
        _uiState.update { it.copy(
            isLoading = false, isVerified = true, isNewUser = isNewUser,
            isCalling = false, awaitingApproval = false,
            telegramCodeSent = false, emailCodeSent = false,
            canTelegram = false, canEmail = false
        ) }
        return true
    }
}
