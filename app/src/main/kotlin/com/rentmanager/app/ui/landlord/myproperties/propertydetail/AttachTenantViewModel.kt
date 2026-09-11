package com.rentmanager.app.ui.landlord.myproperties.propertydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.AttachTenantRequest
import com.rentmanager.app.data.api.BookingApi
import com.rentmanager.app.data.api.CreateBookingRequest
import com.rentmanager.app.data.api.PropertyApi
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.model.TenantDto
import com.rentmanager.app.util.PhoneUtils
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
    /** Объект для шапки экрана (фото, название, адрес) */
    val property: com.rentmanager.app.data.model.PropertyDto? = null
)

@HiltViewModel
class AttachTenantViewModel @Inject constructor(
    private val propertyApi: PropertyApi,
    private val tenantApi: TenantApi,
    private val bookingApi: BookingApi
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
                val tenantResp = tenantApi.createTenant(
                    TenantDto(id = "", fullName = name, phone = normalizedPhone)
                )
                if (!tenantResp.isSuccessful || tenantResp.body() == null) {
                    _uiState.update { it.copy(isAttaching = false, failed = true) }
                    return@launch
                }
                val attachResp = propertyApi.attachTenant(
                    propertyId, AttachTenantRequest(tenantId = tenantResp.body()!!.id)
                )
                if (!attachResp.isSuccessful) {
                    _uiState.update { it.copy(isAttaching = false, failed = true) }
                    return@launch
                }
                // Карточка и бронь периода: данные объекта могли не грузиться —
                // читаем текущие и обновляем нужные поля
                val propResp = propertyApi.getProperties()
                val current = propResp.body()?.firstOrNull { it.id == propertyId }
                if (current != null) {
                    propertyApi.updateProperty(
                        propertyId,
                        current.copy(tenantInfo = name, phone = normalizedPhone)
                    )
                }
                bookingApi.createBooking(
                    propertyId, CreateBookingRequest(start.toString(), end.toString())
                )
                _uiState.update { it.copy(isAttaching = false) }
                onSuccess()
            } catch (_: Exception) {
                _uiState.update { it.copy(isAttaching = false, failed = true) }
            }
        }
    }

    /**
     * Открепление (Figma 2936:41556):Tenant удалять нельзя (сносится вся
     * история) — снимаем связь с объектом: tenant_id/status/tenant_info/phone,
     * период снимается удалением будущих броней.
     */
    fun detach(propertyId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAttaching = true, failed = false) }
            try {
                val propResp = propertyApi.getProperties()
                val current = propResp.body()?.firstOrNull { it.id == propertyId }
                if (current != null) {
                    propertyApi.updateProperty(
                        propertyId,
                        current.copy(
                            tenantId = null,
                            tenantInfo = "",
                            phone = "",
                            status = "free"
                        )
                    )
                }
                // Будущие брони периода — снять (прошедшие остаются историей)
                val today = LocalDate.now()
                bookingApi.getBookings(propertyId).body().orEmpty()
                    .filter { b ->
                        val e = runCatching { LocalDate.parse(b.endDate) }.getOrNull()
                        e != null && !e.isBefore(today)
                    }
                    .forEach { b -> b.id?.let { runCatching { bookingApi.deleteBooking(propertyId, it) } } }
                _uiState.update { it.copy(isAttaching = false) }
                onSuccess()
            } catch (_: Exception) {
                _uiState.update { it.copy(isAttaching = false, failed = true) }
            }
        }
    }
}
