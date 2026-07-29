package com.rentmanager.app.ui.property.detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MeterInfo(
    val id: String,
    val name: String,
    val number: String,
    val currentValue: String,
    val unit: String
)

data class PropertyDetailUiState(
    val propertyName: String = "БЦ Легенда",
    val address: String = "пр. Космонавтов, 1",
    val area: String = "120 м²",
    val rentPrice: String = "25 000 ₽",
    val debtAmount: String = "150 000 ₽",
    val tenantName: String = "ИП Петров В.А.",
    val contractEndDate: String = "24.07.2027",
    val contractNumber: String = "№45 от 14.02.2025",
    val meters: List<MeterInfo> = listOf(
        MeterInfo("1", "Электроэнергия", "7485912545", "456", "кВт"),
        MeterInfo("2", "Холодная вода", "85451546", "9.5", "м³"),
        MeterInfo("3", "Горячая вода", "8446565656", "6.86", "м³"),
        MeterInfo("4", "Отопление", "8446565656", "11", "Гкал")
    ),
    val wifiPassword: String = "ABC12345",
    val houseRules: String = "Без животных, без шума после 23:00",
    val phoneNumber: String = "+7 (999) 123-45-67"
)

class PropertyDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PropertyDetailUiState())
    val uiState: StateFlow<PropertyDetailUiState> = _uiState.asStateFlow()
}