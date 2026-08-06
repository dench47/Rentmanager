package com.rentmanager.app.ui.auth.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.ConfirmCodeRequest
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmsCodeUiState(
    val code: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SmsCodeViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmsCodeUiState())
    val uiState: StateFlow<SmsCodeUiState> = _uiState.asStateFlow()

    fun setPhoneNumber(number: String) {
        _uiState.update { it.copy(phoneNumber = number) }
    }

    fun onCodeChange(newCode: String) {
        if (newCode.length <= 6) {
            _uiState.update { it.copy(code = newCode, errorMessage = null) }
        }
    }

    fun onCodeComplete(onSuccess: () -> Unit) {
        val code = _uiState.value.code
        val phone = _uiState.value.phoneNumber
        if (code.length < 6) {
            _uiState.update { it.copy(errorMessage = "Введите 6 цифр") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val response = authApi.confirmCode(ConfirmCodeRequest(phone, code))
                if (response.isSuccessful) {
                    response.body()?.let { auth ->
                        tokenManager.accessToken = auth.token
                        tokenManager.phone = phone
                        tokenManager.userName = auth.user.name.takeIf { it.isNotEmpty() }
                    }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Неверный код") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }
}
