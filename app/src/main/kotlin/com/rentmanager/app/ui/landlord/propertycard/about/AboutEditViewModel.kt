package com.rentmanager.app.ui.landlord.propertycard.about

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AboutEditUiState(
    val isLoading: Boolean = true,
    val property: PropertyDto? = null,
    val isSaving: Boolean = false,
    /** Исходные значения — для «Сбросить изменения». */
    val initial: PropertyDto? = null
)

/** Экран «Об объекте» (Figma 2726-33827): редактирование основных параметров объекта. */
@HiltViewModel
class AboutEditViewModel @Inject constructor(
    private val repository: PropertyRepository,
    private val detailCache: PropertyDetailCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutEditUiState())
    val uiState: StateFlow<AboutEditUiState> = _uiState.asStateFlow()

    private val _savedEvents = MutableSharedFlow<Unit>()
    val savedEvents: SharedFlow<Unit> = _savedEvents.asSharedFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    fun load(propertyId: String) {
        if (propertyId.isBlank()) return
        viewModelScope.launch {
            // мгновенно из кэша, затем актуальные данные с сервера
            val cached = detailCache.load(propertyId)?.property
            _uiState.value = AboutEditUiState(isLoading = false, property = cached, initial = cached)
            try {
                val resp = repository.getProperty(propertyId)
                val body = if (resp.isSuccessful) resp.body() else null
                if (body != null) {
                    detailCache.saveProperty(body)
                    _uiState.update { it.copy(property = body, initial = body) }
                }
            } catch (_: Exception) {
            }
        }
    }

    /** Сохранение основных параметров (аналог saveAboutInfo карточки). */
    fun save(
        name: String,
        address: String,
        rooms: String?,
        area: String,
        sleepingPlaces: String?,
        floor: String?,
        floorsInHouse: String?,
        description: String,
        rentAmount: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val current = _uiState.value.property ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val dto = current.copy(
                    name = name,
                    address = address.trim().trimEnd(',', ' '),
                    rooms = rooms,
                    area = area.toDoubleOrNull(),
                    sleepingPlaces = sleepingPlaces,
                    floor = floor,
                    floorsInHouse = floorsInHouse,
                    description = description.ifBlank { null },
                    rentAmount = rentAmount.toDoubleOrNull(),
                    latitude = latitude ?: current.latitude,
                    longitude = longitude ?: current.longitude
                )
                val resp = repository.updateProperty(current.id, dto)
                if (resp.isSuccessful) {
                    detailCache.saveProperty(dto)
                    _uiState.update { it.copy(isSaving = false) }
                    _savedEvents.emit(Unit)
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                    _errorEvents.emit("Не удалось сохранить изменения (${resp.code()})")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _errorEvents.emit(e.message ?: "Ошибка сети")
            }
        }
    }
}
