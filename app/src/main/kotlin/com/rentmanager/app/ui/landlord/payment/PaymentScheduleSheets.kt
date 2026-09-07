package com.rentmanager.app.ui.landlord.payment

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.data.model.PaymentRequisiteDto
import com.rentmanager.app.ui.components.SheetDragHandle
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardShape
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobPlaceholderStyle
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.ToolbarTitleStyle

/** Шит выбора реквизитов (Figma 2872-34373): заголовок + подпись + список
 *  с радио-кружками справа; тап по строке выбирает и закрывает шит. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequisitesSheet(
    requisites: List<PaymentRequisiteDto>,
    selectedId: String?,
    onPick: (PaymentRequisiteDto) -> Unit,
    onDismiss: () -> Unit
) {
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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            SheetDragHandle()
            Text("Выберите реквизиты", style = ToolbarTitleStyle)
            Spacer(Modifier.height(6.dp))
            Text("Они будут указаны для будущих платежей", style = CardSubtitleStyle)
            Spacer(Modifier.height(20.dp))
            requisites.forEach { req ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clickable { onPick(req) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(req.name, style = Headline2MobStyle)
                        Text(req.accountCaption, style = CardSubtitleStyle)
                    }
                    Spacer(Modifier.width(12.dp))
                    RadioButton(selected = req.id == selectedId)
                }
            }
        }
    }
}

/** Радио-кружок дизайнера 24dp: невыбранный — серый обвод 1.5dp;
 *  выбранный — графитовая обводка + внутренняя точка 10dp. */
@Composable
private fun RadioButton(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(
                width = 1.5.dp,
                color = if (selected) Graphite else GreyText,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Graphite)
            )
        }
    }
}

/** Шит «Нет реквизитов» (Figma 2872-34394): иллюстрация 128, заголовок,
 *  подпись, CTA «Добавить реквизиты» с белым плюсом. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyRequisitesSheet(
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SheetDragHandle()
            Image(
                painter = painterResource(R.drawable.img_no_requisites),
                contentDescription = null,
                modifier = Modifier.size(128.dp)
            )
            Spacer(Modifier.height(32.dp))
            Text("Нет реквизитов", style = ToolbarTitleStyle)
            Spacer(Modifier.height(6.dp))
            Text(
                "Добавьте реквизиты, чтобы привязать их к графику платежей",
                style = CardSubtitleStyle,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            BlackCtaButton(
                text = "Добавить реквизиты",
                iconRes = R.drawable.ic_cta_plus,
                onClick = onAdd
            )
        }
    }
}

/** Форма создания реквизитов (в макете отсутствует — собрана по дизайн-системе):
 *  три поля card_inf + CTA «Сохранить». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequisiteSheet(
    onSave: (name: String, account: String, bank: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }

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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetDragHandle()
            Text("Добавить реквизиты", style = ToolbarTitleStyle)
            Spacer(Modifier.height(4.dp))
            RequisiteTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Название"
            )
            RequisiteTextField(
                value = account,
                onValueChange = { account = it },
                placeholder = "Расчётный счёт"
            )
            RequisiteTextField(
                value = bank,
                onValueChange = { bank = it },
                placeholder = "Банк"
            )
            Spacer(Modifier.height(4.dp))
            BlackCtaButton(
                text = "Сохранить",
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), account.trim(), bank.trim()) }
            )
        }
    }
}

/** Поле card_inf (64dp, #EFEFEF, r20) с плейсхолдером — паттерн дизайн-системы. */
@Composable
private fun RequisiteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CardShape)
            .background(CardBackground)
            .padding(horizontal = 16.dp),
        textStyle = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = Graphite
        ),
        cursorBrush = SolidColor(Graphite),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, style = Headline2MobPlaceholderStyle)
                }
                inner()
            }
        }
    )
}
