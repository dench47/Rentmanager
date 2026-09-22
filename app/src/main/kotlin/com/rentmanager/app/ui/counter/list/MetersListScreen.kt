package com.rentmanager.app.ui.counter.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.R
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.repository.PropertyRepository
import com.rentmanager.app.ui.counter.readings.meterName
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.InterFontFamily
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetersListUiState(
    val isLoading: Boolean = true,
    val meters: List<MeterDto> = emptyList()
)

@HiltViewModel
class MetersListViewModel @Inject constructor(
    private val repository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetersListUiState())
    val uiState: StateFlow<MetersListUiState> = _uiState.asStateFlow()

    fun load(propertyId: String) {
        viewModelScope.launch {
            _uiState.value = MetersListUiState(isLoading = true)
            val meters = try {
                repository.getMeters(propertyId).body().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            _uiState.value = MetersListUiState(isLoading = false, meters = meters)
        }
    }
}

/**
 * Экран «Редактировать счетчики» (Figma 2755-37699): жёлтый фон (#FFF1CF)
 * со статус-баром и тулбаром, ниже — белая панель со скруглением верхних
 * углов 20dp. Строки счётчиков 372×64 #EFEFEF r20 (зазор 6): тип 15/600,
 * № 13/400 #727272 слева, шеврон вправо 24 в зоне 40×40 (10dp от края).
 * Кнопки «Добавить счетчик» в макете нет.
 */
@Composable
fun MetersListScreen(
    propertyId: String,
    onBack: () -> Unit,
    onOpenMeter: (propertyId: String, meterId: String) -> Unit,
    viewModel: MetersListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(propertyId) { viewModel.load(propertyId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF1CF))
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(27.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Назад — клик по всей зоне «стрелка + название» (глобальное правило)
            Row(
                modifier = Modifier.clickable(onClick = onBack),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_landlord_back),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(Graphite)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Редактировать счетчики",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Graphite
                )
            }
        }

        // Белая панель со скруглением верхних углов
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color.White)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Graphite)
                    }
                }
                uiState.meters.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Счётчики ещё не добавлены",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = GreyText
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.meters, key = { it.id }) { meter ->
                            MeterListRow(meter) { onOpenMeter(propertyId, meter.id) }
                        }
                    }
                }
            }
        }
    }
}

// Строка счётчика (макет 2755-37699): 372×64 #EFEFEF r20; слева тип 15/600
// и № 13/400 #727272 (внутренний отступ 20), справа шеврон 24 в зоне 40×40
@Composable
private fun MeterListRow(meter: MeterDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEFEFEF))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                meterName(meter.type),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Graphite
            )
            Text(
                "№${meter.factoryNumber}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = InterFontFamily,
                color = GreyText
            )
        }
        Box(
            modifier = Modifier
                .padding(end = 10.dp)
                .size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { rotationZ = -90f }
            )
        }
    }
}
