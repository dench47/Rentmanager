package com.rentmanager.app.ui.landlord.createproperty

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.FieldTextStyle
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobPlaceholderStyle
import com.rentmanager.app.ui.theme.Headline2MobStyle

// Опции дропдаунов и чипов (локально, в бэкенд уходят как строки)
private val SleepingOptions = listOf("1", "2", "3", "4", "5", "6+")
private val FloorOptions = listOf("Подвал", "Цоколь", "-2", "-1") + (1..100).map { it.toString() }
private val FloorsInHouseOptions = (1..100).map { it.toString() }

// Шаг 4: карточка объекта (Figma 2533:18104, пример для «Квартира + посуточно»).
// Адрес уже выбран на шаге 3 — здесь он только отображается (без карты).
@Composable
fun CreatePropertyScreen(
    propertyId: String? = null,
    propertyType: String = "Квартира",
    rentType: String = "посуточно",
    initialAddress: String = "",
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    onBack: () -> Unit,
    onCreated: () -> Unit = {},
    onPaymentSchedule: (String) -> Unit = {},
    viewModel: CreatePropertyViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var rooms by remember { mutableStateOf<String?>(null) }
    var sleepingPlaces by remember { mutableStateOf<String?>(null) }
    var floor by remember { mutableStateOf<String?>(null) }
    var floorsInHouse by remember { mutableStateOf<String?>(null) }

    var phoneNumber by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var rulesText by remember { mutableStateOf("") }
    var serviceInfo by remember { mutableStateOf("") }
    var tenantInfoExpanded by remember { mutableStateOf(false) }
    var serviceInfoExpanded by remember { mutableStateOf(false) }

    var photoUris by remember { mutableStateOf(listOf<String>()) }
    var showPhotoMenuIndex by remember { mutableIntStateOf(-1) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoUris = uris.map { it.toString() } + photoUris
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val canCreate = name.isNotBlank() &&
        initialAddress.isNotBlank() &&
        !uiState.isCreating

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            ScreenToolbar(title = "Новый объект", onBack = onBack, showClose = false)
            CreationProgressBar(currentStep = 4)
            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Фото (Figma: плитка 183x130, r30, камера 50, подпись Grey/Text)
                if (photoUris.isEmpty()) {
                    AddPhotoTile { galleryLauncher.launch("image/*") }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(photoUris) { index, uri ->
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(183.dp)
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(30.dp))
                                        .clickable { showPhotoMenuIndex = index }
                                )
                                DropdownMenu(
                                    expanded = showPhotoMenuIndex == index,
                                    onDismissRequest = { showPhotoMenuIndex = -1 },
                                    containerColor = Color.White
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
                            AddPhotoTile { galleryLauncher.launch("image/*") }
                        }
                    }
                }

                // 2. О квартире (Figma: заголовок + 12, чипы комнат r30, gap 6)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowSectionTitle("О квартире")
                    RoomsChips(selected = rooms, onSelect = { rooms = it })
                }

                // 3. Поля (Figma: gap 6; Название/Адрес 64, сетка 2x2 по 65)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CardInput(value = name, onValueChange = { name = it }, placeholder = "Название")
                    // Адрес перенесён на шаг 3 — здесь только отображение
                    CardAddressDisplay(address = initialAddress)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(65.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CardInput(
                            value = area,
                            onValueChange = { area = it },
                            placeholder = "Площадь, м2",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        CardDropdown(
                            selected = sleepingPlaces,
                            options = SleepingOptions,
                            placeholder = "Спальные места",
                            onSelect = { sleepingPlaces = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(65.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CardDropdown(
                            selected = floor,
                            options = FloorOptions,
                            placeholder = "Этаж",
                            onSelect = { floor = it },
                            modifier = Modifier.weight(1f)
                        )
                        CardDropdown(
                            selected = floorsInHouse,
                            options = FloorsInHouseOptions,
                            placeholder = "Этажей в доме",
                            onSelect = { floorsInHouse = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Описание объявления (Figma: заголовок + 12, карточка 155, padding 10)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowSectionTitle("Описание объявления")
                    DescriptionCard(
                        value = description,
                        onValueChange = { description = it }
                    )
                }

                // 5. Стоимость (Figma: заголовок + 12, карточка 64)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowSectionTitle("Стоимость")
                    CardInput(
                        value = price,
                        onValueChange = { price = it },
                        placeholder = if (rentType == "длительно") "Цена за месяц, ₽" else "Цена за сутки, ₽",
                        keyboardType = KeyboardType.Decimal
                    )
                }

                // 6. Дополнительно (Figma: заголовок + 12, контурные кнопки 55 r100, gap 6)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowSectionTitle("Дополнительно")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlineCtaButton(
                            text = "График платежей и реквизиты",
                            iconRes = R.drawable.ic_payment_schedule,
                            borderColor = Color(0xD9212121)
                        ) {
                            if (propertyId != null) {
                                onPaymentSchedule(propertyId)
                            } else if (canCreate) {
                                submitCreate(
                                    viewModel, name, initialAddress, area, price, description,
                                    photoUris, serviceInfo, phoneNumber, wifiPassword, rulesText,
                                    propertyType, rentType, rooms, sleepingPlaces, floor,
                                    floorsInHouse, initialLatitude, initialLongitude
                                ) { newId -> onPaymentSchedule(newId) }
                            }
                        }
                        OutlineCtaButton(
                            text = "Добавить счетчики",
                            iconRes = R.drawable.ic_add_counter,
                            borderColor = Graphite
                        ) {
                            // Заглушка: экран счетчиков будет добавлен позже
                        }
                    }
                }


                // 7. Аккордеоны (Figma: карточки 64, r20, подзаголовок Text 1 mob, шеврон 40)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoAccordionCard(
                        title = "Информация об объекте",
                        subtitle = "Эта информация будет видна арендатору",
                        expanded = tenantInfoExpanded,
                        onToggle = { tenantInfoExpanded = !tenantInfoExpanded }
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
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Правила объекта", style = Headline2MobStyle)
                            MultilineTextField(
                                value = rulesText,
                                onValueChange = { rulesText = it },
                                placeholder = "Использовать помещение исключительно в целях, указанных в договоре"
                            )
                        }
                    }
                    InfoAccordionCard(
                        title = "Служебная информация",
                        subtitle = "Эта информация будет видна только вам",
                        expanded = serviceInfoExpanded,
                        onToggle = { serviceInfoExpanded = !serviceInfoExpanded }
                    ) {
                        MultilineTextField(
                            value = serviceInfo,
                            onValueChange = { serviceInfo = it },
                            placeholder = ""
                        )
                    }
                }

                // 8. Кнопки (Figma: «Создать объект» контур + «Создать и опубликовать» градиент)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlineCtaButton(text = "Создать объект", enabled = canCreate) {
                        submitCreate(
                            viewModel, name, initialAddress, area, price, description,
                            photoUris, serviceInfo, phoneNumber, wifiPassword, rulesText,
                            propertyType, rentType, rooms, sleepingPlaces, floor,
                            floorsInHouse, initialLatitude, initialLongitude
                        ) { onCreated() }
                    }
                    // Публикация — отдельный шаг (как и раньше, неактивна)
                    GradientCtaButton(text = "Создать и опубликовать", enabled = false) {}
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun submitCreate(
    viewModel: CreatePropertyViewModel,
    name: String,
    address: String,
    area: String,
    price: String,
    description: String,
    photoUris: List<String>,
    serviceInfo: String,
    phone: String,
    wifiPassword: String,
    houseRules: String,
    type: String,
    rentType: String,
    rooms: String?,
    sleepingPlaces: String?,
    floor: String?,
    floorsInHouse: String?,
    latitude: Double?,
    longitude: Double?,
    onSuccess: (String) -> Unit
) {
    viewModel.createProperty(
        name = name,
        address = address,
        area = area,
        rentAmount = price,
        description = description,
        photoUris = photoUris,
        serviceInfo = serviceInfo,
        phone = phone,
        wifiPassword = wifiPassword,
        houseRules = houseRules,
        type = type,
        rentType = rentType,
        rooms = rooms,
        sleepingPlaces = sleepingPlaces,
        floor = floor,
        floorsInHouse = floorsInHouse,
        latitude = latitude,
        longitude = longitude,
        onSuccess = onSuccess
    )
}

// Плитка «Добавить фото» (Figma: 183x130, r30, камера 50, подпись 15 SemiBold Grey/Text)
@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(183.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(CardBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_add_photo_camera),
                contentDescription = "Добавить фото",
                modifier = Modifier.size(50.dp),
                colorFilter = ColorFilter.tint(GreyText)
            )
            Text("Добавить фото", style = Headline2MobStyle.copy(color = GreyText))
        }
    }
}

// Чипы количества комнат (Figma: сетка 5 равных колонок, gap 6; «6+ комнат» — по ширине текста, ряд 2 пустые колонки справа)
@Composable
private fun RoomsChips(selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Студия", "1", "2", "3", "4").forEach { option ->
                RoomChip(
                    label = option,
                    selected = selected == option,
                    modifier = Modifier.weight(1f)
                ) { onSelect(option) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoomChip(
                label = "5",
                selected = selected == "5",
                modifier = Modifier.width(69.6.dp)
            ) { onSelect("5") }
            RoomChip(label = "6+ комнат", selected = selected == "6+") { onSelect("6+") }
        }
    }
}

@Composable
private fun RoomChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(if (selected) Color.White else CardBackground)
            .then(
                if (selected) Modifier.border(1.dp, Graphite, RoundedCornerShape(30.dp)) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = Headline2MobStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Текстовое поле в карточке card_inf (Figma: 64, r20, #EFEFEF, padding 20/10, плейсхолдер Headline 2 mob Grey/Text)
@Composable
private fun CardInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = Headline2MobStyle,
            cursorBrush = SolidColor(Graphite),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = Headline2MobPlaceholderStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// Адрес — только отображение (карта на шаге 3)
@Composable
private fun CardAddressDisplay(address: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (address.isBlank()) {
            Text("Адрес", style = Headline2MobPlaceholderStyle)
        } else {
            Text(
                address,
                style = Headline2MobStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Дропдаун в карточке card_inf (ячейка 65, шеврон 40 справа, padding 20/10)
@Composable
private fun CardDropdown(
    selected: String?,
    options: List<String>,
    placeholder: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .clickable { expanded = true }
                .padding(start = 20.dp, end = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected ?: placeholder,
                    style = if (selected == null) Headline2MobPlaceholderStyle else Headline2MobStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(R.drawable.ic_card_chevron),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
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

// Описание объявления (Figma: карточка 155, r20, padding 10, плейсхолдер Text 1 mob)
@Composable
private fun DescriptionCard(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(155.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = CardSubtitleStyle.copy(color = Graphite),
            cursorBrush = SolidColor(Graphite),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text("Эта информация будет видна в объявлении", style = CardSubtitleStyle)
                    }
                    innerTextField()
                }
            }
        )
    }
}

// Аккордеон card_inf (Figma: 64, r20, заголовок 15 SemiBold + подзаголовок 13 Regular Grey/Text, шеврон 40)
@Composable
private fun InfoAccordionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(start = 20.dp, end = 10.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(title, style = Headline2MobStyle)
                Text(subtitle, style = CardSubtitleStyle)
            }
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .rotate(if (expanded) 180f else 0f)
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )
        }
    }
}

// Поле с иконкой (телефон/WiFi) внутри аккордеона — белая карточка
@Composable
private fun LabeledField(
    icon: ImageVector,
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(caption, style = CardSubtitleStyle)
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, style = Headline2MobPlaceholderStyle)
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

// Многострочное поле внутри аккордеона — белая карточка
@Composable
private fun MultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            textStyle = CardSubtitleStyle.copy(color = Graphite),
            cursorBrush = SolidColor(Graphite),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = CardSubtitleStyle)
                    }
                    innerTextField()
                }
            }
        )
    }
}




