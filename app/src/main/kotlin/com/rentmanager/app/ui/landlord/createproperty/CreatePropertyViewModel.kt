package com.rentmanager.app.ui.landlord.createproperty

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.local.LocationProvider
import com.rentmanager.app.data.local.PropertyDetailCache
import com.rentmanager.app.data.model.PhotoDto
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.AddressSuggestion
import com.rentmanager.app.data.repository.GeoRepository
import com.rentmanager.app.data.repository.PhotoUploader
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePropertyUiState(
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    val addressSuggestions: List<AddressSuggestion> = emptyList(),
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val addressToSet: String? = null,
    val addressError: String? = null,
    /** Объект, загруженный для редактирования (не null только в режиме правки). */
    val editProperty: PropertyDto? = null
)

@HiltViewModel
class CreatePropertyViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val photoUploader: PhotoUploader,
    private val geoRepository: GeoRepository,
    private val detailCache: PropertyDetailCache,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePropertyUiState())
    val uiState: StateFlow<CreatePropertyUiState> = _uiState.asStateFlow()

    private var suggestJob: Job? = null
    private var baseAddress: String = ""
    private var scopeBbox: String? = null
    private var refinePrefix: String? = null

    fun suggestAddress(fullText: String) {
        suggestJob?.cancel()

        // Если пользователь стёр всё — сбрасываем накопленную базу и область поиска
        if (fullText.trim().isEmpty()) {
            baseAddress = ""
            scopeBbox = null
            refinePrefix = null
            _uiState.value = _uiState.value.copy(addressSuggestions = emptyList(), addressError = null)
            return
        }

        // «Активный сегмент» — то, что пользователь печатает после уже выбранной базы
        val segment = when {
            baseAddress.isNotEmpty() && fullText.endsWith(baseAddress) ->
                fullText.removeSuffix(baseAddress).trim(' ', ',', '-', '.', ';')
            baseAddress.isNotEmpty() && fullText.startsWith(baseAddress) ->
                fullText.removePrefix(baseAddress).trim(' ', ',', '-', '.', ';')
            else -> fullText.trim()
        }

        // Порог: 1 символ при уточнении номера дома, 3 — для общего поиска
        val minLength = if (refinePrefix != null) 1 else 3
        if (segment.length < minLength) {
            _uiState.value = _uiState.value.copy(addressSuggestions = emptyList(), addressError = null)
            return
        }

        // Запрос: «улица номер» при уточнении дома, иначе сам сегмент
        val query = if (refinePrefix != null) "$refinePrefix $segment" else segment

        suggestJob = viewModelScope.launch {
            delay(300)
            var lat: Double? = null
            var lon: Double? = null
            if (scopeBbox == null && refinePrefix == null) {
                locationProvider.lastKnownLocation()?.let { pair ->
                    lat = pair.first
                    lon = pair.second
                }
            }
            val suggestions = geoRepository.suggest(query, scopeBbox, lat, lon)
            _uiState.value = _uiState.value.copy(addressSuggestions = suggestions, addressError = null)
        }
    }

    fun selectAddress(suggestion: AddressSuggestion) {
        baseAddress = suggestion.displayName
        val type = suggestion.type ?: ""

        if (type == "street") {
            // Улица: оставляем городской bbox, запоминаем имя для уточнения номера дома
            refinePrefix = suggestion.displayName.substringBefore(",").trim()
        } else {
            refinePrefix = null
            if (type in setOf("city", "town", "village", "hamlet", "state", "country", "district", "county", "municipality")) {
                scopeBbox = suggestion.extent?.let { ext ->
                    if (ext.size >= 4) ext.take(4).joinToString(",") else null
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            addressSuggestions = emptyList(),
            selectedLatitude = suggestion.latitude,
            selectedLongitude = suggestion.longitude,
            addressError = null
        )
    }

    fun clearAddressSelection() {
        baseAddress = ""
        scopeBbox = null
        refinePrefix = null
        _uiState.value = _uiState.value.copy(
            addressSuggestions = emptyList(),
            selectedLatitude = null,
            selectedLongitude = null,
            addressError = null
        )
    }

    /** Восстановление адреса/метки после возврата с шага 4 */
    fun restoreSelection(address: String, lat: Double?, lon: Double?) {
        if (lat != null && lon != null) {
            baseAddress = address
            scopeBbox = null
            refinePrefix = null
            _uiState.value = _uiState.value.copy(
                selectedLatitude = lat,
                selectedLongitude = lon,
                addressSuggestions = emptyList(),
                addressError = null
            )
        }
    }

    fun onMapTapped(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            selectedLatitude = lat,
            selectedLongitude = lon
        )
        viewModelScope.launch {
            val suggestion = geoRepository.reverse(lat, lon)
            if (suggestion != null) {
                baseAddress = suggestion.displayName
                scopeBbox = null
                refinePrefix = null
                _uiState.value = _uiState.value.copy(
                    addressToSet = suggestion.displayName,
                    addressSuggestions = emptyList()
                )
            }
        }
    }

    fun consumeAddressToSet() {
        _uiState.value = _uiState.value.copy(addressToSet = null)
    }

    fun commitAddress(text: String) {
        val trimmed = text.trim().trimEnd(',', ' ')
        suggestJob?.cancel()
        _uiState.value = _uiState.value.copy(addressSuggestions = emptyList())
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val loc = locationProvider.lastKnownLocation()
            val suggestions = geoRepository.suggest(trimmed, null, loc?.first, loc?.second)
            val best = suggestions.firstOrNull()
            if (best == null) {
                _uiState.value = _uiState.value.copy(addressError = "Укажите валидный адрес")
                return@launch
            }
            val needCity = (best.type == "street" || best.type == "house" || best.type == "locality") && !trimmed.contains(",")
            if (needCity) {
                // Улица/дом без города: не ставим метку, ждём город или выбор из подсказок
                _uiState.value = _uiState.value.copy(
                    selectedLatitude = null,
                    selectedLongitude = null,
                    addressError = "Добавьте город"
                )
            } else {
                baseAddress = best.displayName
                scopeBbox = null
                refinePrefix = null
                _uiState.value = _uiState.value.copy(
                    selectedLatitude = best.latitude,
                    selectedLongitude = best.longitude,
                    addressError = null
                )
            }
        }
    }

    fun createProperty(
        name: String,
        address: String,
        area: String?,
        rentAmount: String?,
        description: String?,
        photoUris: List<String>,
        serviceInfo: String?,
        phone: String?,
        wifiPassword: String?,
        houseRules: String?,
        type: String?,
        rentType: String?,
        rooms: String?,
        sleepingPlaces: String?,
        floor: String?,
        floorsInHouse: String?,
        latitude: Double?,
        longitude: Double?,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            try {
                // 1. Загружаем фото в S3 (папка photos), по аналогии с аватаркой
                val photoUrls = photoUris.map { photoUploader.upload(it.toUri()) }

                // 2. Создаём объект в БД
                val dto = PropertyDto(
                    name = name,
                    address = address.trim().trimEnd(',', ' '),
                    area = area?.toDoubleOrNull(),
                    rentAmount = rentAmount?.toDoubleOrNull(),
                    description = description,
                    photos = photoUrls.map { PhotoDto(url = it) },
                    serviceInfo = serviceInfo,
                    phone = phone,
                    wifiPassword = wifiPassword,
                    houseRules = houseRules,
                    type = type,
                    rentType = rentType,
                    rooms = rooms,
                    sleepingPlaces = sleepingPlaces,
                    floor = floor,
                    floorsInHouse = floorsInHouse,
                    latitude = latitude,
                    longitude = longitude
                )
                val resp = propertyRepository.createProperty(dto)
                if (resp.isSuccessful) {
                    val created = resp.body()
                    created?.let { detailCache.saveProperty(it) }
                    onSuccess(created?.id ?: "")
                } else {
                    _uiState.value = _uiState.value.copy(isCreating = false, errorMessage = "Ошибка создания объекта")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isCreating = false, errorMessage = e.message ?: "Ошибка")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Загружает объект для редактирования: мгновенно из кэша (если есть),
     * затем актуальные данные с сервера.
     */
    fun loadForEdit(propertyId: String) {
        if (propertyId.isBlank()) return
        if (_uiState.value.editProperty?.id == propertyId) return
        viewModelScope.launch {
            val cached = try {
                detailCache.load(propertyId)?.property
            } catch (_: Exception) {
                null
            }
            if (cached != null) {
                _uiState.value = _uiState.value.copy(editProperty = cached)
            }
            try {
                val resp = propertyRepository.getProperty(propertyId)
                val body = if (resp.isSuccessful) resp.body() else null
                if (body != null) {
                    detailCache.saveProperty(body)
                    _uiState.value = _uiState.value.copy(editProperty = body)
                } else if (cached == null) {
                    _uiState.value =
                        _uiState.value.copy(errorMessage = "Не удалось загрузить объект (${resp.code()})")
                }
            } catch (e: Exception) {
                if (cached == null) {
                    _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Ошибка сети")
                }
            }
        }
    }

    /**
     * Сохраняет изменения существующего объекта (PUT): сначала поля,
     * затем удаление убранных фото и загрузка добавленных.
     */
    fun updateProperty(
        propertyId: String,
        name: String,
        address: String,
        area: String?,
        rentAmount: String?,
        description: String?,
        photoUris: List<String>,
        serviceInfo: String?,
        phone: String?,
        wifiPassword: String?,
        houseRules: String?,
        type: String?,
        rentType: String?,
        rooms: String?,
        sleepingPlaces: String?,
        floor: String?,
        floorsInHouse: String?,
        latitude: Double?,
        longitude: Double?,
        onSuccess: (String) -> Unit
    ) {
        val original = _uiState.value.editProperty
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            try {
                // 1. Сохраняем поля объекта
                val dto = PropertyDto(
                    name = name,
                    address = address.trim().trimEnd(',', ' '),
                    area = area?.toDoubleOrNull(),
                    rentAmount = rentAmount?.toDoubleOrNull(),
                    description = description,
                    serviceInfo = serviceInfo,
                    phone = phone,
                    wifiPassword = wifiPassword,
                    houseRules = houseRules,
                    type = type,
                    rentType = rentType,
                    rooms = rooms,
                    sleepingPlaces = sleepingPlaces,
                    floor = floor,
                    floorsInHouse = floorsInHouse,
                    latitude = latitude,
                    longitude = longitude
                )
                val resp = propertyRepository.updateProperty(propertyId, dto)
                if (!resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        errorMessage = "Ошибка сохранения объекта (${resp.code()})"
                    )
                    return@launch
                }

                // 2. Удаляем существующие фото, которые убрали из списка
                // (локальные uri — не http, они обрабатываются шагом 3)
                val keptUrls = photoUris.filter { it.startsWith("http") }
                original?.photos.orEmpty().forEach { photo ->
                    if (photo.id != null && photo.url !in keptUrls) {
                        try {
                            propertyRepository.deletePhoto(photo.id)
                        } catch (_: Exception) {
                        }
                    }
                }

                // 3. Загружаем новые локальные фото и прикрепляем к объекту
                photoUris.filter { !it.startsWith("http") }.forEach { uri ->
                    try {
                        val url = photoUploader.upload(uri.toUri())
                        propertyRepository.addPhoto(propertyId, url)
                    } catch (_: Exception) {
                    }
                }

                // 4. Перечитываем финальное состояние и обновляем кэш
                val finalResp = propertyRepository.getProperty(propertyId)
                val finalDto = if (finalResp.isSuccessful) finalResp.body() else resp.body()
                if (finalDto != null) detailCache.saveProperty(finalDto)
                _uiState.value = _uiState.value.copy(isCreating = false, editProperty = finalDto)
                onSuccess(propertyId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isCreating = false, errorMessage = e.message ?: "Ошибка")
            }
        }
    }
}
