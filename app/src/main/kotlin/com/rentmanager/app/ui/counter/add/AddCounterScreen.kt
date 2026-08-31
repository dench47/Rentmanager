package com.rentmanager.app.ui.counter.add

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.DesignWidthDialog
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.ScreenToolbar
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.TextIconeStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Фон тулбара и системного бара (Figma Brand/tint)
private val BrandTint = Color(0xFFFFF1CF)
// Нижний таббар — rgba(237,237,237,0.6)
private val TabBarFill = Color(0x99EDEDED)
// Включённый тумблер (Figma Switch 2691:29404: трек #151515)
private val SwitchOnTrack = Color(0xFF151515)

/**
 * Экран «Добавить счетчик» (Figma 2713:40952).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCounterScreen(
    propertyId: String,
    onBack: () -> Unit,
    onAdded: () -> Unit,
    viewModel: AddCounterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.savedEvents.collect { onAdded() } }
    LaunchedEffect(Unit) { viewModel.errorEvents.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }

    var showVerificationPicker by remember { mutableStateOf(false) }
    var showSubmitReadingsPicker by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // «Начал хоть что-то делать» — любое отличие от пустой формы
    val formDirty = uiState.counterType.isNotBlank() ||
        uiState.counterNumber.isNotBlank() ||
        uiState.initialValue.isNotBlank() ||
        uiState.nextVerificationDate.isNotBlank() ||
        uiState.submitReadingsBy.isNotBlank() ||
        uiState.remindVerification ||
        uiState.remindReadings

    val requestExit = { if (formDirty) showExitDialog = true else onBack() }
    // Системный «назад» с грязной формой — тоже через диалог
    BackHandler(enabled = formDirty) { showExitDialog = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandTint)
            .statusBarsPadding()
    ) {
        ScreenToolbar(title = "Добавить счетчик", onBack = requestExit)

        // Белая область: контент + таббар
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color.White)
        ) {
            // Скроллируемая форма
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeSelectorField(
                        selectedType = uiState.counterType,
                        isOpen = uiState.isTypeDropdownOpen,
                        types = uiState.types,
                        onToggle = viewModel::toggleTypeDropdown,
                        onSelect = viewModel::selectType
                    )
                    CounterInputField(
                        label = "Заводской номер",
                        value = uiState.counterNumber,
                        onValueChange = viewModel::onNumberChange,
                        keyboardType = KeyboardType.Number,
                        prefix = "№"
                    )
                    InitialReadingField(
                        value = uiState.initialValue,
                        onValueChange = viewModel::onValueChange,
                        unit = uiState.counterType.toDisplayUnit()
                    )
                    // Дата поверки: тап по полю/иконке календаря открывает пикер
                    CalendarPickerField(
                        label = "Дата следующей поверки",
                        value = uiState.nextVerificationDate,
                        onClick = { showVerificationPicker = true }
                    )
                    RemindSwitchRow(
                        checked = uiState.remindVerification,
                        onCheckedChange = { viewModel.toggleRemindVerification() }
                    )
                    // Иконки календаря тут нет (ошибка макета): тап по полю
                    // открывает окно даты в режиме ввода — как «календарь → карандаш»
                    DateTapField(
                        label = "Передавать показания до",
                        value = uiState.submitReadingsBy,
                        onClick = { showSubmitReadingsPicker = true }
                    )
                    RemindSwitchRow(
                        checked = uiState.remindReadings,
                        onCheckedChange = { viewModel.toggleRemindReadings() }
                    )
                }

                BlackCtaButton(
                    text = "Добавить счетчик",
                    enabled = !uiState.isSaving,
                    onClick = { viewModel.addMeter(propertyId) }
                )
            }

            // Нижний таббар (Figma 2713:40968)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(TabBarFill)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(196.dp)
                            .height(55.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Graphite),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Финансовый отчет объекта", style = CardSubtitleStyle.copy(color = Color.White))
                    }
                    CounterTabButton("Позвонить", R.drawable.ic_tab_call) {}
                    CounterTabButton("Написать", R.drawable.ic_tab_email) {}
                }
            }
        }
    }

    if (showVerificationPicker) {
        CounterDatePickerDialog(
            initialDisplayMode = DisplayMode.Picker,
            onConfirm = {
                showVerificationPicker = false
                viewModel.onNextVerificationDateChange(it)
            },
            onDismiss = { showVerificationPicker = false }
        )
    }
    if (showSubmitReadingsPicker) {
        // Режим ввода — то же окно, что получается по «календарь → карандаш»
        CounterDatePickerDialog(
            initialDisplayMode = DisplayMode.Input,
            onConfirm = {
                showSubmitReadingsPicker = false
                viewModel.onSubmitReadingsByChange(it)
            },
            onDismiss = { showSubmitReadingsPicker = false }
        )
    }
    if (showExitDialog) {
        ExitConfirmDialog(
            onContinueEditing = { showExitDialog = false },
            onExit = {
                showExitDialog = false
                onBack()
            }
        )
    }
}

// ---------- вспомогательные ----------

// Карточка выбора типа счётчика: шеврон вниз (Figma arrow 2425:7132)
@Composable
private fun TypeSelectorField(
    selectedType: String,
    isOpen: Boolean,
    types: List<String>,
    onToggle: () -> Unit,
    onSelect: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .clickable(onClick = onToggle)
                .padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedType.ifBlank { "Выберите тип счетчика" },
                style = Headline2MobStyle,
                maxLines = 1
            )
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = onToggle) {
            types.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type, style = Headline2MobStyle) },
                    onClick = { onSelect(type) }
                )
            }
        }
    }
}

// Поле даты с иконкой календаря: название и значение в одну строку
// («Дата следующей поверки 30.08.2026»), иконка 24dp в зоне 40dp
@Composable
private fun CalendarPickerField(
    label: String,
    value: String,
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
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DateLabelValueLine(label = label, value = value, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Поле-дата без иконки: тап по всему полю открывает окно ввода даты
@Composable
private fun DateTapField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateLabelValueLine(label = label, value = value, modifier = Modifier.weight(1f))
    }
}

// Одна строка: название (15/600) + значение через пробел (15/400)
@Composable
private fun DateLabelValueLine(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Headline2MobStyle, maxLines = 1)
        if (value.isNotBlank()) {
            Spacer(Modifier.width(4.dp))
            Text(value, style = Headline2MobStyle.copy(fontWeight = FontWeight.Normal), maxLines = 1)
        }
    }
}

// Поле ввода (Figma): подпись сверху тонким (13/400 #727272),
// значение ниже жирным (15/600 #212121); «№» — постоянный жирный префикс,
// курсор сразу после него; suffix — хвост значения
@Composable
private fun CounterInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    suffix: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, style = CardSubtitleStyle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (prefix != null) {
                            Text(prefix, style = Headline2MobStyle)
                        }
                        Box(Modifier.weight(1f).heightIn(min = 18.dp)) {
                            innerTextField()
                        }
                        if (suffix != null) {
                            Text(suffix, style = Headline2MobStyle)
                        }
                    }
                }
            }
        )
    }
}

// «Внести начальное показание»: пусто — фраза одна строка слева, по центру
// высоты; при вводе поле двустрочное: подпись «Начальное показание» тонким
// сверху, значение (и единица) жирным на второй строке
@Composable
private fun InitialReadingField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = Headline2MobStyle,
            cursorBrush = SolidColor(Graphite),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    // Плейсхолдер-фраза по центру строки; ввод прозрачен, но уже может получить фокус
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        Text(
                            "Внести начальное показание",
                            style = Headline2MobStyle.copy(fontWeight = FontWeight.Normal, color = GreyText)
                        )
                        innerTextField()
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Начальное показание", style = CardSubtitleStyle)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(Modifier.weight(1f).heightIn(min = 18.dp)) {
                                innerTextField()
                            }
                            if (unit != null) {
                                Text(unit, style = Headline2MobStyle)
                            }
                        }
                    }
                }
            }
        )
    }
}

// Русское имя типа → единица измерения для суффикса начального показания;
// тип не выбран — суффикса нет
private fun String.toDisplayUnit(): String? = when (this) {
    "Электроэнергия" -> "кВт·ч"
    "Отопление" -> "Гкал"
    "Холодная вода", "Горячая вода" -> "м³"
    else -> null
}

// Тумблер «Включить напоминание» (Figma Switch 2691:29404):
// OFF — трек #EFEFEF, рамка #727272 2dp, бегунок 16dp #727272;
// ON — трек #151515 без рамки, бегунок 24dp белый с галочкой #212121
@Composable
private fun RemindSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Включить напоминание",
            style = Headline2MobStyle.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp)
        )
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(100.dp))
                .then(
                    if (checked) Modifier.background(SwitchOnTrack)
                    else Modifier
                        .background(CardBackground)
                        .border(2.dp, GreyText, RoundedCornerShape(100.dp))
                )
                .clickable { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_switch_check),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(GreyText)
                )
            }
        }
    }
}


// Диалог «Выйти без сохранения?» (Figma 2711:39066): карточка r20, заголовок
// 20/600, подпись 15/600 #727272, чёрная CTA «Продолжить редактирование» и
// контурная «Выйти без сохранения». Закрывается ТОЛЬКО кнопками — ни тап
// мимо, ни системный «назад» его не скрывают. Ширина — макетная (DesignWidthDialog)
@Composable
private fun ExitConfirmDialog(
    onContinueEditing: () -> Unit,
    onExit: () -> Unit
) {
    DesignWidthDialog(
        onDismissRequest = { },
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Выйти без сохранения?", style = ToolbarTitleStyle)
            Text(
                "Внесенные изменения не сохранятся",
                style = Headline2MobStyle.copy(color = GreyText)
            )
            Spacer(Modifier.height(14.dp))
            BlackCtaButton(
                text = "Продолжить редактирование",
                onClick = onContinueEditing
            )
            // Контурная кнопка выхода (1dp #212121, r100)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .border(1.dp, Graphite, RoundedCornerShape(100.dp))
                    .clickable(onClick = onExit),
                contentAlignment = Alignment.Center
            ) {
                Text("Выйти без сохранения", style = Headline2MobStyle)
            }
        }
    }
}


// Пикер даты (Material3): возвращает выбранную дату в виде dd.MM.yyyy.
// initialDisplayMode = Input — то же окно, что открывается по «карандашу»
// внутри календаря (режим ручного ввода даты)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterDatePickerDialog(
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(initialDisplayMode = initialDisplayMode)
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val cal = Calendar.getInstance().apply { timeInMillis = millis }
                    onConfirm(dateFormat.format(cal.time))
                } ?: onDismiss()
            }) { Text("ОК") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    ) {
        DatePicker(state = state, showModeToggle = true)
    }
}

// Таб-кнопка нижнего таббара (копия TabButton из карточки объекта)
@Composable
private fun CounterTabButton(label: String, iconRes: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .height(55.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Image(painter = painterResource(iconRes), contentDescription = label, modifier = Modifier.size(24.dp))
        Text(label, style = TextIconeStyle)
    }
}

