package com.rentmanager.app.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.RegisterDeviceRequest
import com.rentmanager.app.data.api.SendCodeRequest
import com.rentmanager.app.data.api.UpdateProfileRequest
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val fullName: String = "",
    val phone: String = "",
    val email: String? = null,
    val legalName: String? = null,
    val avatarUrl: String? = null,
    val defaultStartScreen: String = "main",
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null,
    // Биометрия
    val useBiometric: Boolean = false,
    // Смена телефона
    val isChangingPhone: Boolean = false,
    val newPhone: String = "",
    val callPhone: String = "",
    val callPhonePretty: String = "",
    val showPhoneWarning: Boolean = false,
    // Выход
    val showLogoutDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    // Начальный экран
    val showStartScreenDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(
        phone = tokenManager.phone ?: "",
        defaultStartScreen = tokenManager.defaultStartScreen,
        avatarUrl = tokenManager.avatarUrl,
        useBiometric = tokenManager.useBiometric
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val resp = authApi.getMe()
                if (resp.isSuccessful) {
                    val user = resp.body()!!
                    val url = user.avatarUrl
                    _uiState.update {
                        it.copy(
                            userName = user.name,
                            fullName = user.fullName ?: "",
                            phone = user.phone,
                            email = user.email,
                            legalName = user.legalName,
                            avatarUrl = url
                        )
                    }
                    tokenManager.userName = user.name
                    tokenManager.phone = user.phone
                    tokenManager.avatarUrl = url
                }
            } catch (_: Exception) { }
        }
    }

    fun uploadAndSetAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                // 1. Upload file to server
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot open file")
                val bytes = inputStream.readBytes()
                inputStream.close()

                val fileName = getFileName(uri) ?: "avatar.jpg"
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

                val uploadResp = authApi.uploadAvatar(part)
                if (!uploadResp.isSuccessful) {
                    _uiState.update { it.copy(isUploading = false, errorMessage = "Ошибка загрузки файла") }
                    return@launch
                }
                val uploadedUrl = uploadResp.body()!!.url

                // 2. Update profile with uploaded URL
                val updateResp = authApi.updateProfile(UpdateProfileRequest(avatarUrl = uploadedUrl))
                if (updateResp.isSuccessful) {
                    val user = updateResp.body()!!
                    val newUrl = user.avatarUrl
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            avatarUrl = newUrl,
                            userName = user.name,
                            email = user.email,
                            legalName = user.legalName
                        )
                    }
                    tokenManager.avatarUrl = newUrl
                } else {
                    _uiState.update { it.copy(isUploading = false, errorMessage = "Ошибка сохранения") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isUploading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun updateProfile(
        name: String? = null,
        fullName: String? = null,
        email: String? = null,
        legalName: String? = null,
        avatarUrl: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resp = authApi.updateProfile(UpdateProfileRequest(name = name, fullName = fullName, email = email, legalName = legalName, avatarUrl = avatarUrl))
                if (resp.isSuccessful) {
                    val user = resp.body()!!
                    val newUrl = user.avatarUrl
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userName = user.name,
                            fullName = user.fullName ?: "",
                            email = user.email,
                            legalName = user.legalName,
                            avatarUrl = newUrl
                        )
                    }
                    tokenManager.avatarUrl = newUrl
                    tokenManager.userName = user.name
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка сохранения") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun onPhoneChangeRequest(newPhone: String) {
        val clean = newPhone.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(showPhoneWarning = true, newPhone = "+7$clean") }
    }

    fun dismissPhoneWarning() {
        _uiState.update { it.copy(showPhoneWarning = false) }
    }

    fun startPhoneVerification(onNavigate: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(showPhoneWarning = false, isLoading = true) }
            try {
                val resp = authApi.changePhone(SendCodeRequest(_uiState.value.newPhone))
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    _uiState.update {
                        it.copy(isLoading = false, isChangingPhone = true, callPhone = body.callPhone, callPhonePretty = body.callPhonePretty)
                    }
                    onNavigate()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }

    fun showLogoutDialog() { _uiState.update { it.copy(showLogoutDialog = true) } }

    fun logoutCurrentDevice(onLoggedOut: () -> Unit) {
        val fcm = tokenManager.fcmToken
        val finish = {
            tokenManager.clear()
            _uiState.update { it.copy(showLogoutDialog = false) }
            onLoggedOut()
        }
        if (fcm != null) {
            viewModelScope.launch {
                // Отвязываем FCM-токен на сервере, пока access-токен ещё валиден
                withTimeoutOrNull(3000) {
                    try {
                        val resp = authApi.unregisterDevice(RegisterDeviceRequest(fcm))
                        if (!resp.isSuccessful) android.util.Log.e("FCM", "unregister failed: ${resp.code()}")
                    } catch (e: Exception) {
                        android.util.Log.e("FCM", "unregister error: ${e.message}")
                    }
                }
                finish()
            }
        } else {
            finish()
        }
    }

    fun logoutAllDevices(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try { authApi.logoutAll() } catch (_: Exception) { }
            tokenManager.clear()
            _uiState.update { it.copy(showLogoutDialog = false) }
            onLoggedOut()
        }
    }

    fun showDeleteDialog() { _uiState.update { it.copy(showDeleteDialog = true) } }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try { authApi.deleteAccount() } catch (_: Exception) { }
            tokenManager.clear()
            _uiState.update { it.copy(showDeleteDialog = false) }
            onDeleted()
        }
    }

    fun showStartScreenDialog() { _uiState.update { it.copy(showStartScreenDialog = true) } }

    fun setDefaultStartScreen(screen: String) {
        tokenManager.defaultStartScreen = screen
        _uiState.update { it.copy(defaultStartScreen = screen, showStartScreenDialog = false) }
        viewModelScope.launch {
            try {
                authApi.updateProfile(UpdateProfileRequest(defaultStartScreen = screen))
            } catch (_: Exception) { }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        tokenManager.useBiometric = enabled
        _uiState.update { it.copy(useBiometric = enabled) }
    }

    fun dismissDialogs() {
        _uiState.update { it.copy(showLogoutDialog = false, showDeleteDialog = false, showStartScreenDialog = false, showPhoneWarning = false, errorMessage = null) }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }
}