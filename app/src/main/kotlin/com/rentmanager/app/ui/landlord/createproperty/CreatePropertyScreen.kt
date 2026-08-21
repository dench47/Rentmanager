package com.rentmanager.app.ui.landlord.createproperty

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.InterFontFamily

// --- Цвета из макета (Figma) ---
private val ScreenBackground = Color(0xFFF5F5F5)   // colors/backgrounds/light
private val CardBackground = Color(0xFFEFEFEF)     // Grey/Icon
private val Graphite = Color(0xFF212121)           // Graphite/Icon
private val GreyText = Color(0xFF727272)           // Grey/Text
private val ErrorRed = Color(0xFFFF4249)           // Red/Text

private val CardShape = RoundedCornerShape(20.dp)
private val PillShape = RoundedCornerShape(100.dp)

private val ToolbarTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    letterSpacing = (-0.3).sp,
    color = Graphite
)
private val FieldTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = (-0.4).sp,
    color = Graphite
)
private val FieldLabelStyle = FieldTextStyle.copy(color = GreyText)
private val FieldLabelErrorStyle = FieldTextStyle.copy(color = ErrorRed)
private val CardSubtitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = (-0.4).sp,
    color = GreyText
)
private val ButtonTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = (-0.4).sp
)

// Опции дропдаунов (локально, в бэкенд не уходят)
private val RoomsOptions = listOf("Студия", "1", "2", "3", "4", "5+")
private val SleepingOptions = listOf("1", "2", "3", "4", "5", "6+")
private val FloorOptions = (1..30).map { it.toString() }
private val FloorsInHouseOptions = (1..30).map { it.toString() }
@Composable
fun CreatePropertyScreen(
    propertyId: String? = null,
    onBack: () -> Unit,
    onCreated: () -> Unit = {},
    onPaymentSchedule: (String) -> Unit = {},
    viewModel: CreatePropertyViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(TextFieldValue("")) }
    var area by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Новые поля макета (только на экране, в бэкенд не сохраняются)
    var rooms by remember { mutableStateOf<String?>(null) }
    var sleepingPlaces by remember { mutableStateOf<String?>(null) }
    var floor by remember { mutableStateOf<String?>(null) }
    var floorsInHouse by remember { mutableStateOf<String?>(null) }

    // Photos
    var photoUris by remember { mutableStateOf(listOf<String>()) }
    var showPhotoMenuIndex by remember { mutableIntStateOf(-1) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoUris = photoUris + uris.map { it.toString() }
        }
    }

    var descriptionExpanded by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.addressToSet) {
        uiState.addressToSet?.let { text ->
            address = TextFieldValue(text, TextRange(0))
            viewModel.consumeAddressToSet()
        }
    }

    val canCreate = name.isNotBlank() &&
        address.text.isNotBlank() &&
        uiState.addressError == null &&
        !uiState.isCreating

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .padding(paddingValues)
        ) {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onBack() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = "Назад",
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Новый объект", style = ToolbarTitleStyle)
                }
            }

            // Progress indicator (4 шага)
            CreationProgressBar()

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Фото
                if (photoUris.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(183.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(CardBackground)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_add_photo),
                                contentDescription = "Добавить фото",
                                modifier = Modifier.size(50.dp)
                            )
                            Text("Добавить фото", style = FieldLabelStyle)
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photoUris.size) { index ->
                            @OptIn(ExperimentalFoundationApi::class)
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFD3D3D3))
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = { showPhotoMenuIndex = index }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = photoUris[index],
                                    contentDescription = "Фото ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (index == 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color(0x99000000)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Основное",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showPhotoMenuIndex == index,
                                    onDismissRequest = { showPhotoMenuIndex = -1 }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Сделать основным") },
                                        onClick = {
                                            if (index > 0) {
                                                photoUris = listOf(photoUris[index]) + photoUris.filterIndexed { i, _ -> i != index }
                                            }
                                            showPhotoMenuIndex = -1
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Удалить", color = ErrorRed) },
                                        onClick = {
                                            photoUris = photoUris.filterIndexed { i, _ -> i != index }
                                            showPhotoMenuIndex = -1
                                        }
                                    )
                                }
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .clickable { galleryLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_add_photo),
                                    contentDescription = "Добавить фото",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Название
                InfoTextField(value = name, onValueChange = { name = it }, label = "Название")

                // 3. Адрес
                AddressInfoField(
                    value = address,
                    onValueChange = {
                        address = it
                        viewModel.suggestAddress(it.text)
                    },
                    label = "Адрес",
                    onFocusChanged = { focused ->
                        if (!focused) {
                            viewModel.commitAddress(address.text)
                        }
                    },
                    onDone = { viewModel.commitAddress(address.text) },
                    onClear = {
                        address = TextFieldValue("")
                        viewModel.clearAddressSelection()
                    },
                    isError = uiState.addressError != null
                )

                if (uiState.addressError != null) {
                    Text(
                        uiState.addressError!!,
                        style = CardSubtitleStyle.copy(color = ErrorRed, fontSize = 12.sp)
                    )
                }

                // Подсказки адреса (Photon / OpenStreetMap)
                if (uiState.addressSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            uiState.addressSuggestions.forEachIndexed { index, suggestion ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            address = TextFieldValue(" " + suggestion.displayName, TextRange(0))
                                            viewModel.selectAddress(suggestion)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        suggestion.displayName,
                                        fontSize = 14.sp,
                                        color = Graphite,
                                        letterSpacing = (-0.4).sp
                                    )
                                }
                                if (index < uiState.addressSuggestions.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
                // 4. Количество комнат
                InfoDropdown(
                    selected = rooms,
                    options = RoomsOptions,
                    onSelect = { rooms = it },
                    label = "Количество комнат"
                )

                // 5. Сетка: Площадь / Спальные места
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = "Площадь, м2",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    InfoDropdown(
                        selected = sleepingPlaces,
                        options = SleepingOptions,
                        onSelect = { sleepingPlaces = it },
                        label = "Спальные места",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 6. Сетка: Этаж / Этажей в доме
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoDropdown(
                        selected = floor,
                        options = FloorOptions,
                        onSelect = { floor = it },
                        label = "Этаж",
                        modifier = Modifier.weight(1f)
                    )
                    InfoDropdown(
                        selected = floorsInHouse,
                        options = FloorsInHouseOptions,
                        onSelect = { floorsInHouse = it },
                        label = "Этажей в доме",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 7. Описание объявления (аккордеон)
                DescriptionCard(
                    value = description,
                    onValueChange = { description = it },
                    expanded = descriptionExpanded,
                    onToggle = { descriptionExpanded = !descriptionExpanded }
                )

                // 8. Стоимость за сутки
                InfoTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = "Стоимость за сутки, ₽",
                    keyboardType = KeyboardType.Decimal
                )

                Spacer(Modifier.height(8.dp))
            }
            // Bottom buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlineButton(
                    text = "Создать объект",
                    enabled = canCreate,
                    onClick = {
                        viewModel.createProperty(
                            name = name,
                            address = address.text,
                            area = area,
                            rentAmount = price,
                            description = description,
                            photoUris = photoUris,
                            latitude = uiState.selectedLatitude,
                            longitude = uiState.selectedLongitude,
                            onSuccess = onCreated
                        )
                    }
                )
                FilledButton(
                    text = "Создать и опубликовать объявление",
                    enabled = canCreate,
                    onClick = {
                        viewModel.createProperty(
                            name = name,
                            address = address.text,
                            area = area,
                            rentAmount = price,
                            description = description,
                            photoUris = photoUris,
                            latitude = uiState.selectedLatitude,
                            longitude = uiState.selectedLongitude,
                            onSuccess = onCreated
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
// 4 плоских сегмента-индикатора (Progress indicator / Width 4)
@Composable
private fun CreationProgressBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Graphite.copy(alpha = if (index == 3) 0.9f else 1f),
                            RoundedCornerShape(24.dp)
                        )
                )
            }
        }
    }
}

// Базовая карточка card_inf: фон #EFEFEF, radius 20, высота 64, padding 0/10
@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(if (isError) Modifier.border(1.dp, ErrorRed, CardShape) else Modifier),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            content = content
        )
    }
}

// Текстовое поле в карточке card_inf (лейбл виден, когда поле пустое)
@Composable
private fun InfoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = FieldTextStyle,
            cursorBrush = SolidColor(Graphite),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(label, style = FieldLabelStyle)
                    }
                    innerTextField()
                }
            }
        )
    }
}

// Дропдаун в карточке card_inf (лейбл виден, пока ничего не выбрано)
@Composable
private fun InfoDropdown(
    selected: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        InfoCard(onClick = { expanded = true }) {
            Text(
                text = selected ?: label,
                style = if (selected == null) FieldLabelStyle else FieldTextStyle,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Graphite
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = FieldTextStyle) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
// Поле адреса с подсказками, кнопкой очистки и красной рамкой только при ошибке
@Composable
private fun AddressInfoField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    onFocusChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
    onClear: () -> Unit,
    isError: Boolean
) {
    InfoCard(isError = isError) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            textStyle = FieldTextStyle,
            cursorBrush = SolidColor(Graphite),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.text.isEmpty()) {
                        Text(
                            label,
                            style = if (isError) FieldLabelErrorStyle else FieldLabelStyle
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (value.text.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Очистить",
                modifier = Modifier
                    .clickable { onClear() }
                    .size(20.dp),
                tint = GreyText
            )
        }
    }
}

// Аккордеон «Описание объявления» с подзаголовком
@Composable
private fun DescriptionCard(
    value: String,
    onValueChange: (String) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { onToggle() }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Описание объявления", style = FieldTextStyle)
                    Text("Эта информация будет видна в объявлении", style = CardSubtitleStyle)
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Graphite
                )
            }
            AnimatedVisibility(visible = expanded) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 12.dp)
                        .heightIn(min = 80.dp),
                    textStyle = FieldTextStyle.copy(fontWeight = FontWeight.Normal),
                    cursorBrush = SolidColor(Graphite),
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text("Описание", style = FieldLabelStyle.copy(fontWeight = FontWeight.Normal))
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

// Кнопка с обводкой (вторичная)
@Composable
private fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val color = if (enabled) Graphite else GreyText
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .border(1.dp, color, PillShape)
            .clip(PillShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = ButtonTextStyle.copy(color = color))
    }
}

// Залитая кнопка (основная)
@Composable
private fun FilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(PillShape)
            .background(if (enabled) Graphite else Color(0xFFD3D3D3))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = ButtonTextStyle.copy(color = Color.White))
    }
}
