package com.rentmanager.app.ui.counter.edit

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.DayOfMonthPickerSheet
import com.rentmanager.app.ui.components.DatePickerSheet
import com.rentmanager.app.ui.counter.readings.formatMeterDateShort
import com.rentmanager.app.ui.counter.readings.meterName
import com.rentmanager.app.ui.counter.readings.meterTypeColors
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.repository.PropertyRepository
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton

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

private val FieldBg = Color(0xFFEFEFEF)

/**
 * Экран редактирования счётчика (Figma 2756-40749/40780): цвет типа заливает
 * экран до самого верха (статус-бар на цвете), под шапкой — белая панель со
 * скруглением верхних углов 20dp. Поля 372×64 #EFEFEF r20, между полями 12dp;
 * «Дата следующей поверки» — с иконкой календаря, «Передавать показания до» —
 * с шевроном вниз; под каждым тумблер-строка 32dp. Кнопки: чёрная
 * «Сохранить изменения» (серых состояний нет) и контурная красная
 * «Удалить счетчик» с иконкой из макета 2756-40767.
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
    val colors = meterTypeColors(meter?.type ?: "cold_water")
    if (uiState.isLoading || meter == null) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.header)
                .statusBarsPadding()
        ) {
            EditMeterToolbar("", colors.header, colors.onHeader, onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.onHeader)
            }
        }
        return
    }

    val ddMmYyyy = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    // Локальные правки (rememberSaveable — переживают уход на шит выбора даты)
    var factoryNumber by rememberSaveable(meter.id) { mutableStateOf(meter.factoryNumber) }
    var newValueText by rememberSaveable(meter.id) { mutableStateOf("") }
    var verificationDate by rememberSaveable(meter.id) {
        mutableStateOf(runCatching { LocalDate.parse(meter.nextVerificationDate) }.getOrNull())
    }
    var remindVerification by rememberSaveable(meter.id) { mutableStateOf(meter.remindVerification) }
    var submitDay by rememberSaveable(meter.id) { mutableStateOf(meter.submitReadingsBy.toIntOrNull()) }
    var remindReadings by rememberSaveable(meter.id) { mutableStateOf(meter.remindReadings) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showDeleteSheet by remember { mutableStateOf(false) }

    // Живая валидация показания: меньше предыдущего — ошибка, при вводе ≥
    // ошибка снимается сразу, до повторного нажатия «Сохранить»
    val newValue = newValueText.replace(',', '.').toDoubleOrNull()
    val readingError = newValue != null && newValue < meter.currentValue
    // Сохранение блокируется, пока включено напоминание без выбранной даты/дня —
    // тумблер при включении открывает шит выбора, но если выбор отменили,
    // CTA не срабатывает
    val remindersIncomplete =
        (remindVerification && verificationDate == null) ||
            (remindReadings && submitDay == null)
    val canSave = factoryNumber.isNotBlank() && !readingError && !remindersIncomplete && !uiState.isSaving

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Цвет типа заливает экран до самого верха — часы тоже на цвете
            .background(colors.header)
            .statusBarsPadding()
            .imePadding()
    ) {
        EditMeterToolbar(meterName(meter.type), colors.header, colors.onHeader, onBack)

        // Инфо-полоса на цвете: текущее показание и последнее изменение
        // (изменение сегодня → «Сегодня» вместо даты)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
        ) {
            InfoStripValue("Текущие показания", "${formatMeterNumber(meter.currentValue)} ${meter.unit}", colors.onHeader, Modifier.weight(1f))
            InfoStripValue("Последнее изменение", lastChangeText(meter.lastUpdated), colors.onHeader, Modifier.weight(1f))
        }

        // Белая панель со скруглением верхних углов
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)
        ) {
            MeterTextField(
                placeholder = "Заводской номер",
                caption = "Заводской номер",
                value = factoryNumber,
                keyboardType = KeyboardType.Number,
                onValueChange = { factoryNumber = it.filter { c -> c.isDigit() } }
            )
            Spacer(Modifier.height(12.dp))
            MeterTextField(
                placeholder = "Новое показание, ${meter.unit}",
                caption = "Новое показание, ${meter.unit}",
                value = newValueText,
                isError = readingError,
                errorHint = "Проверьте правильность заполнения",
                keyboardType = KeyboardType.Decimal,
                onValueChange = { newValueText = it }
            )
            Spacer(Modifier.height(12.dp))

            // Дата следующей поверки: иконка календаря справа
            PickerField(
                placeholder = "Дата следующей поверки",
                caption = "Дата следующей поверки",
                value = verificationDate?.format(ddMmYyyy).orEmpty(),
                iconRes = R.drawable.ic_calendar,
                onClick = { showDatePicker = true },
                // Тумблер напоминания включён, а дата не выбрана — поле красное
                isError = remindVerification && verificationDate == null,
                errorHint = "Выберите дату"
            )
            Spacer(Modifier.height(12.dp))
            EditSwitchRow(
                title = "Напоминание о поверке",
                checked = remindVerification,
                onChecked = { checked ->
                    remindVerification = checked
                    // Тумблер без даты — сразу открываем выбор даты
                    if (checked && verificationDate == null) showDatePicker = true
                }
            )
            // От тумблера до следующего поля — 18dp (макет 2756-40789 → 2756-40791),
            // тогда как поле → тумблер — 12dp
            Spacer(Modifier.height(18.dp))

            // Передавать показания до: шеврон вниз справа
            PickerField(
                placeholder = "Передавать показания до",
                caption = "Передавать показания до",
                value = submitDay?.let { "$it-го числа" }.orEmpty(),
                // Шеврон из макета (40-viewport) — отрисовка в полные 40dp,
                // иначе 11x5.5dp сжимаются до 6.6dp
                iconRes = R.drawable.ic_card_chevron,
                onClick = { showDayPicker = true },
                iconSize = 40.dp,
                // Тумблер напоминания включён, а день не выбран — поле красное
                isError = remindReadings && submitDay == null,
                errorHint = "Выберите день месяца"
            )
            Spacer(Modifier.height(12.dp))
            EditSwitchRow(
                title = "Напоминание о передаче показаний",
                checked = remindReadings,
                onChecked = { checked ->
                    remindReadings = checked
                    // Тумблер без дня — сразу открываем выбор дня
                    if (checked && submitDay == null) showDayPicker = true
                }
            )

            Spacer(Modifier.height(26.dp))
            // Кнопка всегда чёрная (серых состояний в макете нет);
            // клик без эффекта, пока форма некорректна
            BlackCtaButton(
                text = if (uiState.isSaving) "Сохранение…" else "Сохранить изменения",
                enabled = true,
                onClick = {
                    if (canSave) {
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
                }
            )
            Spacer(Modifier.height(6.dp))
            // «Удалить счетчик»: красный контур + иконка из макета 2756-40767
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
                Image(
                    painter = painterResource(R.drawable.ic_trash_meter),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "Удалить счетчик",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = ErrorRed
                )
            }
        }
    }

    // Закрытие шита гасит фокус — курсор не прыгает в поля ввода
    val sheetsFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
    if (showDatePicker) {
        DatePickerSheet(
            title = "Дата следующей поверки",
            initialDate = verificationDate ?: LocalDate.now(),
            onDone = {
                verificationDate = it
                showDatePicker = false
                sheetsFocusManager.clearFocus()
            },
            onDismiss = {
                showDatePicker = false
                sheetsFocusManager.clearFocus()
            }
        )
    }
    if (showDayPicker) {
        DayOfMonthPickerSheet(
            selectedDay = submitDay,
            onDone = {
                submitDay = it
                showDayPicker = false
                sheetsFocusManager.clearFocus()
            },
            onDismiss = {
                showDayPicker = false
                sheetsFocusManager.clearFocus()
            }
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

/** yyyy-MM-dd → «Сегодня» или dd.MM.yyyy. */
private fun lastChangeText(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return try {
        val date = LocalDate.parse(raw)
        if (date == LocalDate.now()) "Сегодня" else formatMeterDateShort(raw)
    } catch (_: Exception) {
        formatMeterDateShort(raw)
    }
}

@Composable
private fun InfoStripValue(
    caption: String,
    value: String,
    onColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            caption,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = InterFontFamily,
            color = onColor
        )
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = onColor
        )
    }
}

/** 456.0 -> "456"; 9.5 -> "9,5" (целые без дробной части, запятая как в макете). */
private fun formatMeterNumber(value: Double): String {
    val v = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    return v.replace('.', ',')
}

@Composable
private fun EditMeterToolbar(
    title: String,
    color: Color,
    onColor: Color,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .padding(start = 20.dp, end = 20.dp, top = 27.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Назад — клик по всей зоне «стрелка + название» (глобальное правило)
        Row(
            modifier = Modifier.clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_landlord_back),
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(onColor)
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
}

/**
 * Поле 372×64 #EFEFEF r20: пустое — плейсхолдер 15/600 #727272,
 * заполненное — подпись 13/400 сверху + значение 15/600 снизу.
 */
@Composable
private fun MeterTextField(
    placeholder: String,
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    suffix: String? = null,
    isError: Boolean = false,
    errorHint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val fieldFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(FieldBg)
                .then(
                    if (isError) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp))
                    else Modifier
                )
                // Тап в любом месте поля открывает клавиатуру
                .clickable { fieldFocus.requestFocus() }
                .padding(start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                // fillMaxWidth: иначе пустое поле схлопывается, декорация
                // сжимается и плейсхолдер переносится на вторую строку
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fieldFocus),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = if (isError) ErrorRed else Graphite
                ),
                cursorBrush = SolidColor(Graphite),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        // Плейсхолдер + само поле: курсор виден сразу при тапе
                        Box {
                            Text(
                                placeholder,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                color = GreyText,
                                maxLines = 1
                            )
                            inner()
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    caption,
                                    fontSize = 13.sp,
                                    fontFamily = InterFontFamily,
                                    color = GreyText
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (prefix != null) {
                                        Text(
                                            prefix,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterFontFamily,
                                            color = GreyText
                                        )
                                    }
                                    // Редактор занимает остаток строки: суффикс-юнит
                                    // никогда не выдавливается и не падает на вторую строку
                                    Box(Modifier.weight(1f, fill = false)) { inner() }
                                    if (suffix != null) {
                                        Text(
                                            suffix,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterFontFamily,
                                            color = GreyText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
        if (isError && errorHint != null) {
            Text(
                errorHint,
                fontSize = 9.5.sp,
                fontFamily = InterFontFamily,
                color = ErrorRed,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

/**
 * Поле-кнопка выбора (дата / день месяца): 372×64 #EFEFEF r20,
 * справа иконка 24 в белой зоне 40×40 (отступ 10 от края).
 */
@Composable
private fun PickerField(
    placeholder: String,
    caption: String,
    value: String,
    iconRes: Int,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    isError: Boolean = false,
    errorHint: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(FieldBg)
                .then(
                    if (isError) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp))
                    else Modifier
                )
                .clickable(onClick = onClick)
                .padding(start = 20.dp, end = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = GreyText,
                    modifier = Modifier.padding(end = 50.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        caption,
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        color = GreyText
                    )
                    Text(
                        value,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Graphite
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 0.dp)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        if (isError && errorHint != null) {
            Text(
                errorHint,
                fontSize = 9.5.sp,
                fontFamily = InterFontFamily,
                color = ErrorRed,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

/**
 * Тумблер-строка (Figma 2756-40749/40780): текст 13/500 #212121 слева,
 * тумблер 52×32 справа. OFF: трек #EFEFEF без рамки, ручка 16 #727272 слева;
 * ON: трек #212121, ручка 24 белая с галочкой справа.
 */
@Composable
private fun EditSwitchRow(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            // По макету 2756-40749: подпись с доп. отступом 20dp от края поля,
            // тумблер — 20dp от правого края
            .padding(start = 20.dp, end = 20.dp)
            .clickable { onChecked(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = InterFontFamily,
            color = Graphite,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(if (checked) Graphite else FieldBg)
                .then(
                    // Выключенный тумблер — обводка #727272 2dp (2756-40794)
                    if (checked) Modifier
                    else Modifier.border(2.dp, GreyText, RoundedCornerShape(100.dp))
                )
                .clickable { onChecked(!checked) },
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
            // Ручка серая #79747E (2761-43513); заголовок 20/600 ls-0.3 lh24 —
            // «?» доходит ровно до левого края ручки
            com.rentmanager.app.ui.components.SheetDragHandle(color = Color(0xFF79747E))
            Text(
                "Удалить счетчик?",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Graphite,
                lineHeight = 24.sp,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(6.dp))
            // 15/600 #727272 — при этой жирности текст ложится в три строки (2761-43513)
            Text(
                "Счётчик «$meterName» будет удален вместе с показаниями и напоминаниями. Это действие нельзя отменить",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = GreyText,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(20.dp))
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
                Text(
                    "Отменить",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Graphite
                )
            }
        }
    }
}
