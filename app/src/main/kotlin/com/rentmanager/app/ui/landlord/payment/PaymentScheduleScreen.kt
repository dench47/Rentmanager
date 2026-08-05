package com.rentmanager.app.ui.landlord.payment

import android.widget.NumberPicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.rentmanager.app.R

data class VariablePayment(
    val date: String,
    val amount: String
)

private val GradientBackground = Brush.verticalGradient(
    colors = listOf(Color.White, Color(0xFFF5F7FA))
)

@Composable
fun PaymentScheduleScreen(
    onBack: () -> Unit
) {
    var fixedDay by remember { mutableStateOf("") }
    var fixedAmount by remember { mutableStateOf("") }
    var fixedActive by remember { mutableStateOf(false) }
    var fixedDirty by remember { mutableStateOf(false) }

    var variableActive by remember { mutableStateOf(false) }
    var variableDirty by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf("") }
    var variableAmount by remember { mutableStateOf("") }
    val variableDates = remember { mutableStateListOf<VariablePayment>() }

    var calendarYear by remember { mutableStateOf(2026) }
    var calendarMonth by remember { mutableStateOf(8) }

    var requisitesExpanded by remember { mutableStateOf(false) }
    val requisitesList = remember { listOf("Реквизиты ИП", "Реквизиты ООО", "Карта Сбербанк") }
    var selectedRequisite by remember { mutableStateOf<String?>(null) }

    fun resetFixed() {
        fixedActive = false
        fixedDirty = false
        fixedDay = ""
        fixedAmount = ""
    }

    fun resetVariable() {
        variableActive = false
        variableDirty = false
        selectedDay = ""
        variableAmount = ""
        variableDates.clear()
    }

    fun onFixedChange() {
        if (variableDirty) resetVariable()
        fixedDirty = true
    }

    fun onVariableChange() {
        if (fixedDirty) resetFixed()
        variableDirty = true
    }

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GradientBackground)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "График платежей",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1D1F),
                            letterSpacing = (-0.3).sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ========== Fixed Payment Card ==========
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (fixedActive) Color(0xFFE8F5E9) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Постоянный платеж",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1D1D1F)
                                )
                                if (fixedActive) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        Modifier.size(20.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                }
                            }

                            Text(
                                "Укажите число месяца, в которое будет происходить оплата",
                                fontSize = 13.sp,
                                color = Color(0xFF8E8E93)
                            )

                            if (!variableActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DayPickerWithDialog(
                                        value = fixedDay,
                                        onDaySelected = {
                                            fixedDay = it
                                            onFixedChange()
                                        },
                                        modifier = Modifier.width(80.dp)
                                    )
                                    NumberTextField(
                                        value = fixedAmount,
                                        onValueChange = {
                                            fixedAmount = it
                                            onFixedChange()
                                        },
                                        placeholder = "Сумма (руб.)",
                                        textColor = Color(0xFF007AFF),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Buttons
                            if (fixedDirty || fixedActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!fixedActive) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(Color(0xFF212121))
                                                .clickable {
                                                    if (fixedAmount.isNotBlank()) {
                                                        if (fixedDay.isBlank()) fixedDay = "1"
                                                        fixedActive = true
                                                        fixedDirty = false
                                                        variableActive = false
                                                        resetVariable()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("ОК", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(Color(0xFFE53935).copy(alpha = 0.1f))
                                                .clickable { resetFixed() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Отменить и очистить", color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ========== Variable Payment Card ==========
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (variableActive) Color(0xFFE8F5E9) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Переменный платеж",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1D1D1F)
                                )
                                if (variableActive) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        Modifier.size(20.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                }
                            }

                            Text(
                                "Выберите конкретные дни и сумму платежа для каждого дня отдельно",
                                fontSize = 13.sp,
                                color = Color(0xFF8E8E93)
                            )

                            if (!fixedActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "<",
                                        modifier = Modifier.clickable {
                                            if (calendarMonth > 1) calendarMonth-- else { calendarMonth = 12; calendarYear-- }
                                            onVariableChange()
                                        },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1D1F)
                                    )
                                    Text(
                                        "${monthName(calendarMonth)} $calendarYear",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF007AFF)
                                    )
                                    Text(
                                        ">",
                                        modifier = Modifier.clickable {
                                            if (calendarMonth < 12) calendarMonth++ else { calendarMonth = 1; calendarYear++ }
                                            onVariableChange()
                                        },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1D1F)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DayPickerWithDialog(
                                        value = selectedDay,
                                        onDaySelected = {
                                            selectedDay = it
                                            onVariableChange()
                                        },
                                        modifier = Modifier.width(80.dp)
                                    )
                                    NumberTextField(
                                        value = variableAmount,
                                        onValueChange = {
                                            variableAmount = it
                                            onVariableChange()
                                        },
                                        placeholder = "Сумма",
                                        textColor = Color(0xFF007AFF),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF212121))
                                            .clickable {
                                                val day = selectedDay.ifEmpty { "1" }.trim()
                                                val amount = variableAmount.trim()
                                                if (amount.isNotBlank()) {
                                                    variableDates.add(
                                                        VariablePayment(
                                                            "${day.padStart(2, '0')}.${calendarMonth.toString().padStart(2, '0')}.$calendarYear",
                                                            amount
                                                        )
                                                    )
                                                    selectedDay = ""
                                                    variableAmount = ""
                                                    onVariableChange()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                LazyColumn(Modifier.heightIn(max = 150.dp)) {
                                    items(variableDates.toList()) { vp ->
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(vp.date, fontSize = 14.sp, color = Color(0xFF1D1D1F))
                                            Text("${vp.amount} руб.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D1D1F))
                                        }
                                    }
                                }
                            }

                            // Buttons
                            if (variableDirty || variableActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!variableActive) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(Color(0xFF212121))
                                                .clickable {
                                                    if (variableDates.isNotEmpty()) {
                                                        variableActive = true
                                                        variableDirty = false
                                                        fixedActive = false
                                                        resetFixed()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("ОК", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(Color(0xFFE53935).copy(alpha = 0.1f))
                                                .clickable { resetVariable() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Отменить и очистить", color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ========== Attach Requisites ==========
                    Box {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { requisitesExpanded = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    selectedRequisite ?: "Прикрепить реквизиты",
                                    fontSize = 15.sp,
                                    color = if (selectedRequisite != null) Color(0xFF1D1D1F) else Color(0xFF8E8E93)
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    null,
                                    Modifier.size(20.dp),
                                    tint = Color(0xFF8E8E93)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = requisitesExpanded,
                            onDismissRequest = { requisitesExpanded = false }
                        ) {
                            requisitesList.forEach { req ->
                                DropdownMenuItem(
                                    text = { Text(req) },
                                    onClick = {
                                        selectedRequisite = req
                                        requisitesExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color = Color(0xFF1D1D1F),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = TextStyle(fontSize = 15.sp, color = textColor),
            cursorBrush = SolidColor(textColor),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 15.sp, color = Color(0xFF8E8E93))
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun DayPickerWithDialog(
    value: String,
    onDaySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val initialValue = value.toIntOrNull() ?: 1
    var selectedValue by remember(initialValue) { mutableIntStateOf(initialValue) }

    Box(modifier = modifier) {
        Text(
            text = value.ifEmpty { "1" },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF007AFF),
            modifier = Modifier.clickable { showDialog = true }
        )
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Выберите день месяца",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1D1D1F),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Box(modifier = Modifier.padding(horizontal = 48.dp)) {
                        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
                        AndroidView(
                            factory = { context ->
                                NumberPicker(context).apply {
                                    minValue = 1
                                    maxValue = 31
                                    this.value = initialValue
                                    setOnValueChangedListener { _, _, newVal ->
                                        selectedValue = newVal
                                    }
                                }
                            },
                            modifier = Modifier.height(150.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            onDaySelected(selectedValue.toString())
                            showDialog = false
                        }
                    ) {
                        Text("Готово", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun monthName(month: Int): String = when (month) {
    1 -> "Январь"
    2 -> "Февраль"
    3 -> "Март"
    4 -> "Апрель"
    5 -> "Май"
    6 -> "Июнь"
    7 -> "Июль"
    8 -> "Август"
    9 -> "Сентябрь"
    10 -> "Октябрь"
    11 -> "Ноябрь"
    12 -> "Декабрь"
    else -> ""
}