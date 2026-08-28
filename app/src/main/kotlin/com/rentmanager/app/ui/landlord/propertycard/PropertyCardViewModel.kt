package com.rentmanager.app.ui.landlord.propertycard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PropertyCardUiState(
    val isLoading: Boolean = true,
    val property: PropertyDto? = null,
    /** Блокировка действий, пока выполняется публикация/удаление. */
    val isActionInProgress: Boolean = false
)

@HiltViewModel
class PropertyCardViewModel @Inject constructor(
    private val repository: PropertyRepository,
    private val detailCache: PropertyDetailCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyCardUiState())
    val uiState: StateFlow<PropertyCardUiState> = _uiState.asStateFlow()

    /** Одноразовые ошибки для UI (Toast). */
    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    /** Событие «объект удалён» — экран должен закрыться. */
    private val _deleted = MutableSharedFlow<Unit>()
    val deleted: SharedFlow<Unit> = _deleted.asSharedFlow()

    fun load(propertyId: String) {
        if (propertyId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = PropertyCardUiState(isLoading = true)
            try {
                val resp = repository.getProperty(propertyId)
                val body = if (resp.isSuccessful) resp.body() else null
                _uiState.value = PropertyCardUiState(isLoading = false, property = body)
                if (body != null) {
                    detailCache.saveProperty(body)
                } else {
                    _errorEvents.emit("Не удалось загрузить объект (${resp.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = PropertyCardUiState(isLoading = false)
                _errorEvents.emit(e.message ?: "Ошибка сети")
            }
        }
    }

    fun publish() = setPublished(published = true)

    fun unpublish() = setPublished(published = false)

    private fun setPublished(published: Boolean) {
        val id = _uiState.value.property?.id ?: return
        if (_uiState.value.isActionInProgress) return
        _uiState.value = _uiState.value.copy(isActionInProgress = true)
        viewModelScope.launch {
            try {
                val resp = if (published) {
                    repository.publishProperty(id)
                } else {
                    repository.unpublishProperty(id)
                }
                val body = if (resp.isSuccessful) resp.body() else null
                if (body != null) {
                    detailCache.saveProperty(body)
                    _uiState.value = _uiState.value.copy(
                        isActionInProgress = false,
                        property = body
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isActionInProgress = false)
                    _errorEvents.emit(
                        if (published) "Не удалось опубликовать объявление (${resp.code()})"
                        else "Не удалось снять объявление с публикации (${resp.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionInProgress = false)
                _errorEvents.emit(e.message ?: "Ошибка сети")
            }
        }
    }

    fun deleteProperty() {
        val id = _uiState.value.property?.id ?: return
        if (_uiState.value.isActionInProgress) return
        _uiState.value = _uiState.value.copy(isActionInProgress = true)
        viewModelScope.launch {
            try {
                val resp = repository.deleteProperty(id)
                if (resp.isSuccessful) {
                    detailCache.remove(id)
                    _deleted.emit(Unit)
                } else {
                    _uiState.value = _uiState.value.copy(isActionInProgress = false)
                    _errorEvents.emit("Не удалось удалить объект (${resp.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionInProgress = false)
                _errorEvents.emit(e.message ?: "Ошибка сети")
            }
        }
    }
}