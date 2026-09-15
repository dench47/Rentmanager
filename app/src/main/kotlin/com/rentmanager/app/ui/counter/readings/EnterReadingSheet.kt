package com.rentmanager.app.ui.counter.readings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.ui.components.SheetDragHandle
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.InterFontFamily

/**
 * Цвет шита и хедера — по типу счётчика (Figma 2713-49720):
 * ХВС #3568B6, ГВС #9C503F, электроэнергия #F8DF8C (тёмный текст),
 * отопление #E89B5A.
 */
data class MeterTypeColors(val header: Color, val onHeader: Color)

fun meterTypeColors(apiType: String): MeterTypeColors = when (apiType) {
    "cold_water" -> MeterTypeColors(Color(0xFF3568B6), Color.White)
    "hot_water" -> MeterTypeColors(Color(0xFF9C503F), Color.White)
    "electricity" -> MeterTypeColors(Color(0xFFF8DF8C), Color(0xFF212121))
    "heat" -> MeterTypeColors(Color(0xFFE89B5A), Color.White)
    else -> MeterTypeColors(Color(0xFF3568B6), Color.White)
}

fun meterName(apiType: String): String = when (apiType) {
    "electricity" -> "Электроэнергия"
    "cold_water" -> "Холодная вода"
    "hot_water" -> "Горячая вода"
    "heat" -> "Отопление"
    else -> apiType
}

/** yyyy-MM-dd → dd.MM.yyyy; пусто → «—». */
fun formatMeterDateShort(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    val parts = raw.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else raw
}

/**
 * Шит «Внести показания» (Figma 2756-40701/40678/40722):
 * цветной блок сверху (ручка + строка «Имя …… №» + строка «Дата поверки  дд.мм.гггг»),
 * белый контент с центрированными строками «Текущие показания  X юнит»,
 * «Введите новое показание», крупное число + юнит; кнопка «Сохранить показания».
 * Состояния: пусто — юнит без числа, кнопка чёрная; заполнено — число, кнопка
 * чёрная; ошибка (меньше предыдущего) — число красное, кнопка серый контур,
 * без подсказки (макет 2756-40722).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterReadingSheet(
    meter: MeterDto,
    isSaving: Boolean,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = meterTypeColors(meter.type)
    // Поле предзаполнено текущим показанием (макет 2756-40695): при повторном
    // открытии шита сохранённая цифра стоит в поле ввода, а не только в «Текущих показаниях»
    var input by remember { mutableStateOf(formatReadingNumber(meter.currentValue)) }
    val value = input.replace(',', '.').toDoubleOrNull()
    val isError = value != null && value < meter.currentValue
    val canSubmit = value != null && !isError && value != meter.currentValue && !isSaving

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Контейнер — цвет типа: скругление верхних углов шита (20dp) красит
        // именно цветной блок, а белый контент со своим скруглением сверху
        // оставляет в уголках цвет (макет 2756-40701)
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.header,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Цветной блок (102dp в макете): ручка, «Имя … №», «Дата поверки …»
            // (фон — сам контейнер шита)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp)
            ) {
                SheetDragHandle()
                // Строка 1: имя слева (20/600), заводской номер справа (15/600)
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        meterName(meter.type),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = colors.onHeader,
                        lineHeight = 24.sp,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        "№${meter.factoryNumber}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = colors.onHeader,
                        lineHeight = 18.sp,
                        letterSpacing = (-0.4).sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Строка 2: «Дата поверки» 13/400 + значение 15/600 в одну строку
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Дата поверки",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = InterFontFamily,
                        color = colors.onHeader,
                        lineHeight = 16.sp,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatMeterDateShort(meter.nextVerificationDate),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = colors.onHeader,
                        lineHeight = 18.sp,
                        letterSpacing = (-0.4).sp
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Белый контент — все строки центрированы (макет)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .padding(start = 20.dp, end = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                // «Текущие показания  456 кВт» — центрированная пара
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Текущие показания",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        color = GreyText,
                        lineHeight = 16.sp,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        "${formatReadingNumber(meter.currentValue)} ${meter.unit}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF212121),
                        lineHeight = 18.sp,
                        letterSpacing = (-0.4).sp
                    )
                }
                Spacer(Modifier.height(60.dp))
                Text(
                    "Введите новое показание",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF212121),
                    lineHeight = 18.sp,
                    letterSpacing = (-0.4).sp
                )
                Spacer(Modifier.height(20.dp))
                // Число 32/600 + юнит 20/600 группой по центру (макет 2756-40678:
                // зазор цифра→кВт = 4dp). Поле ввода само растягивается широко и
                // уводило юнит — поэтому ширина поля жёстко равна ширине цифр
                // (+2dp под курсор): курсор стоит сразу после цифр, дальше 4dp и юнит
                val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
                val density = androidx.compose.ui.platform.LocalDensity.current
                val numberWidth = with(density) {
                    textMeasurer.measure(
                        input,
                        TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            lineHeight = 39.sp,
                            letterSpacing = (-0.4).sp
                        )
                    ).size.width.toDp() + 2.dp
                }
                Row(
                    modifier = Modifier.clickable { focusRequester.requestFocus() },
                    // По макету: цифры и юнит на одной базовой линии шрифта
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = {
                            input = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10)
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .width(numberWidth)
                            .alignByBaseline(),
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = if (isError) ErrorRed else Color(0xFF212121),
                            lineHeight = 39.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF212121)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        )
                    )
                    Text(
                        meter.unit,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF212121),
                        lineHeight = 24.sp,
                        letterSpacing = (-0.4).sp,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Spacer(Modifier.height(40.dp))
                // Кнопка: пусто/заполнено — чёрная; ошибка — серый контур (макет)
                val buttonModifier = if (isError) {
                    Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .border(1.dp, GreyText, RoundedCornerShape(100.dp))
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF212121))
                        .then(
                            if (canSubmit) Modifier.clickable(onClick = { value?.let(onSave) })
                            else Modifier
                        )
                }
                Box(buttonModifier, contentAlignment = Alignment.Center) {
                    Text(
                        if (isSaving) "Сохранение…" else "Сохранить показания",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = if (isError) GreyText else Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        letterSpacing = (-0.4).sp
                    )
                }
            }
        }
    }
}

/** 456.0 → «456», дробные как есть. */
private fun formatReadingNumber(value: Double): String =
    (if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString())
        .replace('.', ',')
