package com.rentmanager.app.ui.auth.name

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.SaveNameRequest
import com.rentmanager.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnterNameUiState(
    val name: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class EnterNameViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnterNameUiState())
    val uiState: StateFlow<EnterNameUiState> = _uiState.asStateFlow()

    fun onNameChange(newName: String) {
        // Автоматически поднимаем первую букву: «иван» → «Иван» на лету
        val capitalized = if (newName.isEmpty()) newName
        else newName[0].uppercaseChar() + newName.substring(1)
        _uiState.update { it.copy(name = capitalized, errorMessage = null) }
    }

    fun onContinue(onSuccess: () -> Unit) {
        val name = _uiState.value.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Введите имя") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val response = authApi.saveName(SaveNameRequest(name))
                if (response.isSuccessful) {
                    tokenManager.userName = name
                    response.body()?.let { user ->
                        tokenManager.userName = user.name
                    }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка сохранения") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Нет связи с сервером") }
            }
        }
    }
}
