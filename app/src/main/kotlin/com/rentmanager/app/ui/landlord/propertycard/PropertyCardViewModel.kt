package com.rentmanager.app.ui.landlord.propertycard

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.PhotoUploader
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
    /** Счётчики объекта для жёлтой секции «Счетчики» (Figma 2574:20899). */
    val meters: List<MeterDto> = emptyList(),
    /** Блокировка действий, пока выполняется публикация/удаление. */
    val isActionInProgress: Boolean = false
)

@HiltViewModel
class PropertyCardViewModel @Inject constructor(
    private val repository: PropertyRepository,
    private val photoUploader: PhotoUploader,
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

    /** Одноразовые события «изменения сохранены» для UI (Toast). */
    private val _savedEvents = MutableSharedFlow<String>()
    val savedEvents: SharedFlow<String> = _savedEvents.asSharedFlow()

    fun load(propertyId: String) {
        if (propertyId.isBlank()) return
        viewModelScope.launch {
            val previousMeters = _uiState.value.meters
            // Сразу отрисовываем кэш: без этого до ответа сети экран мигал
            // пустым состоянием («Нет фотографий», «Без названия» и т.п.)
            val cached = detailCache.load(propertyId)?.property
            _uiState.value = PropertyCardUiState(isLoading = true, property = cached, meters = previousMeters)
            try {
                val resp = repository.getProperty(propertyId)
                val body = if (resp.isSuccessful) resp.body() else null
                // Счётчики грузим отдельным эндпоинтом и не роняем карточку при ошибке
                val meters = try {
                    val mResp = repository.getMeters(propertyId)
                    if (mResp.isSuccessful) mResp.body().orEmpty() else previousMeters
                } catch (_: Exception) {
                    previousMeters
                }
                _uiState.value = PropertyCardUiState(isLoading = false, property = body, meters = meters)
                if (body != null) {
                    detailCache.saveProperty(body)
                } else {
                    _errorEvents.emit("Не удалось загрузить объект (${resp.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
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

    // ---------- Быстрое редактирование секций карточки (шиты по карандашам) ----------

    /** Шит «Аренда и платежи»: ставка и дата окончания аренды. */
    fun saveRentInfo(rentAmount: String, rentEndDate: String) {
        val current = _uiState.value.property ?: return
        saveProperty(
            current.copy(
                rentAmount = parseAmount(rentAmount),
                rentEndDate = rentEndDate.takeIf { it.isNotBlank() }
            ),
            "Изменения сохранены"
        )
    }

    // «25 000» / «25,5» → Double: пробелы-разделители разрядов выкидываем, запятую — в точку
    private fun parseAmount(raw: String): Double? =
        raw.filter { it.isDigit() || it == '.' || it == ',' }
            .replace(',', '.')
            .toDoubleOrNull()

    /** Шит «Арендатор и договор»: ровно три поля (арендатор, договор «№… от …», телефон). */
    fun saveTenantInfo(tenantInfo: String, contractText: String, phone: String) {
        val current = _uiState.value.property ?: return
        val text = contractText.trim()
        val match = Regex("""№\s*(\S+)\s+от\s+(\d{2}\.\d{2}\.\d{4})""").find(text)
        saveProperty(
            current.copy(
                tenantInfo = tenantInfo.takeIf { it.isNotBlank() },
                contractNumber = match?.groupValues?.get(1)
                    ?: text.takeIf { it.isNotBlank() }?.let { if (it.startsWith("№")) it.removePrefix("№").trim() else it },
                contractDate = match?.groupValues?.get(2)
                    ?: text.takeIf { it.isNotBlank() }?.let { current.contractDate },
                phone = phone.takeIf { it.isNotBlank() }
            ),
            "Изменения сохранены"
        )
    }

    /** Инлайн-режим «Информация об объекте»: контактный телефон, пароль WiFi и правила. */
    fun saveObjectInfo(phone: String, wifiPassword: String, houseRules: String) {
        val current = _uiState.value.property ?: return
        // Пустые значения отправляем как пустые строки, а не как null:
        // Gson выбрасывает null-поля из JSON, сервер считает их «не переданными»
        // и очистка поля не сохранялась бы
        saveProperty(
            current.copy(
                phone = phone.trim(),
                wifiPassword = wifiPassword.trim(),
                houseRules = houseRules.trim()
            ),
            "Изменения сохранены"
        )
    }


    /** Заметка «Служебная информация» в аккордеоне — сохранение по потере фокуса. */
    fun saveServiceInfo(text: String) {
        val current = _uiState.value.property ?: return
        if (current.serviceInfo.orEmpty() == text) return
        // Пустая строка вместо null — иначе очистка заметки не сохранится (см. saveObjectInfo)
        saveProperty(
            current.copy(serviceInfo = text.trim()),
            "Изменения сохранены"
        )
    }

    /** Шит «Об объекте»: основные параметры и стоимость. */
    fun saveAboutInfo(
        name: String,
        address: String,
        rooms: String?,
        area: String,
        sleepingPlaces: String?,
        floor: String?,
        floorsInHouse: String?,
        description: String,
        rentAmount: String
    ) {
        val current = _uiState.value.property ?: return
        saveProperty(
            current.copy(
                name = name,
                address = address,
                rooms = rooms,
                area = area.toDoubleOrNull(),
                sleepingPlaces = sleepingPlaces,
                floor = floor,
                floorsInHouse = floorsInHouse,
                description = description.takeIf { it.isNotBlank() },
                rentAmount = parseAmount(rentAmount)
            ),
            "Изменения сохранены"
        )
    }

    private fun saveProperty(dto: PropertyDto, successMessage: String) {
        val id = dto.id.ifBlank { _uiState.value.property?.id } ?: return
        if (_uiState.value.isActionInProgress) return
        _uiState.value = _uiState.value.copy(isActionInProgress = true)
        viewModelScope.launch {
            try {
                val resp = repository.updateProperty(id, dto)
                val body = if (resp.isSuccessful) resp.body() else null
                if (body != null) {
                    detailCache.saveProperty(body)
                    _uiState.value = _uiState.value.copy(isActionInProgress = false, property = body)
                    _savedEvents.emit(successMessage)
                } else {
                    _uiState.value = _uiState.value.copy(isActionInProgress = false)
                    _errorEvents.emit("Не удалось сохранить изменения (${resp.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionInProgress = false)
                _errorEvents.emit(e.message ?: "Ошибка сети")
            }
        }
    }

    /** Удаление фото крестиком в шите: сразу из БД и S3 (серверный DeletePhoto),
     *  локально добавленные (ещё не загруженные) файлы просто уходят из списка. */
    fun deletePhoto(uri: String) {
        val current = _uiState.value.property ?: return
        val photo = current.photos.orEmpty().firstOrNull { it.url == uri }
        val photoId = photo?.id
        if (photoId == null) {
            // Локальный файл (content://) — на сервере его нет, убираем только из состояния
            return
        }
        if (_uiState.value.isActionInProgress) return
        _uiState.value = _uiState.value.copy(isActionInProgress = true)
        viewModelScope.launch {
            try {
                val resp = repository.deletePhoto(photoId)
                if (resp.isSuccessful) {
                    val updated = current.copy(photos = current.photos.orEmpty().filterNot { it.url == uri })
                    detailCache.saveProperty(updated)
                    _uiState.value = _uiState.value.copy(isActionInProgress = false, property = updated)
                } else {
                    _uiState.value = _uiState.value.copy(isActionInProgress = false)
                    _errorEvents.emit("Не удалось удалить фото (${resp.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionInProgress = false)
                _errorEvents.emit(e.message ?: "Ошибка сети")
            }
        }
    }

    /** Шит фотографий: добавить/удалить и сохранить (как в CreatePropertyViewModel.updateProperty). */
    fun savePhotos(photoUris: List<String>) {
        val current = _uiState.value.property ?: return
        if (_uiState.value.isActionInProgress) return
        _uiState.value = _uiState.value.copy(isActionInProgress = true)
        viewModelScope.launch {
            try {
                // 1. Удаляем фото, которые убрали из списка
                val keptUrls = photoUris.filter { it.startsWith("http") }
                current.photos.orEmpty().forEach { photo ->
                    if (photo.id != null && photo.url !in keptUrls) {
                        try {
                            repository.deletePhoto(photo.id)
                        } catch (_: Exception) {
                        }
                    }
                }

                // 2. Загружаем новые локальные фото
                photoUris.filter { !it.startsWith("http") }.forEach { uri ->
                    try {
                        val url = photoUploader.upload(uri.toUri())
                        repository.addPhoto(current.id, url)
                    } catch (_: Exception) {
                    }
                }

                // 3. Перечитываем итоговое состояние
                val finalResp = repository.getProperty(current.id)
                val finalDto = if (finalResp.isSuccessful) finalResp.body() else null
                if (finalDto != null) {
                    detailCache.saveProperty(finalDto)
                    _uiState.value = _uiState.value.copy(isActionInProgress = false, property = finalDto)
                    _savedEvents.emit("Изменения сохранены")
                } else {
                    _uiState.value = _uiState.value.copy(isActionInProgress = false)
                    _errorEvents.emit("Не удалось сохранить фото (${finalResp.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionInProgress = false)
                _errorEvents.emit(e.message ?: "Ошибка сети")
            }
        }
    }
}