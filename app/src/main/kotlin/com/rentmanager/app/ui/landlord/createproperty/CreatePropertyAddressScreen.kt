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
@Composable
fun CreatePropertyAddressScreen(
    propertyType: String,
    rentType: String,
    onBack: () -> Unit,
    onAddressConfirmed: (String, Double?, Double?) -> Unit,
    viewModel: CreatePropertyViewModel = hiltViewModel()
) {
    var address by remember { mutableStateOf(TextFieldValue("")) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
            // Карта на весь экран (под тулбаром и шитом)
            AddressMapPicker(
                latitude = uiState.selectedLatitude ?: 55.755826,
                longitude = uiState.selectedLongitude ?: 37.617300,
                onLocationSelected = { lat, lon -> viewModel.onMapTapped(lat, lon) },
                markerIconRes = R.drawable.ic_map_pin,
                modifier = Modifier.fillMaxSize()
            )

            // Тулбар поверх карты (в макете — полупрозрачная подложка rgba(237,237,237,0.6))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                ScreenToolbar(
                    title = "Новый объект",
                    onBack = onBack,
                    showClose = true,
                    modifier = Modifier.background(Color(0x99EDEDED))
                )
                CreationProgressBar(currentStep = 3)
            }

            // Подсказки адреса над шитом
            if (uiState.addressSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = paddingValues.calculateBottomPadding() + 165.dp + 8.dp,
                            start = 20.dp,
                            end = 20.dp
                        ),
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

            // Нижний шит (Figma: высота 165, радиус 30 сверху, padding 20, поле 55 + кнопка 55)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .background(Color.White, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .padding(20.dp)
                    .height(165.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Поле «Укажите адрес» (55, #EFEFEF, r20, padding 20/10)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
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
