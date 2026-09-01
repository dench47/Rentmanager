package com.rentmanager.app.ui.counter.edit

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.repository.PropertyRepository
import com.rentmanager.app.ui.components.DayOfMonthPickerSheet
import com.rentmanager.app.ui.components.DatePickerSheet
import com.rentmanager.app.ui.counter.readings.formatMeterDateShort
import com.rentmanager.app.ui.counter.readings.meterName
import com.rentmanager.app.ui.counter.readings.meterTypeColors
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobPlaceholderStyle
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.InterFontFamily
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EditMeterUiState(
    val isLoading: Boolean = true,
    val meter: MeterDto? = null,
    val isSaving: Boolean = false,
    val deleted: Boolean = false
)

@HiltViewModel
class EditMeterViewModel @Inject constructor(
    private val repository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditMeterUiState())
    val uiState: StateFlow<EditMeterUiState> = _uiState.asStateFlow()

    private val _savedEvents = MutableSharedFlow<Unit>()
    val savedEvents: SharedFlow<Unit> = _savedEvents.asSharedFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    fun load(propertyId: String, meterId: String) {
        viewModelScope.launch {
            val meter = try {
                repository.getMeters(propertyId).body()
                    ?.firstOrNull { it.id == meterId }
            } catch (_: Exception) {
                null
            }
            _uiState.value = EditMeterUiState(isLoading = false, meter = meter)
        }
    }

    /**
     * Сохранение правок: обновляет поля счётчика; если «Новое показание»
     * отличается от текущего — дополнительно вносит показание (история +
     * обновление текущего значения на сервере).
     */
    fun save(
        meter: MeterDto,
        factoryNumber: String,
        newValue: Double?,
        verificationDate: LocalDate?,
        remindVerification: Boolean,
        submitDay: Int?,
        remindReadings: Boolean
    ) {
        if (_uiState.value.isSaving) return
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val apiDate = verificationDate?.toString() ?: ""
                val updated = meter.copy(
                    factoryNumber = factoryNumber,
                    nextVerificationDate = apiDate,
                    submitReadingsBy = submitDay?.toString() ?: "",
                    remindVerification = remindVerification,
                    remindReadings = remindReadings
                )
                val resp = repository.updateMeter(meter.id, updated)
                if (!resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _errorEvents.emit("Не удалось сохранить (${resp.code()})")
                    return@launch
                }
                if (newValue != null && newValue != meter.currentValue) {
                    runCatching { repository.submitReading(meter.id, newValue) }
                }
                _uiState.value = _uiState.value.copy(isSaving = false, meter = resp.body() ?: updated)
                _savedEvents.emit(Unit)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
                _errorEvents.emit("Нет связи с сервером")
            }
        }
    }

    fun delete(meterId: String) {
        if (_uiState.value.isSaving) return
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val resp = repository.deleteMeter(meterId)
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isSaving = false, deleted = true)
                } else {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _errorEvents.emit("Не удалось удалить (${resp.code()})")
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
                _errorEvents.emit("Нет связи с сервером")
            }
        }
    }
}

/**
 * Экран редактирования счётчика (Figma 2755-37555): тулбар и полоса инфо
 * в цвете типа; поля Заводской № / Новое показание / Дата следующей проверки /
 * тумблеры напоминаний / Передавать показания до; «Сохранить изменения»
 * (неактивна при ошибке показания) и «Удалить счетчик» (красный контур)
 * с шитом подтверждения.
 */
@Composable
fun EditMeterScreen(
    propertyId: String,
    meterId: String,
    onBack: () -> Unit,
    viewModel: EditMeterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(propertyId, meterId) { viewModel.load(propertyId, meterId) }
    LaunchedEffect(Unit) { viewModel.savedEvents.collect { onBack() } }
    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(uiState.deleted) { if (uiState.deleted) onBack() }

    val meter = uiState.meter
    if (uiState.isLoading || meter == null) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            ToolbarColored(title = "", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        return
    }

    val colors = meterTypeColors(meter.type)
    val ddMmYyyy = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    // Локальные правки (rememberSaveable — переживают уход на шит выбора даты)
    var factoryNumber by androidx.compose.runtime.saveable.rememberSaveable(meter.id) {
        mutableStateOf(meter.factoryNumber)
    }
    var newValueText by androidx.compose.runtime.saveable.rememberSaveable(meter.id) {
        mutableStateOf("")
    }
    var verificationDate by androidx.compose.runtime.saveable.rememberSaveable(meter.id) {
        mutableStateOf(
            runCatching { LocalDate.parse(meter.nextVerificationDate) }.getOrNull()
        )
    }
    var remindVerification by androidx.compose.runtime.saveable.rememberSaveable(meter.id) {
        mutableStateOf(meter.remindVerification)
    }
    var submitDay by androidx.compose.runtime.saveable.rememberSaveable(meter.id) {
        mutableStateOf(meter.submitReadingsBy.toIntOrNull())
    }
    var remindReadings by androidx.compose.runtime.saveable.rememberSaveable(meter.id) {
        mutableStateOf(meter.remindReadings)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showDeleteSheet by remember { mutableStateOf(false) }

    // Живая валидация показания: меньше предыдущего — ошибка, при вводе ≥
    // ошибка снимается сразу, до повторного нажатия «Сохранить»
    val newValue = newValueText.replace(',', '.').toDoubleOrNull()
    val readingError = newValue != null && newValue < meter.currentValue
    val canSave = factoryNumber.isNotBlank() && !readingError && !uiState.isSaving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
    ) {
        ToolbarColored(
            title = meterName(meter.type),
            color = colors.header,
            onColor = colors.onHeader,
            onBack = onBack
        )

        // Инфо-полоса в цвете типа
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.header)
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Текущие показания",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = colors.onHeader.copy(alpha = 0.85f)
                )
                Text(
                    "${meter.currentValue} ${meter.unit}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = colors.onHeader
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Последнее изменение",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = colors.onHeader.copy(alpha = 0.85f)
                )
                Text(
                    formatMeterDateShort(meter.lastUpdated),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = colors.onHeader
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditMeterField(
                caption = "Заводской номер",
                prefix = "№",
                value = factoryNumber,
                onValueChange = { factoryNumber = it }
            )
            NewValueField(
                caption = "Новое показание",
                unit = meter.unit,
                value = newValueText,
                isError = readingError,
                onValueChange = { newValueText = it }
            )
            // Дата следующей проверки: тап открывает календарный шит
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEFEFEF))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (verificationDate == null) {
                    Text("Дата следующей проверки", style = Headline2MobPlaceholderStyle)
                } else {
                    Column {
                        Text("Дата следующей проверки", style = CardSubtitleStyle.copy(color = GreyText))
                        Text(verificationDate!!.format(ddMmYyyy), style = Headline2MobStyle)
                    }
                }
            }
            SwitchRow(
                title = "Напоминание о проверке",
                checked = remindVerification,
                onChecked = { checked ->
                    remindVerification = checked
                    // Тумблер без даты — сразу открываем выбор даты
                    if (checked && verificationDate == null) showDatePicker = true
                }
            )
            // Передавать показания до: тап открывает шит дня месяца
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEFEFEF))
                    .clickable { showDayPicker = true }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (submitDay == null) {
                    Text("Передавать показания до", style = Headline2MobPlaceholderStyle)
                } else {
                    Column {
                        Text("Передавать показания до", style = CardSubtitleStyle.copy(color = GreyText))
                        Text("${submitDay}-го числа", style = Headline2MobStyle)
                    }
                }
            }
            SwitchRow(
                title = "Напоминание о передаче показаний",
                checked = remindReadings,
                onChecked = { checked ->
                    remindReadings = checked
                    // Тумблер без дня — сразу открываем выбор дня
                    if (checked && submitDay == null) showDayPicker = true
                }
            )

            Spacer(Modifier.height(14.dp))
            BlackCtaButton(
                text = if (uiState.isSaving) "Сохранение…" else "Сохранить изменения",
                enabled = canSave,
                onClick = {
                    viewModel.save(
                        meter = meter,
                        factoryNumber = factoryNumber.trim(),
                        newValue = newValue,
                        verificationDate = verificationDate,
                        remindVerification = remindVerification,
                        submitDay = submitDay,
                        remindReadings = remindReadings
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
            // «Удалить счетчик» — красный контур с корзиной
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .border(1.dp, ErrorRed, RoundedCornerShape(100.dp))
                    .clickable { showDeleteSheet = true },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🗑 Удалить счетчик",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = ErrorRed
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        DatePickerSheet(
            title = "Дата следующей проверки",
            initialDate = verificationDate ?: LocalDate.now(),
            onDone = {
                verificationDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
    if (showDayPicker) {
        DayOfMonthPickerSheet(
            selectedDay = submitDay,
            onDone = {
                submitDay = it
                showDayPicker = false
            },
            onDismiss = { showDayPicker = false }
        )
    }
    if (showDeleteSheet) {
        DeleteMeterSheet(
            meterName = meterName(meter.type),
            onDelete = {
                showDeleteSheet = false
                viewModel.delete(meter.id)
            },
            onDismiss = { showDeleteSheet = false }
        )
    }
}

@Composable
private fun ToolbarColored(
    title: String,
    color: Color = Graphite,
    onColor: Color = Color.White,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(com.rentmanager.app.R.drawable.ic_landlord_back),
            contentDescription = "Назад",
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(onColor)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = onColor
        )
    }
}

@Composable
private fun EditMeterField(
    caption: String,
    prefix: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEFEFEF))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Headline2MobStyle,
            cursorBrush = SolidColor(Graphite),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("$caption $prefix", style = Headline2MobPlaceholderStyle)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(prefix, style = Headline2MobStyle.copy(color = GreyText))
                        inner()
                    }
                }
            }
        )
    }
}

@Composable
private fun NewValueField(
    caption: String,
    unit: String,
    value: String,
    isError: Boolean,
    onValueChange: (String) -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEFEFEF))
                .then(
                    if (isError) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp))
                    else Modifier
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10)) },
                singleLine = true,
                textStyle = Headline2MobStyle.copy(color = if (isError) ErrorRed else Color.Unspecified),
                cursorBrush = SolidColor(Graphite),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("$caption, $unit", style = Headline2MobPlaceholderStyle)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            inner()
                            Text(unit, style = Headline2MobStyle.copy(color = GreyText))
                        }
                    }
                }
            )
        }
        if (isError) {
            Text(
                "Проверьте правильность заполнения",
                fontSize = 9.5.sp,
                fontFamily = InterFontFamily,
                color = ErrorRed,
                modifier = Modifier.padding(start = 20.dp, top = 2.dp)
            )
        }
    }
}

/** Тумблер 52×32: off #EFEFEF c контуром, on #212121 (Figma 2713-49720). */
@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = Headline2MobStyle.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(RoundedCornerShape(100.dp))
                .then(
                    if (checked) Modifier.background(Graphite)
                    else Modifier
                        .background(Color(0xFFEFEFEF))
                        .border(1.dp, GreyText, RoundedCornerShape(100.dp))
                )
                .clickable { onChecked(!checked) },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(width = 44.dp, height = 28.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White)
                    .then(
                        if (checked) Modifier.padding(start = 6.dp)
                        else Modifier.padding(start = 0.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}

/** Шит подтверждения удаления (Figma 2761-43465). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DeleteMeterSheet(
    meterName: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            com.rentmanager.app.ui.components.SheetDragHandle()
            Text(
                "Удалить счетчик?",
                style = com.rentmanager.app.ui.theme.ToolbarTitleStyle
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Счётчик «$meterName» будет удален вместе с показаниями и напоминаниями. Это действие нельзя отменить",
                style = CardSubtitleStyle
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(ErrorRed)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Удалить счетчик",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .border(1.dp, Graphite, RoundedCornerShape(100.dp))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("Отменить", style = Headline2MobStyle)
            }
        }
    }
}
