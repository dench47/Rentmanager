package com.rentmanager.app.ui.landlord.createproperty

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.AddressMapPicker
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.DividerLight
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.Headline2MobPlaceholderStyle
import com.rentmanager.app.ui.theme.Headline2MobStyle

// Шаг 3: адрес объекта на карте (Figma 2533:17784).
// Карта на весь экран, пин по центру, нижний шит с полем «Укажите адрес» и кнопкой «Продолжить».
// Вызывается и с экрана «Об объекте» (выбор адреса) — тогда без прогресс-бара.
@Composable
fun CreatePropertyAddressScreen(
    propertyType: String,
    rentType: String,
    onBack: () -> Unit,
    onAddressConfirmed: (String, Double?, Double?) -> Unit,
    onClose: () -> Unit,
    showProgress: Boolean = true,
    screenTitle: String = "Новый объект",
    initialAddress: String = "",
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    // false = автономный режим (экран «Об объекте»): не подмешивать черновик создания
    useDraftFallback: Boolean = true,
    viewModel: CreatePropertyViewModel = hiltViewModel()
) {
    var address by remember(initialAddress) {
        mutableStateOf(
            TextFieldValue(
                if (useDraftFallback) initialAddress.ifBlank { CreateDraftHolder.address } else initialAddress
            )
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Восстановление адреса/метки после возврата с шага 4,
    // либо автопродолжение после диалога «Продолжить создание?»
    LaunchedEffect(Unit) {
        if (!useDraftFallback) {
            // Автономный режим: только восстановление переданной точки, без черновика
            if (initialLatitude != null && initialLongitude != null) {
                viewModel.restoreSelection(initialAddress, initialLatitude, initialLongitude)
            }
        } else if (initialLatitude != null && initialLongitude != null) {
            // Повторный вход с уже выбранной точкой (экран «Об объекте»)
            viewModel.restoreSelection(initialAddress, initialLatitude, initialLongitude)
        } else if (CreateDraftHolder.consumeAutoContinue() &&
            CreateDraftHolder.latitude != null && CreateDraftHolder.longitude != null
        ) {
            viewModel.restoreSelection(
                CreateDraftHolder.address,
                CreateDraftHolder.latitude,
                CreateDraftHolder.longitude
            )
            onAddressConfirmed(
                CreateDraftHolder.address,
                CreateDraftHolder.latitude,
                CreateDraftHolder.longitude
            )
        } else if (CreateDraftHolder.latitude != null && CreateDraftHolder.longitude != null) {
            viewModel.restoreSelection(
                CreateDraftHolder.address,
                CreateDraftHolder.latitude,
                CreateDraftHolder.longitude
            )
        }
    }

    LaunchedEffect(uiState.addressToSet) {
        uiState.addressToSet?.let { text ->
            address = TextFieldValue(text, TextRange(0))
            viewModel.consumeAddressToSet()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val canContinue = address.text.isNotBlank() &&
        uiState.selectedLatitude != null &&
        uiState.addressError == null

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Карта на весь экран (под тулбаром и шитом); в макете 2507-8642 карта
            // начинается СТРОГО под статус-баром — не заходит на часы и иконки
            AddressMapPicker(
                latitude = uiState.selectedLatitude ?: 55.755826,
                longitude = uiState.selectedLongitude ?: 37.617300,
                onLocationSelected = { lat, lon -> viewModel.onMapTapped(lat, lon) },
                markerIconRes = R.drawable.ic_map_pin,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            )

            // Тулбар поверх карты (в макете — полупрозрачная подложка rgba(237,237,237,0.6))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                ScreenToolbar(
                    title = screenTitle,
                    onBack = onBack,
                    showClose = true,
                    onClose = onClose,
                    modifier = Modifier.background(Color(0x99EDEDED))
                )
                if (showProgress) {
                    CreationProgressBar(currentStep = 3)
                }
            }

            // Низ экрана: подсказки + шит одним блоком; imePadding поднимает
            // блок над клавиатурой — поле «Укажите адрес» всегда видно при вводе
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Подсказки адреса над шитом
                if (uiState.addressSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            uiState.addressSuggestions.forEachIndexed { index, suggestion ->
                                Text(
                                    suggestion.displayName,
                                    fontSize = 14.sp,
                                    color = Graphite,
                                    letterSpacing = (-0.4).sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            address = TextFieldValue(" " + suggestion.displayName, TextRange(0))
                                            viewModel.selectAddress(suggestion)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                                if (index < uiState.addressSuggestions.lastIndex) {
                                    HorizontalDivider(color = DividerLight, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }

                // Нижний шит (Figma 2507-8642: высота 165 = 20 + поле 64 + зазор 6 + CTA 55 + 20)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .padding(20.dp)
                        .height(165.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                // Поле «Укажите адрес» (64dp, #EFEFEF, r20, padding 20/10)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .padding(start = 20.dp, end = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            viewModel.suggestAddress(it.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                if (!state.isFocused) viewModel.commitAddress(address.text)
                            },
                        textStyle = Headline2MobStyle,
                        cursorBrush = SolidColor(Graphite),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (address.text.isEmpty()) {
                                    Text("Укажите адрес", style = Headline2MobPlaceholderStyle)
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Кнопка «Продолжить» (55, r100, #212121, белый текст)
                BlackCtaButton(
                    text = "Продолжить",
                    enabled = canContinue
                ) {
                    // Черновик создания трогаем только в потоке создания объекта;
                    // автономный режим («Об объекте») его не перезаписывает
                    if (useDraftFallback) {
                        CreateDraftHolder.address = address.text.trim()
                        CreateDraftHolder.latitude = uiState.selectedLatitude
                        CreateDraftHolder.longitude = uiState.selectedLongitude
                        // Шаг пройден — фиксируем черновик (адрес/координаты переживают перезапуск)
                        CreateDraftHolder.persist()
                    }
                    onAddressConfirmed(
                        address.text.trim(),
                        uiState.selectedLatitude,
                        uiState.selectedLongitude
                    )
                }
                }
            }
        }
    }
}
