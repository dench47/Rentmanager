package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@Immutable
data class MyPropertyItem(
    val id: String,
    val name: String,
    val address: String,
    val photoUrl: String? = null,
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

@HiltViewModel
class MyPropertiesViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository
) : ViewModel() {
    private val _properties = MutableStateFlow(figmaProperties.toMutableList())
    val properties: StateFlow<List<MyPropertyItem>> = _properties.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.MONTHS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val resp = propertyRepository.getProperties()
                if (resp.isSuccessful) {
                    val apiItems = resp.body()!!.map { it.toMyPropertyItem() }
                    _properties.value = (figmaProperties + apiItems).toMutableList()
                }
            } catch (_: Exception) { }
        }
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

private fun PropertyDto.toMyPropertyItem(): MyPropertyItem = MyPropertyItem(
    id = id,
    name = name,
    address = address,
    photoUrl = photos?.firstOrNull()?.url
)
