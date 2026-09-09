package com.rentmanager.app.ui.landlord.propertycard

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.BookingApi
import com.rentmanager.app.data.api.CreateBookingRequest
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.model.PaymentScheduleDto
import com.rentmanager.app.data.model.forProperty
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PropertyCardUiState(
    val isLoading: Boolean = true,
    val property: PropertyDto? = null,
    /** Счётчики объекта для жёлтой секции «Счетчики» (Figma 2574:20899). */
    val meters: List<MeterDto> = emptyList(),
    /** График платежей объекта — задаёт «X ₽ / месяц(сутки)» в «Арендной плате». */
    val schedule: PaymentScheduleDto? = null,
    /** Посуточно: дата последней брони (дд.ММ.гггг) для «Срока аренды» */
    val lastBookingEnd: String? = null,
    /** Блокировка действий, пока выполняется публикация/удаление. */
    val isActionInProgress: Boolean = false
)

@HiltViewModel
class PropertyCardViewModel @Inject constructor(
    private val repository: PropertyRepository,
    private val photoUploader: PhotoUploader,
    private val detailCache: PropertyDetailCache,
    private val bookingApi: BookingApi,
    private val financeApi: FinanceApi
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
            // пустым состоянием («Нет фотографий», «Без названия» и т.п.).
            // График — тоже из кэша: иначе «Арендная плата» на полсекунды
            // мелькает ставкой объявления (28000), пока едет график (30000)
            val cachedEntry = detailCache.load(propertyId)
            _uiState.value = PropertyCardUiState(
                isLoading = true,
                property = cachedEntry?.property,
                schedule = cachedEntry?.schedule,
                meters = previousMeters
            )
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
                // График платежей: тип задаёт «₽ / месяц(сутки)», сумма — актуальную ставку
                val schedule = try {
                    financeApi.getSchedules().body()?.forProperty(propertyId)
                } catch (_: Exception) {
                    null
                }
                // Посуточно: «Срок аренды» — дата последней брони в календаре
                val lastBookingEnd = if ((body?.rentType ?: "посуточно") == "посуточно") {
                    try {
                        bookingApi.getBookings(propertyId).body().orEmpty()
                            .mapNotNull { b ->
                                runCatching { java.time.LocalDate.parse(b.endDate) }.getOrNull()
                            }
                            .maxOrNull()
                            ?.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
                _uiState.value = PropertyCardUiState(
                    isLoading = false, property = body, meters = meters, schedule = schedule,
                    lastBookingEnd = lastBookingEnd
                )
                if (body != null) {
                    detailCache.saveProperty(body)
                    detailCache.saveSchedule(propertyId, schedule)
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

    /** «Внести показание» из шита: пишет в историю и обновляет список счётчиков. */
    fun submitReading(meterId: String, value: Double) {
        val propertyId = _uiState.value.property?.id ?: return
        viewModelScope.launch {
            try {
                repository.submitReading(meterId, value)
                reloadMeters(propertyId)
                _savedEvents.emit("Показание сохранено")
            } catch (_: Exception) {
                _errorEvents.emit("Не удалось сохранить показание")
            }
        }
    }

    /** Обновляет только счётчики (возврат с экранов счётчиков/ввода показания). */
    fun reloadMeters(propertyId: String) {
        viewModelScope.launch {
            try {
                val meters = repository.getMeters(propertyId).body().orEmpty()
                _uiState.value = _uiState.value.copy(meters = meters)
            } catch (_: Exception) { }
        }
    }

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
        val rentEndDateChanged = current.rentEndDate != rentEndDate.takeIf { it.isNotBlank() }
        saveProperty(
            current.copy(
                rentAmount = parseAmount(rentAmount),
                rentEndDate = rentEndDate.takeIf { it.isNotBlank() }
            ),
            "Изменения сохранены",
            onSaved = { saved ->
                val schedule = _uiState.value.schedule
                // Тип отображения = тип графика; без графика — по типу аренды объекта
                val type = schedule?.type
                    ?: if ((saved.rentType ?: "посуточно") == "длительно") "auto" else "manual"
                // Помесячный режим: срок аренды ↔ шахматка. Изменение поля
                // перекрашивает шахматку от текущего месяца до месяца даты
                if (rentEndDateChanged && type == "auto") {
                    syncBookingsToRentEnd(saved)
                }
                if (type == "auto") {
                    // Ставка в карточке = сумма постоянного графика — держим их равными
                    runCatching {
                        financeApi.createSchedule(
                            com.rentmanager.app.data.model.PaymentScheduleDto(
                                propertyId = saved.id,
                                dayOfMonth = schedule?.dayOfMonth,
                                amount = saved.rentAmount,
                                type = "auto",
                                requisites = schedule?.requisites
                            )
                        )
                        _uiState.value = _uiState.value.copy(
                            schedule = (schedule ?: com.rentmanager.app.data.model.PaymentScheduleDto(
                                propertyId = saved.id, type = "auto"
                            )).copy(amount = saved.rentAmount)
                        )
                    }
                }
            }
        )
    }

    /**
     * Длительная аренда: брони = от 1-го числа текущего месяца до «Срок аренды».
     * Очистка поля — будущие брони снимаются (шахматка очищается).
     */
    private suspend fun syncBookingsToRentEnd(property: PropertyDto) {
        try {
            val ddMMyyyy = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val today = LocalDate.now()
            val monthStart = today.withDayOfMonth(1)
            val target = property.rentEndDate?.let {
                runCatching { LocalDate.parse(it, ddMMyyyy) }.getOrNull()
            }
            val existing = bookingApi.getBookings(property.id).body().orEmpty()
            // Снимаем брони текущего и будущих месяцев (прошлые не трогаем)
            existing.forEach { b ->
                val s = runCatching { LocalDate.parse(b.startDate) }.getOrNull()
                if (s != null && !s.isBefore(monthStart)) {
                    b.id?.let { runCatching { bookingApi.deleteBooking(property.id, it) } }
                }
            }
            if (target != null && !target.isBefore(today)) {
                bookingApi.createBooking(
                    property.id,
                    CreateBookingRequest(monthStart.toString(), target.toString())
                )
            }
        } catch (_: Exception) { }
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

    private fun saveProperty(
        dto: PropertyDto,
        successMessage: String,
        onSaved: (suspend (PropertyDto) -> Unit)? = null
    ) {
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
                    onSaved?.invoke(body)
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