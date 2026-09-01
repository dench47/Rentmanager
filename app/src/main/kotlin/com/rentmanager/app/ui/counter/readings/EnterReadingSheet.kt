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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
 * Шит «Внести показания» (Figma 2756-39236): хедер цвета типа
 * (имя + № + дата следующей проверки), текущее показание,
 * большое поле ввода с единицей, «Сохранить показания».
 * Живая валидация: значение меньше предыдущего — красное поле + подсказка,
 * при вводе значения ≥ предыдущего ошибка снимается сразу; кнопка
 * при ошибке/пустом поле неактивна (серый контур).
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
    var input by remember { mutableStateOf("") }
    val value = input.replace(',', '.').toDoubleOrNull()
    val isError = value != null && value < meter.currentValue
    val canSave = value != null && !isError && !isSaving

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Хедер цвета типа: имя, №, дата следующей проверки
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.header)
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp)
            ) {
                SheetDragHandle()
                Text(
                    meterName(meter.type),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = colors.onHeader
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "№${meter.factoryNumber}",
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        color = colors.onHeader.copy(alpha = 0.85f)
                    )
                    Text(
                        "Дата проверки: ${formatMeterDateShort(meter.nextVerificationDate)}",
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        color = colors.onHeader.copy(alpha = 0.85f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Текущие показания", fontSize = 13.sp, fontFamily = InterFontFamily, color = GreyText)
                    Text(
                        "${meter.currentValue} ${meter.unit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF212121)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Введите новое показание",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = GreyText
                )
                Spacer(Modifier.height(4.dp))
                BasicTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10) },
                    textStyle = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = if (isError) ErrorRed else Color(0xFF212121)
                    ),
                    cursorBrush = SolidColor(Color(0xFF212121)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    decorationBox = { inner ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box {
                                if (input.isEmpty()) {
                                    Text(
                                        "0",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = InterFontFamily,
                                        color = Color(0xFFC6C6C6)
                                    )
                                }
                                inner()
                            }
                            Text(
                                meter.unit,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = InterFontFamily,
                                color = GreyText,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                )
                if (isError) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Показание не может быть меньше предыдущего: ${meter.currentValue} ${meter.unit}",
                        fontSize = 9.5.sp,
                        fontFamily = InterFontFamily,
                        color = ErrorRed
                    )
                }
                Spacer(Modifier.height(20.dp))
                // Неактивна при пустом поле/ошибке: серый контур + серый текст
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .then(
                            if (canSave) Modifier
                                .background(Color(0xFF212121))
                                .clickable(onClick = { value?.let(onSave) })
                            else Modifier.border(1.dp, GreyText, RoundedCornerShape(100.dp))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isSaving) "Сохранение…" else "Сохранить показания",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = if (canSave) Color.White else GreyText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
