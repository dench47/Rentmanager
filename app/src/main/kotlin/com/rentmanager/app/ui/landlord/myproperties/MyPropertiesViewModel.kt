package com.rentmanager.app.ui.landlord.myproperties

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class MyPropertyItem(
    val id: String,
    val name: String,
    val address: String,
    val schedule: List<String> = listOf(
        "fullness", "fullness", "fullness", "fullness", "fullness",
        "expired",
        "free", "free",
        "fullness", "fullness", "fullness", "fullness"
    ),
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue // 1..12
)

/**
 * Данные из Figma (node-id=2183:9531):
 *   БЦ Пять морей — пер. Серебряного бора
 *   БЦ Легенда — пр. Космонавтов, 1
 *   Квартира 12 — ул. Ленина
 *
 * Шахматка: 12 месяцев (СЕН–АВГ), ячейки 44×39, r=4, gap=4:
 *   занято — fill #CFDECB / stroke #66A256
 *   просрочено — stroke #FF4249 (ФЕВ)
 *   свободно — fill #EFEFEF / stroke #727272 (МАР, АПР)
 */
private val figmaProperties = listOf(
    MyPropertyItem("1", "БЦ Пять морей", "пер. Серебряного бора"),
    MyPropertyItem("2", "БЦ Легенда", "пр. Космонавтов, 1"),
    MyPropertyItem("3", "Квартира 12", "ул. Ленина")
)

enum class ViewMode { MONTHS, DAYS }

class MyPropertiesViewModel : ViewModel() {
    private val _properties = MutableStateFlow(figmaProperties.toMutableList())
    val properties: StateFlow<List<MyPropertyItem>> = _properties.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.MONTHS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    fun addProperty(name: String, address: String) {
        val newId = "${_properties.value.size + 1}"
        val list = _properties.value.toMutableList()
        list.add(0, MyPropertyItem(newId, name, address))
        _properties.value = list
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun selectPeriod(propertyId: String, year: Int, month: Int) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(year = year, month = month) else it
        }.toMutableList()
    }
}
