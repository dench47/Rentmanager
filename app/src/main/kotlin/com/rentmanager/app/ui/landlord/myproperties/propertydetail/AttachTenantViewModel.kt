package com.rentmanager.app.ui.landlord.myproperties.propertydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AttachTenantRequest
import com.rentmanager.app.data.api.BookingApi
import com.rentmanager.app.data.api.CreateBookingRequest
import com.rentmanager.app.data.api.PropertyApi
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.local.BookingChangeEvents
import com.rentmanager.app.data.model.TenantDto
import com.rentmanager.app.util.PhoneUtils
import com.rentmanager.app.util.DD_MM_YYYY
import com.rentmanager.app.util.parseAnyDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AttachTenantUiState(
    val isAttaching: Boolean = false,
    /** Сбой прикрепления — окно «Нет связи» (Figma 2935:40038) */
    val failed: Boolean = false,
    /** Период пересекается с существующей арендой — текст для окна */
    val conflictMessage: String? = null,
    /** Объект для шапки экрана (фото, название, адрес) */
    val property: com.rentmanager.app.data.model.PropertyDto? = null
)

@HiltViewModel
class AttachTenantViewModel @Inject constructor(
    private val propertyApi: PropertyApi,
    private val tenantApi: TenantApi,
    private val bookingApi: BookingApi,
    private val bookingChangeEvents: BookingChangeEvents
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttachTenantUiState())
    val uiState: StateFlow<AttachTenantUiState> = _uiState.asStateFlow()

    fun load(propertyId: String) {
        viewModelScope.launch {
            try {
                val p = propertyApi.getProperties().body()?.firstOrNull { it.id == propertyId }
                _uiState.update { it.copy(property = p) }
            } catch (_: Exception) { }
        }
    }

    fun clearFailure() {
        _uiState.update { it.copy(failed = false) }
    }

    fun clearConflict() {
        _uiState.update { it.copy(conflictMessage = null) }
    }

    /** Пресет из карточки арендатора («Прикрепить к объекту»): ФИО/телефон
     *  подставляются в поля, сохранение идёт через attachExisting */
    private val _presetTenant = MutableStateFlow<TenantDto?>(null)
    val presetTenant: StateFlow<TenantDto?> = _presetTenant.asStateFlow()

    fun loadPresetTenant(id: String) {
        viewModelScope.launch {
            runCatching { tenantApi.getTenantCard(id).body() }
                .onSuccess { t -> if (t != null) _presetTenant.value = t }
        }
    }

    /** Уже добавленные арендаторы — источник «Из списка арендаторов» (3429:66563) */
    private val _tenants = MutableStateFlow<List<TenantDto>>(emptyList())
    val tenants: StateFlow<List<TenantDto>> = _tenants.asStateFlow()

    fun loadTenants() {
        viewModelScope.launch {
            runCatching { tenantApi.getTenants().body().orEmpty() }
                .onSuccess { list -> _tenants.value = list }
        }
    }

    /**
     * Прикрепление УЖЕ СУЩЕСТВУЮЩЕГО арендатора (источник «Из списка арендаторов»):
     * новый Tenant НЕ создаём — привязываем выбранного к объекту, пишем его ФИО/
     * телефон в карточку объекта и закрываем период бронью (как в ручном флоу).
     */
    fun attachExisting(
        propertyId: String,
        tenant: TenantDto,
        start: LocalDate,
        end: LocalDate,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAttaching = true, failed = false) }
            try {
                findOverlap(propertyId, start, end)?.let { message ->
                    _uiState.update { it.copy(isAttaching = false, conflictMessage = message) }
                    return@launch
                }
                val attachResp = propertyApi.attachTenant(
                    propertyId, AttachTenantRequest(tenantId = tenant.id)
                )
                if (!attachResp.isSuccessful) {
                    _uiState.update { it.copy(isAttaching = false, failed = true) }
                    return@launch
                }
                bookingApi.createBooking(
                    propertyId, CreateBookingRequest(start.toString(), end.toString(), tenantId = tenant.id)
                )
                // Та же мгновенная перекраска шахматки, что и в ручном флоу
                bookingChangeEvents.emitAdded(propertyId, start, end, tenant.id)
                _uiState.update { it.copy(isAttaching = false) }
                onSuccess()
            } catch (_: Exception) {
                _uiState.update { it.copy(isAttaching = false, failed = true) }
            }
        }
    }

    /**
     * Пересечение периода с ЛЮБОЙ бронью объекта (не только своего арендатора).
     * Стык «выезд/заезд» в один день (конец = началу) пересечением не считается:
     * Возвращает текст конфликта для окна или null, если период свободен.
     */
    private suspend fun findOverlap(propertyId: String, start: LocalDate, end: LocalDate): String? {
        val existing = runCatching { bookingApi.getBookings(propertyId).body().orEmpty() }
            .getOrDefault(emptyList())
        val clash = existing.firstOrNull { b ->
            val s = parseAnyDate(b.startDate) ?: return@firstOrNull false
            val e = parseAnyDate(b.endDate) ?: return@firstOrNull false
            !end.isBefore(s) && !start.isAfter(e) && start != e && end != s
        } ?: return null
        val s = parseAnyDate(clash.startDate) ?: return null
        val e = parseAnyDate(clash.endDate) ?: return null
        return "Этот период уже занят: " + s.format(DD_MM_YYYY) + " - " + e.format(DD_MM_YYYY)
    }

    /**
     * Прикрепление вручную (канвас «Добавить арендатора»): создать Tenant,
     * привязать к объекту, записать ФИО/телефон в карточку (блок «Арендатор
     * и договор» работает по tenantInfo) и закрыть период бронью — шахматка
     * красится, «Срок аренды» считается из брони.
     */
    fun attach(
        propertyId: String,
        name: String,
        phone: String,
        start: LocalDate,
        end: LocalDate,
        onSuccess: () -> Unit
    ) {
        val normalizedPhone = PhoneUtils.normalize(phone) ?: phone
        viewModelScope.launch {
            _uiState.update { it.copy(isAttaching = true, failed = false) }
            try {
                findOverlap(propertyId, start, end)?.let { message ->
                    _uiState.update { it.copy(isAttaching = false, conflictMessage = message) }
                    return@launch
                }
                // Один номер = один арендатор: если он уже есть у владельца,
                // привязываем существующего и дубль не создаём (сервер делает так же)
                val found = runCatching { tenantApi.getTenants().body().orEmpty() }
                    .getOrDefault(emptyList())
                    .firstOrNull { PhoneUtils.normalize(it.phone) == normalizedPhone }
                val tenant: TenantDto
                if (found != null) {
                    tenant = found
                } else {
                    val tenantResp = tenantApi.createTenant(
                        TenantDto(id = "", fullName = name, phone = normalizedPhone)
                    )
                    if (!tenantResp.isSuccessful || tenantResp.body() == null) {
                        _uiState.update { it.copy(isAttaching = false, failed = true) }
                        return@launch
                    }
                    tenant = tenantResp.body()!!
                }
                val attachResp = propertyApi.attachTenant(
                    propertyId, AttachTenantRequest(tenantId = tenant.id)
                )
                if (!attachResp.isSuccessful) {
                    _uiState.update { it.copy(isAttaching = false, failed = true) }
                    return@launch
                }
                // Карточка и бронь периода: данные объекта могли не грузиться —
                // читаем текущие и обновляем нужные поля
                bookingApi.createBooking(
                    propertyId, CreateBookingRequest(start.toString(), end.toString(), tenantId = tenant.id)
                )
                // Шахматка «Моя недвижимость» перекрасит период сразу, не ожидая
                // ON_RESUME-refresh: иначе размеченный период оставался серым
                bookingChangeEvents.emitAdded(propertyId, start, end, tenant.id)
                _uiState.update { it.copy(isAttaching = false) }
                onSuccess()
            } catch (_: Exception) {
                _uiState.update { it.copy(isAttaching = false, failed = true) }
            }
        }
    }

    /**
     * Открепление (Figma 2936:41556): Tenant удалять нельзя (сносится вся
     * история) — связь с объектом снимает серверный detach_tenant: он же
     * освобождает шахматку с первого числа текущего месяца.
     */
    fun detach(propertyId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAttaching = true, failed = false) }
            try {
                val resp = propertyApi.detachTenant(propertyId)
                _uiState.update { it.copy(isAttaching = false, failed = !resp.isSuccessful) }
                if (resp.isSuccessful) onSuccess()
            } catch (_: Exception) {
                _uiState.update { it.copy(isAttaching = false, failed = true) }
            }
        }
    }
}

