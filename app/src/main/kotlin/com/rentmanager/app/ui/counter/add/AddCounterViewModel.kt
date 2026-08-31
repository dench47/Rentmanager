package com.rentmanager.app.ui.counter.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Состояние формы «Добавить счетчик» (Figma 2692:31197).
 * Даты хранятся в отображаемом виде dd.MM.yyyy и конвертируются в формат API yyyy-MM-dd
 * при сохранении. Заводской номер — только цифры (префикс «№ » рисуется в UI).
 * Переключатели напоминаний уходят в API (remind_verification / remind_readings).
 */
data class AddCounterUiState(
    val counterType: String = "",
    val counterNumber: String = "",
    val initialValue: String = "",
    val nextVerificationDate: String = "",
    val remindVerification: Boolean = false,
    val submitReadingsBy: String = "",
    val remindReadings: Boolean = false,
    val types: List<String> = listOf("Электроэнергия", "Холодная вода", "Горячая вода", "Отопление"),
    val isTypeDropdownOpen: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class AddCounterViewModel @Inject constructor(
    private val repository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCounterUiState())
    val uiState: StateFlow<AddCounterUiState> = _uiState.asStateFlow()

    /** Счётчик успешно добавлен — экран можно закрыть. */
    private val _savedEvents = MutableSharedFlow<Unit>()
    val savedEvents: SharedFlow<Unit> = _savedEvents.asSharedFlow()

    /** Одноразовые ошибки для UI (Toast). */
    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    fun selectType(type: String) {
        _uiState.update { it.copy(counterType = type, isTypeDropdownOpen = false) }
    }

    fun toggleTypeDropdown() {
        _uiState.update { it.copy(isTypeDropdownOpen = !it.isTypeDropdownOpen) }
    }

    fun onNumberChange(value: String) {
        // Заводской номер — только цифры; «№ » в начале — постоянный префикс поля, не значение
        _uiState.update { it.copy(counterNumber = value.filter { ch -> ch.isDigit() }) }
    }

    fun onValueChange(value: String) {
        _uiState.update { it.copy(initialValue = value) }
    }

    fun onNextVerificationDateChange(value: String) {
        _uiState.update { it.copy(nextVerificationDate = value) }
    }

    fun onSubmitReadingsByChange(value: String) {
        _uiState.update { it.copy(submitReadingsBy = value) }
    }

    fun toggleRemindVerification() {
        _uiState.update { it.copy(remindVerification = !it.remindVerification) }
    }

    fun toggleRemindReadings() {
        _uiState.update { it.copy(remindReadings = !it.remindReadings) }
    }

    /**
     * Режим черновика (шаг 4 создания — объекта ещё нет): валидирует форму
     * и собирает MeterDraft без запроса к API; отправит его CreateProperty-флоу
     * после создания объекта.
     */
    fun buildDraft(): com.rentmanager.app.ui.landlord.createproperty.MeterDraft? {
        val state = _uiState.value
        val apiType = state.counterType.toApiType()
        if (apiType.isBlank()) {
            viewModelScope.launch { _errorEvents.emit("Выберите тип счётчика") }
            return null
        }
        if (state.counterNumber.isBlank()) {
            viewModelScope.launch { _errorEvents.emit("Введите заводской номер") }
            return null
        }
        return com.rentmanager.app.ui.landlord.createproperty.MeterDraft(
            type = apiType,
            typeLabel = state.counterType,
            factoryNumber = state.counterNumber.trim(),
            nextVerificationDate = state.nextVerificationDate.toApiDate(),
            currentValue = state.initialValue.toDoubleOrNull() ?: 0.0,
            unit = apiType.toUnit(),
            submitReadingsBy = state.submitReadingsBy.trim(),
            remindVerification = state.remindVerification,
            remindReadings = state.remindReadings
        )
    }

    /** Создаёт счётчик на сервере (POST /properties/{id}/meters). */
    fun addMeter(propertyId: String) {        if (_uiState.value.isSaving) return
        val state = _uiState.value
        val apiType = state.counterType.toApiType()
        if (apiType.isBlank()) {
            viewModelScope.launch { _errorEvents.emit("Выберите тип счётчика") }
            return
        }
        if (state.counterNumber.isBlank()) {
            viewModelScope.launch { _errorEvents.emit("Введите заводской номер") }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val meter = MeterDto(
                    id = "",
                    propertyId = propertyId,
                    type = apiType,
                    factoryNumber = state.counterNumber.trim(),
                    nextVerificationDate = state.nextVerificationDate.toApiDate(),
                    currentValue = state.initialValue.toDoubleOrNull() ?: 0.0,
                    unit = apiType.toUnit(),
                    // День месяца (1–31), а не дата: «передавать показания до N-го числа»
                    submitReadingsBy = state.submitReadingsBy.trim(),
                    lastUpdated = null,
                    remindVerification = state.remindVerification,
                    remindReadings = state.remindReadings
                )
                val resp = repository.createMeter(propertyId, meter)
                if (resp.isSuccessful) {
                    _savedEvents.emit(Unit)
                } else {
                    _errorEvents.emit("Не удалось добавить счётчик (${resp.code()})")
                }
            } catch (e: Exception) {
                _errorEvents.emit(e.message ?: "Ошибка сети")
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}

/** Русская подпись типа счётчика → значение enum API (MeterDto.type). */
private fun String.toApiType(): String = when (this) {
    "Электроэнергия" -> "electricity"
    "Холодная вода" -> "cold_water"
    "Горячая вода" -> "hot_water"
    "Отопление" -> "heat"
    else -> ""
}

/** Единица измерения по типу счётчика (API). */
private fun String.toUnit(): String = when (this) {
    "electricity" -> "кВт·ч"
    "heat" -> "Гкал"
    else -> "м³"
}

/** Отображаемая дата dd.MM.yyyy → формат API yyyy-MM-dd. */
private fun String.toApiDate(): String {
    if (isBlank()) return ""
    return try {
        val display = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val api = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        api.format(display.parse(this) ?: return "")
    } catch (_: Exception) {
        ""
    }
}