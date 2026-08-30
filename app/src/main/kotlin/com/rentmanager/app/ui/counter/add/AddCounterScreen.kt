package com.rentmanager.app.ui.counter.add

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.ScreenToolbar
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.TextIconeStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Фон тулбара и системного бара (Figma Brand/tint)
private val BrandTint = Color(0xFFFFF1CF)
// Нижний таббар — rgba(237,237,237,0.6)
private val TabBarFill = Color(0x99EDEDED)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandTint)
            .statusBarsPadding()
    ) {
        ScreenToolbar(title = "Добавить счетчик", onBack = onBack)

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
                        caption = "Заводской номер",
                        value = uiState.counterNumber,
                        placeholder = "№",
                        onValueChange = viewModel::onNumberChange,
                        keyboardType = KeyboardType.Text
                    )
                    CounterInputField(
                        caption = "Внести начальное показание",
                        value = uiState.initialValue,
                        onValueChange = viewModel::onValueChange,
                        keyboardType = KeyboardType.Decimal
                    )
                    // Дата поверки: иконка календаря
                    CalendarPickerField(
                        caption = "Дата следующей поверки",
                        value = uiState.nextVerificationDate,
                        onClick = { showVerificationPicker = true }
                    )
                    RemindSwitchRow(
                        checked = uiState.remindVerification,
                        onCheckedChange = { viewModel.toggleRemindVerification() }
                    )
                    CounterInputField(
                        caption = "Передавать показания до",
                        value = uiState.submitReadingsBy,
                        onValueChange = viewModel::onSubmitReadingsByChange,
                        keyboardType = KeyboardType.Number
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
            onConfirm = { showVerificationPicker = false; viewModel.onNextVerificationDateChange(it) },
            onDismiss = { showVerificationPicker = false }
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

// Поле даты поверки: иконка календаря справа
@Composable
private fun CalendarPickerField(
    caption: String,
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(caption, style = CardSubtitleStyle)
            if (value.isNotBlank()) {
                Text(value, style = Headline2MobStyle, maxLines = 1)
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
    }
}

// Текстовое поле: подпись 13 Regular + ввод 15 SemiBold
@Composable
private fun CounterInputField(
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(caption, style = CardSubtitleStyle)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = Headline2MobStyle,
                cursorBrush = SolidColor(Graphite),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
                decorationBox = { inner ->
                    if (value.isBlank() && placeholder != null) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(placeholder, style = Headline2MobStyle)
                        }
                    }
                    inner()
                }
            )
        }
    }
}

// Кастомный тумблер (Figma Switch 2713:40959): 52×32, радиус 100
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
                    if (checked) Modifier.background(Graphite)
                    else Modifier
                        .background(CardBackground)
                        .drawBehind {
                            drawRoundRect(
                                color = GreyText,
                                cornerRadius = CornerRadius(100f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                )
                .clickable { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color.White else GreyText)
            )
        }
    }
}


// Пикер даты (Material3): возвращает выбранную дату в виде dd.MM.yyyy
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterDatePickerDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState()
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
        DatePicker(state = state)
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

