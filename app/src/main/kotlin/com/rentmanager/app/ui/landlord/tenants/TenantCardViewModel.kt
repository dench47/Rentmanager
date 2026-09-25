package com.rentmanager.app.ui.landlord.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.model.TenantDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BookingEntryUi(
    val propertyId: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate,
    val propertyName: String,
    val propertyAddress: String,
    val propertyPhoto: String?
)

data class TenantCardUiState(
    val isLoading: Boolean = true,
    val tenant: TenantDto? = null,
    /** Текущая/будущая аренда (пересекается с сегодняшним днём или позже) */
    val currentBooking: BookingEntryUi? = null,
    /** Прошлые аренды, новые сверху */
    val pastBookings: List<BookingEntryUi> = emptyList(),
    /** Служебная информация развёрнута (аккордеон) */
    val serviceInfoExpanded: Boolean = false,
    /** История аренды развёрнута (аккордеон) */
    val historyExpanded: Boolean = false
)

@HiltViewModel
class TenantCardViewModel @Inject constructor(
    private val tenantApi: TenantApi,
    private val propertyApi: com.rentmanager.app.data.api.PropertyApi,
    private val tenantEvents: com.rentmanager.app.data.local.TenantEvents,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    /** Свободные объекты для шита «Прикрепить к объекту» (3005:52495) */
    private val _freeProperties = kotlinx.coroutines.flow.MutableStateFlow<List<com.rentmanager.app.data.model.PropertyDto>>(emptyList())
    val freeProperties: kotlinx.coroutines.flow.StateFlow<List<com.rentmanager.app.data.model.PropertyDto>> = _freeProperties.asStateFlow()

    private val _uiState = MutableStateFlow(TenantCardUiState())
    val uiState: StateFlow<TenantCardUiState> = _uiState.asStateFlow()

    /** Правило «отображение сразу»: весь экран целиком в первый кадр.
     *  init ViewModel выполняется до отрисовки:
     *   • повторный вход → полный кеш /card (арендатор + Арендует + история);
     *   • первый вход → кеш списка (имя/телефон/ава), брони доедут за ~50мс
     *     запроса — внутри анимации входа, глазом не видно. */
    private val tenantId: String = savedStateHandle.get<String>("tenantId").orEmpty()

    init {
        val cachedCard = TenantCardCache.takeCardIfMatches(tenantId)
        if (cachedCard != null) {
            _uiState.update { applyCard(it, cachedCard) }
        } else {
            TenantCardCache.takeIfMatches(tenantId)?.let { cached ->
                _uiState.update { it.copy(isLoading = false, tenant = cached) }
            }
        }
        load(tenantId)
        // FCM tenant_profile_changed (арендатор сменил аву/почту/компанию) —
        // перезабираем карточку одним запросом и молча обновляем состояние
        viewModelScope.launch {
            tenantEvents.refreshTick.collect { if (it > 0) load(tenantId) }
        }
    }

    private fun applyCard(state: TenantCardUiState, dto: TenantDto): TenantCardUiState {
        val entries = dto.bookings.orEmpty().mapNotNull { b ->
            runCatching {
                BookingEntryUi(
                    propertyId = b.propertyId.orEmpty(),
                    startDate = LocalDate.parse(b.startDate),
                    endDate = LocalDate.parse(b.endDate),
                    propertyName = b.propertyName?.ifBlank { null } ?: "Без названия",
                    propertyAddress = (b.propertyAddress ?: "").substringBefore(',').trim(),
                    propertyPhoto = b.propertyPhoto
                )
            }.getOrNull()
        }
        val today = LocalDate.now()
        return state.copy(
            isLoading = false,
            tenant = dto,
            currentBooking = entries.lastOrNull { !it.endDate.isBefore(today) },
            pastBookings = entries.filter { it.endDate.isBefore(today) }
                .sortedByDescending { it.startDate }
        )
    }

    fun load(tenantId: String) {
        viewModelScope.launch {
            // Один запрос /tenants/{id}/card: арендатор + брони с объектами
            runCatching { tenantApi.getTenantCard(tenantId).body() }.getOrNull()?.let { dto ->
                TenantCardCache.putCard(dto)
                _uiState.update { applyCard(it, dto) }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadFreeProperties() {
        viewModelScope.launch {
            runCatching { propertyApi.getProperties().body().orEmpty().filter { it.status == "free" } }
                .onSuccess { _freeProperties.value = it }
        }
    }

    /** Прикрепление арендатора к свободному объекту (шит 3005:52495) */
    fun attachToProperty(propertyId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                propertyApi.attachTenant(
                    propertyId,
                    com.rentmanager.app.data.api.AttachTenantRequest(tenantId = tenantId)
                ).isSuccessful
            }.getOrDefault(false)
            if (ok) load(tenantId)
            onDone(ok)
        }
    }

    /** Удаление: false = 409 «нельзя, есть активная аренда» (3005:53081) */
    fun deleteTenant(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val code = runCatching { tenantApi.deleteTenant(tenantId).code() }.getOrDefault(500)
            onResult(code == 200)
        }
    }

    /** «Отменить удаление» (3014:22433) */
    fun restoreTenant(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { tenantApi.restoreTenant(tenantId) }
            load(tenantId)
            onDone()
        }
    }

    fun toggleServiceInfo() = _uiState.update { it.copy(serviceInfoExpanded = !it.serviceInfoExpanded) }
    fun toggleHistory() = _uiState.update { it.copy(historyExpanded = !it.historyExpanded) }
}

/**
 * Значение поля «Номер документа» (правка Вики 2026-09-24, 3677:31337):
 * серия 2+2 через пробел, затем номер — «45 08 7485912545».
 * Хранение не меняется: цифры, первые 4 — серия, остальные — номер.
 * Глаз/маска в новом макете убраны. null — данных нет (показываем «—»).
 */
fun passportDisplay(passportData: String?): String? {
    // Номер документа — свободный текст (серия бывает буквенной):
    // показываем как есть, без разбиения на серию/номер и без фильтра цифр
    return passportData?.trim()?.takeIf { it.isNotEmpty() }
}
