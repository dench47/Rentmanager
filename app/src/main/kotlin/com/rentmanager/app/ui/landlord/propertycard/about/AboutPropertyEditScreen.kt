package com.rentmanager.app.ui.landlord.propertycard.about

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.landlord.createproperty.ScreenToolbar
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobPlaceholderStyle
import com.rentmanager.app.ui.theme.Headline2MobStyle

private val RoomsOptions = listOf("Студия", "1", "2", "3", "4", "5", "6+ комнат")
private val SleepingOptions = listOf("1", "2", "3", "4", "5", "6+")
private val FloorOptions = listOf("Подвал", "Цоколь", "-2", "-1") + (1..100).map { it.toString() }
private val FloorsInHouseOptions = (1..100).map { it.toString() }

/**
 * Экран «Об объекте» (Figma 2726-33827/33715): плоский стек полей без заголовков
 * секций, адаптивные поля (пусто — плейсхолдер, заполнено — подпись+значение),
 * ошибки с подсказками, кнопки «Сохранить изменения»/«Сбросить изменения».
 */
@Composable
fun AboutPropertyEditScreen(
    propertyId: String,
    onBack: () -> Unit,
    onOpenAddressPicker: (address: String, lat: Double?, lon: Double?) -> Unit = { _, _, _ -> },
    viewModel: AboutEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(propertyId) { viewModel.load(propertyId) }
    LaunchedEffect(Unit) { viewModel.savedEvents.collect { onBack() } }
    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val property = uiState.property
    val focusManager = LocalFocusManager.current
    // rememberSaveable: форма переживает уход на полноэкранный выбор адреса и назад
    var name by rememberSaveable(property?.id) { mutableStateOf(property?.name.orEmpty()) }
    var address by rememberSaveable(property?.id) { mutableStateOf(property?.address.orEmpty()) }
    var rooms by rememberSaveable(property?.id) { mutableStateOf(property?.rooms) }
    var area by rememberSaveable(property?.id) { mutableStateOf(property?.area.toFieldText()) }
    var sleepingPlaces by rememberSaveable(property?.id) { mutableStateOf(property?.sleepingPlaces) }
    var floor by rememberSaveable(property?.id) { mutableStateOf(property?.floor) }
    var floorsInHouse by rememberSaveable(property?.id) { mutableStateOf(property?.floorsInHouse) }
    var description by rememberSaveable(property?.id) { mutableStateOf(property?.description.orEmpty()) }
    var price by rememberSaveable(property?.id) { mutableStateOf(property?.rentAmount.toFieldText()) }
    var showDescriptionSheet by remember { mutableStateOf(false) }

    var addressError by remember { mutableStateOf(false) }
    // Координаты, выбранные на экране адреса (шаг 3 без прогресс-бара)
    var pickedLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var pickedLon by rememberSaveable { mutableStateOf<Double?>(null) }

    // Адрес с полноэкранной карты: применяем при возврате на экран
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                AboutAddressResult.consume()?.let { (newAddress, newLat, newLon) ->
                    address = newAddress
                    pickedLat = newLat
                    pickedLon = newLon
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var roomsError by remember { mutableStateOf(false) }
    var areaError by remember { mutableStateOf(false) }
    var sleepingError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    fun resetForm() {
        val p = uiState.initial ?: return
        name = p.name
        address = p.address
        rooms = p.rooms
        area = p.area.toFieldText()
        sleepingPlaces = p.sleepingPlaces
        floor = p.floor
        floorsInHouse = p.floorsInHouse
        description = p.description.orEmpty()
        price = p.rentAmount.toFieldText()
        pickedLat = null
        pickedLon = null
        addressError = false; roomsError = false; areaError = false; sleepingError = false; priceError = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
    ) {
        ScreenToolbar(title = "Об объекте", onBack = onBack)
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AboutField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Название"
            )
            // Тап по полю адреса открывает экран выбора с картой (шаг 3 без прогресс-бара)
            AboutField(
                value = address,
                onValueChange = { },
                placeholder = "Адрес*",
                isError = addressError,
                errorHint = "Обязательное поле",
                readOnly = true,
                onFieldClick = { onOpenAddressPicker(address, pickedLat ?: property?.latitude, pickedLon ?: property?.longitude) }
            )
            AboutDropdownField(
                selected = rooms,
                options = RoomsOptions,
                placeholder = "Количество комнат*",
                onSelect = { rooms = it; roomsError = false },
                isError = roomsError
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutField(
                    value = area,
                    onValueChange = { area = it; areaError = false },
                    placeholder = "Площадь, м2*",
                    keyboardType = KeyboardType.Decimal,
                    isError = areaError,
                    errorHint = "Обязательное поле",
                    modifier = Modifier.weight(1f)
                )
                AboutDropdownField(
                    selected = sleepingPlaces,
                    options = SleepingOptions,
                    placeholder = "Спальные места*",
                    onSelect = { sleepingPlaces = it; sleepingError = false },
                    isError = sleepingError,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutDropdownField(
                    selected = floor,
                    options = FloorOptions,
                    placeholder = "Этаж",
                    onSelect = { floor = it },
                    modifier = Modifier.weight(1f)
                )
                AboutDropdownField(
                    selected = floorsInHouse,
                    options = FloorsInHouseOptions,
                    placeholder = "Этажей в доме",
                    onSelect = { floorsInHouse = it },
                    modifier = Modifier.weight(1f)
                )
            }
            // Строка-навигация «Описание объявления» → шит с текстарией
            DescriptionNavRow(
                filled = description.isNotBlank(),
                onClick = { showDescriptionSheet = true }
            )
            AboutField(
                value = price,
                onValueChange = { price = it; priceError = false },
                placeholder = if (property?.rentType == "длительно") "Стоимость в месяц, ₽*" else "Стоимость за сутки, ₽*",
                keyboardType = KeyboardType.Decimal,
                isError = priceError,
                errorHint = "Обязательное поле"
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BlackCtaButton(
                    text = "Сохранить изменения",
                    enabled = !uiState.isSaving,
                    onClick = {
                        addressError = address.isBlank()
                        roomsError = rooms == null
                        areaError = area.isBlank()
                        sleepingError = sleepingPlaces == null
                        priceError = price.isBlank()
                        val hasErrors = addressError || roomsError || areaError || sleepingError || priceError
                        if (!hasErrors) {
                            viewModel.save(
                                name, address, rooms, area, sleepingPlaces, floor, floorsInHouse, description, price,
                                latitude = pickedLat, longitude = pickedLon
                            )
                        }
                    }
                )
                OutlineCtaButton(
                    text = "Сбросить изменения",
                    borderColor = Graphite,
                    onClick = { resetForm() }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDescriptionSheet) {
        DescriptionEditSheet(
            initial = description,
            onSave = {
                description = it
                showDescriptionSheet = false
            },
            onDismiss = { showDescriptionSheet = false }
        )
    }
}

// 55.0 → «55»
private fun Double?.toFieldText(): String {
    val v = this ?: return ""
    return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}

// Адаптивное поле (Figma 2726-33827/33715): пусто — плейсхолдер одной строкой
// (15/600 #727272); заполнено — подпись 13/400 сверху + значение 15/600 снизу.
// Ошибка: рамка #FF4249 + подпись красная + подсказка 9.5sp под полем.
@Composable
private fun AboutField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorHint: String? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    readOnly: Boolean = false,
    onFieldClick: (() -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .then(if (isError) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp)) else Modifier)
                .then(if (onFieldClick != null) Modifier.clickable(onClick = onFieldClick) else Modifier)
                .padding(start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (onFieldClick != null) {
                // Поле-кнопка (адрес): BasicTextField перехватывает тап, поэтому
                // рисуем статичный текст того же вида — клик получает Box
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = if (isError) CardSubtitleStyle.copy(color = ErrorRed)
                        else Headline2MobPlaceholderStyle
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            placeholder.removeSuffix("*"),
                            style = CardSubtitleStyle.copy(color = if (isError) ErrorRed else GreyText)
                        )
                        Text(value, style = Headline2MobStyle)
                    }
                }
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = readOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onFocusChanged != null) {
                                Modifier.onFocusChanged { onFocusChanged(it.isFocused) }
                            } else Modifier
                        ),
                    textStyle = Headline2MobStyle,
                    cursorBrush = SolidColor(Graphite),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = if (isError) CardSubtitleStyle.copy(color = ErrorRed)
                                else Headline2MobPlaceholderStyle
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    placeholder.removeSuffix("*"),
                                    style = CardSubtitleStyle.copy(color = if (isError) ErrorRed else GreyText)
                                )
                                innerTextField()
                            }
                        }
                    }
                )
            }
        }
        if (isError && errorHint != null) {
            AboutErrorHint(errorHint)
        }
    }
}

// Дропдаун того же адаптивного вида с шевроном
@Composable
private fun AboutDropdownField(
    selected: String?,
    options: List<String>,
    placeholder: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBackground)
                    .then(if (isError) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp)) else Modifier)
                    .clickable { expanded = true }
                    .padding(start = 20.dp, end = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (selected == null) {
                            Text(
                                placeholder,
                                style = if (isError) CardSubtitleStyle.copy(color = ErrorRed)
                                else Headline2MobPlaceholderStyle,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                placeholder.removeSuffix("*"),
                                style = CardSubtitleStyle.copy(color = if (isError) ErrorRed else GreyText),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(selected, style = Headline2MobStyle, maxLines = 1)
                        }
                    }
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
                        text = { Text(option, style = Headline2MobStyle) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (isError && selected == null) {
            AboutErrorHint("Выберите значение")
        }
    }
}

// Подсказка ошибки (Figma 2726-33715: 9.5/400 #FF4249, отступ слева 20)
@Composable
private fun AboutErrorHint(text: String) {
    Text(
        text,
        fontSize = 9.5.sp,
        lineHeight = 11.5.sp,
        letterSpacing = (-0.2).sp,
        color = ErrorRed,
        modifier = Modifier.padding(start = 20.dp)
    )
}

// Строка «Описание объявления» (Figma 2726-33838): заголовок + серый подзаголовок + шеврон
@Composable
private fun DescriptionNavRow(
    filled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Описание объявления", style = Headline2MobStyle)
            Text(
                if (filled) "Описание добавлено" else "Эта информация будет видна в объявлении",
                style = CardSubtitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_card_chevron),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
    }
}

// Шит редактирования описания: текстариа на серой карточке + «Сохранить изменения»
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DescriptionEditSheet(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0x66212121))
                )
            }
            Text("Описание объявления", style = Headline2MobStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 155.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBackground)
                    .padding(10.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = CardSubtitleStyle.copy(color = Graphite),
                    cursorBrush = SolidColor(Graphite),
                    decorationBox = { innerTextField ->
                        Box {
                            if (text.isEmpty()) {
                                Text("Эта информация будет видна в объявлении", style = CardSubtitleStyle)
                            }
                            innerTextField()
                        }
                    }
                )
            }
            BlackCtaButton(
                text = "Сохранить изменения",
                onClick = { onSave(text) }
            )
        }
    }
}
