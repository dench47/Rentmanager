package com.rentmanager.app.ui.property.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PropertyDetailUiState(
    val isLoading: Boolean = true,
    val propertyName: String = "",
    val address: String = "",
    val area: String = "",
    val rentPrice: String = "",
    val photos: List<String> = emptyList(),
    val serviceInfo: String = "",
    val phone: String = "",
    val wifiPassword: String = "",
    val houseRules: String = "",
    val tenantName: String = "",
    val tenantPhone: String = "",
    val tenantCompany: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class PropertyDetailViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val tenantApi: TenantApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyDetailUiState())
    val uiState: StateFlow<PropertyDetailUiState> = _uiState.asStateFlow()

    fun load(propertyId: String) {
        viewModelScope.launch {
            _uiState.value = PropertyDetailUiState(isLoading = true)
            try {
                val resp = propertyRepository.getProperty(propertyId)
                if (resp.isSuccessful) {
                    val p = resp.body()!!
                    var tenantName = ""
                    var tenantPhone = ""
                    var tenantCompany = ""
                    p.tenantId?.let { tid ->
                        try {
                            val tResp = tenantApi.getTenant(tid)
                            if (tResp.isSuccessful) {
                                val t = tResp.body()
                                tenantName = t?.fullName ?: ""
                                tenantPhone = t?.phone ?: ""
                                tenantCompany = t?.companyName ?: ""
                            }
                        } catch (_: Exception) { }
                    }
                    _uiState.value = PropertyDetailUiState(
                        isLoading = false,
                        propertyName = p.name,
                        address = p.address,
                        area = formatArea(p.area),
                        rentPrice = formatPrice(p.rentAmount),
                        photos = p.photos?.map { it.url } ?: emptyList(),
                        serviceInfo = p.serviceInfo ?: "",
                        phone = p.phone ?: "",
                        wifiPassword = p.wifiPassword ?: "",
                        houseRules = p.houseRules ?: "",
                        tenantName = tenantName,
                        tenantPhone = tenantPhone,
                        tenantCompany = tenantCompany
                    )
                } else {
                    _uiState.value = PropertyDetailUiState(isLoading = false, errorMessage = "Объект не найден")
                }
            } catch (e: Exception) {
                _uiState.value = PropertyDetailUiState(isLoading = false, errorMessage = e.message ?: "Ошибка")
            }
        }
    }

    private fun formatArea(area: Double?): String {
        if (area == null) return ""
        return if (area == area.toLong().toDouble()) "${area.toLong()} м²" else "$area м²"
    }

    private fun formatPrice(amount: Double?): String {
        if (amount == null) return ""
        val whole = amount.toLong()
        val withSpaces = whole.toString().reversed().chunked(3).joinToString(" ").reversed()
        return "$withSpaces ₽"
    }
}
