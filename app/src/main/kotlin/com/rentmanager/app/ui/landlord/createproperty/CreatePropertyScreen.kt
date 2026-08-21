package com.rentmanager.app.ui.landlord.createproperty

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.AddressMapPicker
import com.rentmanager.app.ui.components.BlackButtonWithIcon
import com.rentmanager.app.ui.theme.ButtonTextStyle
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardShape
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.DividerLight
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.FieldLabelErrorStyle
import com.rentmanager.app.ui.theme.FieldLabelStyle
import com.rentmanager.app.ui.theme.FieldTextStyle
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.InactiveGray
import com.rentmanager.app.ui.theme.PillShape
import com.rentmanager.app.ui.theme.ScreenBackground
import com.rentmanager.app.ui.theme.ToolbarTitleStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Опции дропдаунов (локально, в бэкенд не уходят)
private val RoomsOptions = listOf("Студия", "1", "2", "3", "4", "5+")
private val SleepingOptions = listOf("1", "2", "3", "4", "5", "6+")
private val FloorOptions = listOf("Подвал", "Цоколь", "-2", "-1") + (1..100).map { it.toString() }
private val FloorsInHouseOptions = (1..100).map { it.toString() }
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

    // Данные аккордеонов «Информация об объекте» / «Служебная информация»
    var phoneNumber by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var rulesText by remember { mutableStateOf("") }
    var serviceInfo by remember { mutableStateOf("") }
    var tenantInfoExpanded by remember { mutableStateOf(false) }
    var serviceInfoExpanded by remember { mutableStateOf(false) }
    var addressFocused by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var columnTop by remember { mutableStateOf(0f) }
    var viewportHeight by remember { mutableStateOf(0f) }
    var descriptionTop by remember { mutableStateOf(0f) }
    var descriptionHeight by remember { mutableStateOf(0f) }
    var tenantInfoTop by remember { mutableStateOf(0f) }
    var tenantInfoHeight by remember { mutableStateOf(0f) }
    var serviceInfoTop by remember { mutableStateOf(0f) }
    var serviceInfoHeight by remember { mutableStateOf(0f) }

    // Photos
    var photoUris by remember { mutableStateOf(listOf<String>()) }
    var showPhotoMenuIndex by remember { mutableIntStateOf(-1) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoUris = uris.map { it.toString() } + photoUris
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
                    .onGloballyPositioned { coords -> columnTop = coords.localToRoot(Offset.Zero).y; viewportHeight = coords.size.height.toFloat() }
                    .verticalScroll(scrollState)
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
                            Box(
                                modifier = Modifier
                                    .width(183.dp)
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(InactiveGray)
                                    .border(1.dp, Color.Black, RoundedCornerShape(30.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = photoUris[index],
                                    contentDescription = "Фото ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(CardBackground)
                                        .clickable { showPhotoMenuIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Редактировать фото",
                                        modifier = Modifier.size(20.dp),
                                        tint = Graphite
                                    )
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
                        addressFocused = focused
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
                                    HorizontalDivider(color = DividerLight, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
                // Карта — появляется при фокусе на адресе, когда координаты уже выбраны
                if (addressFocused && uiState.selectedLatitude != null && uiState.selectedLongitude != null) {
                    AddressMapPicker(
                        latitude = uiState.selectedLatitude!!,
                        longitude = uiState.selectedLongitude!!,
                        onLocationSelected = { lat, lon -> viewModel.onMapTapped(lat, lon) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(top = 12.dp)
                    )
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
                Box(Modifier.onGloballyPositioned { coords -> descriptionTop = coords.localToRoot(Offset.Zero).y; descriptionHeight = coords.size.height.toFloat() }) {
                    DescriptionCard(
                        value = description,
                        onValueChange = { description = it },
                        expanded = descriptionExpanded,
                        onToggle = {
                            val willExpand = !descriptionExpanded
                            descriptionExpanded = willExpand
                            if (willExpand) scope.launch {
                                delay(250)
                                val overflow = (descriptionTop + descriptionHeight) - (columnTop + viewportHeight)
                                if (overflow > 0f) {
                                    scrollState.animateScrollTo((scrollState.value + overflow).toInt(), animationSpec = tween(300))
                                }
                            }
                        }
                    )
                }

                // 8. Стоимость за сутки
                InfoTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = "Стоимость за сутки, ₽",
                    keyboardType = KeyboardType.Decimal
                )

                // 9. График платежей и реквизиты
                BlackButtonWithIcon(
                    text = "График платежей и реквизиты",
                    iconRes = R.drawable.ic_calendar_edit,
                    onClick = {
                        if (propertyId != null) {
                            onPaymentSchedule(propertyId)
                        } else if (canCreate) {
                            viewModel.createProperty(
                                name = name,
                                address = address.text,
                                area = area,
                                rentAmount = price,
                                description = description,
                                photoUris = photoUris,
                                serviceInfo = serviceInfo,
                                phone = phoneNumber,
                                wifiPassword = wifiPassword,
                                houseRules = rulesText,
                                latitude = uiState.selectedLatitude,
                                longitude = uiState.selectedLongitude,
                                onSuccess = { newId -> onPaymentSchedule(newId) }
                            )
                        }
                    }
                )

                // 10. Информация об объекте
                Box(Modifier.onGloballyPositioned { coords -> tenantInfoTop = coords.localToRoot(Offset.Zero).y; tenantInfoHeight = coords.size.height.toFloat() }) {
                    InfoAccordionCard(
                        title = "Информация об объекте",
                        subtitle = "Эта информация будет видна арендатору",
                        expanded = tenantInfoExpanded,
                        onToggle = {
                            val willExpand = !tenantInfoExpanded
                            tenantInfoExpanded = willExpand
                            if (willExpand) scope.launch {
                                delay(250)
                                val overflow = (tenantInfoTop + tenantInfoHeight) - (columnTop + viewportHeight)
                                if (overflow > 0f) {
                                    scrollState.animateScrollTo((scrollState.value + overflow).toInt(), animationSpec = tween(300))
                                }
                            }
                        }
                    ) {
                        LabeledField(
                            icon = Icons.Filled.Phone,
                            caption = "Номер телефона",
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = "+7 "
                        )
                        LabeledField(
                            icon = Icons.Filled.Wifi,
                            caption = "Пароль WiFi",
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            placeholder = "Rsjuff6749"
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(start = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Правила объекта", style = FieldTextStyle)
                            MultilineTextField(
                                value = rulesText,
                                onValueChange = { rulesText = it },
                                placeholder = "Использовать помещение исключительно в целях, указанных в договоре"
                            )
                        }
                    }
                }

                // 11. Служебная информация
                Box(Modifier.onGloballyPositioned { coords -> serviceInfoTop = coords.localToRoot(Offset.Zero).y; serviceInfoHeight = coords.size.height.toFloat() }) {
                    InfoAccordionCard(
                        title = "Служебная информация",
                        subtitle = "Эта информация будет видна только вам",
                        expanded = serviceInfoExpanded,
                        onToggle = {
                            val willExpand = !serviceInfoExpanded
                            serviceInfoExpanded = willExpand
                            if (willExpand) scope.launch {
                                delay(250)
                                val overflow = (serviceInfoTop + serviceInfoHeight) - (columnTop + viewportHeight)
                                if (overflow > 0f) {
                                    scrollState.animateScrollTo((scrollState.value + overflow).toInt(), animationSpec = tween(300))
                                }
                            }
                        }
                    ) {
                        MultilineTextField(value = serviceInfo, onValueChange = { serviceInfo = it }, placeholder = "")
                    }
                }

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
                            serviceInfo = serviceInfo,
                            phone = phoneNumber,
                            wifiPassword = wifiPassword,
                            houseRules = rulesText,
                            latitude = uiState.selectedLatitude,
                            longitude = uiState.selectedLongitude,
                            onSuccess = { onCreated() }
                        )
                    }
                )
                FilledButton(
                    text = "Создать и опубликовать объявление",
                    enabled = false,
                    onClick = {
                        viewModel.createProperty(
                            name = name,
                            address = address.text,
                            area = area,
                            rentAmount = price,
                            description = description,
                            photoUris = photoUris,
                            serviceInfo = serviceInfo,
                            phone = phoneNumber,
                            wifiPassword = wifiPassword,
                            houseRules = rulesText,
                            latitude = uiState.selectedLatitude,
                            longitude = uiState.selectedLongitude,
                            onSuccess = { onCreated() }
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Graphite,
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
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 304.dp),
            containerColor = Color.White
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
                    if (!expanded) {
                        Text("Эта информация будет видна в объявлении", style = CardSubtitleStyle)
                    }
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
                                Text("Опишите свою недвижимость", style = FieldLabelStyle.copy(fontWeight = FontWeight.Normal))
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
            .background(if (enabled) Graphite else InactiveGray)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = ButtonTextStyle.copy(color = Color.White))
    }
}

// Аккордеон с заголовком и подзаголовком («Информация об объекте», «Служебная информация»)
@Composable
private fun InfoAccordionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
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
                    Text(title, style = FieldTextStyle)
                    if (!expanded) {
                        Text(subtitle, style = CardSubtitleStyle)
                    }
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Graphite
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// Поле с подписью и иконкой (телефон/WiFi) — карточка с рамкой
@Composable
private fun LabeledField(
    icon: ImageVector,
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(1.dp, GreyText, CardShape),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Graphite
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = FieldTextStyle,
                cursorBrush = SolidColor(Graphite),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                decorationBox = { innerTextField ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(caption, style = CardSubtitleStyle)
                        Box {
                            if (value.isEmpty()) {
                                Text(placeholder, style = FieldLabelStyle)
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }
    }
}

// Многострочное текстовое поле в карточке
@Composable
private fun MultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .heightIn(min = 80.dp),
            textStyle = FieldTextStyle.copy(fontWeight = FontWeight.Normal),
            cursorBrush = SolidColor(Graphite),
            singleLine = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = FieldLabelStyle.copy(fontWeight = FontWeight.Normal))
                    }
                    innerTextField()
                }
            }
        )
    }
}
