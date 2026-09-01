package com.rentmanager.app.ui.counter.list

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
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.R
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.repository.PropertyRepository
import com.rentmanager.app.ui.landlord.createproperty.ScreenToolbar
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
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
 * Экран «Редактировать счетчики» (Figma 2755-37699): список счётчиков объекта,
 * каждая строка (имя + № + шеврон) открывает экран редактирования счётчика.
 */
@Composable
fun MetersListScreen(
    propertyId: String,
    onBack: () -> Unit,
    onOpenMeter: (propertyId: String, meterId: String) -> Unit,
    onAddMeter: (propertyId: String) -> Unit,
    viewModel: MetersListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(propertyId) { viewModel.load(propertyId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        ScreenToolbar(title = "Редактировать счетчики", onBack = onBack)
        Spacer(Modifier.height(20.dp))

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Graphite)
                }
            }
            uiState.meters.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Счётчики ещё не добавлены", style = Headline2MobStyle.copy(color = GreyText))
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.meters, key = { it.id }) { meter ->
                        MeterListRow(meter) { onOpenMeter(propertyId, meter.id) }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        // Строка «Добавить счетчик» — как в макете списка
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onAddMeter(propertyId) }
                                .padding(vertical = 18.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Добавить счетчик",
                                style = Headline2MobStyle.copy(color = Graphite)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeterListRow(meter: MeterDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEFEFEF))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                com.rentmanager.app.ui.counter.readings.meterName(meter.type),
                style = Headline2MobStyle
            )
            Text(
                "№${meter.factoryNumber}",
                style = com.rentmanager.app.ui.theme.CardSubtitleStyle.copy(color = GreyText)
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_card_chevron),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}
