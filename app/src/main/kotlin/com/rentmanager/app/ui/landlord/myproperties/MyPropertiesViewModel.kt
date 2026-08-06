package com.rentmanager.app.ui.landlord.myproperties

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MyPropertyItem(
    val id: String,
    val name: String,
    val address: String,
    val schedule: List<String> = listOf(
        "fullness", "fullness", "expired", "fullness",
        "free", "free", "free", "free"
    )
)

/**
 * Данные из Figma (node-id=163:4918):
 *   «БЦ Легенда» — пр. Космонавтов, 1
 *   Индикаторы месяцев: fullness × 3, expired × 1, free × 4
 *   Иконки: ic_indicator_fullness.svg, ic_indicator_expired.svg, ic_indicator_free.svg
 *   Аватар: mock_avatar_legend.png (imageRef: 58c3452f...)
 *   Кнопка «Дать доступ»: borderRadius 100px, обводка #CFCFCF, blur(3.65px), Inter Medium 13sp
 */
private val figmaProperty = MyPropertyItem(
    id = "1",
    name = "БЦ Легенда",
    address = "пр. Космонавтов, 1",
    schedule = listOf("fullness", "fullness", "expired", "fullness", "free", "free", "free", "free")
)

enum class ViewMode { MONTHS, DAYS }

class MyPropertiesViewModel : ViewModel() {
    private val _properties = MutableStateFlow(mutableListOf(figmaProperty))
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
}