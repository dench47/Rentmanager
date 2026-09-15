package com.rentmanager.app.ui.finance

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.GradientCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.landlord.createproperty.ScreenToolbar
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.SectionTitleStyle
import kotlinx.coroutines.delay

private val CardGrey = Color(0xFFEFEFEF)
private val GreenOk = Color(0xFF2F7D4D)
private val RedError = Color(0xFFFF4249)

/** Состояния блока промокода (канвас 2990:50668) */
private enum class PromoState { IDLE, CHECKING, SUCCESS, ERROR }

/** Демо-промокод из макета; серверного API промокодов пока нет */
private const val DEMO_PROMO = "ОСЕНЬ"
private const val BASE_RATE = 10.0   // ₽ / объект / день
private const val PROMO_RATE = 8.0   // ₽ / объект / день по промокоду

/**
 * «Управление подпиской» (канвас 2990:50668): карточка баланса
 * (доступно / объектов / тариф / списание в день), кнопки пополнения
 * и добавления объекта (при нулевом списке), блок промокода со
 * состояниями (ввод / проверка / успех с рамкой и скидкой / ошибка),
 * ссылки на историю операций и условия.
 */
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    propertyCount: Int = 0,
    onAddProperty: () -> Unit = {}
) {
    var promoInput by remember { mutableStateOf("") }
    var promoState by remember { mutableStateOf(PromoState.IDLE) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Проверка промокода — секундная «пауза сервера», как в макете «Проверяем»
    LaunchedEffect(promoState) {
        if (promoState == PromoState.CHECKING) {
            delay(1000)
            promoState = if (promoInput.trim().equals(DEMO_PROMO, ignoreCase = true)) {
                PromoState.SUCCESS
            } else {
                PromoState.ERROR
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            // Статус-бар сверху (эталон 2990:49817: System Bar 53 → Toolbar)
            .statusBarsPadding()
    ) {
        ScreenToolbar(title = "Управление подпиской", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ================= Баланс =================
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Баланс", style = SectionTitleStyle)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardGrey)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Доступно — сумма крупнее, по нижней линии с подписью
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Доступно", style = Headline2MobStyle.copy(color = GreyText))
                        Spacer(Modifier.width(4.dp))
                        // Баланс: серверного API пока нет — показываем 0 ₽
                        Text(
                            "0 ₽",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp,
                            color = Graphite
                        )
                    }
                    HorizontalDivider(color = Graphite.copy(alpha = 0.4f), thickness = 1.dp)
                    BalanceRow("Объектов в управлении", propertyCount.toString())
                    HorizontalDivider(color = Graphite.copy(alpha = 0.4f), thickness = 1.dp)
                    // Тариф: успех промокода → старая цена зачёркнута + новая
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Тариф",
                            style = Headline2MobStyle.copy(color = GreyText),
                            modifier = Modifier.weight(1f)
                        )
                        // Успех промокода: старая цена зачёркнута ВПЛОТНУЮ
                        // к новой (зазор 4, 3005-56664) — единая группа справа
                        if (promoState == PromoState.SUCCESS) {
                            Text(
                                "10 ₽",
                                style = Headline2MobStyle.copy(color = GreyText),
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            if (promoState == PromoState.SUCCESS) "8 ₽ / объект / день"
                            else "10 ₽ / объект / день",
                            style = Headline2MobStyle
                        )
                    }
                    HorizontalDivider(color = Graphite.copy(alpha = 0.4f), thickness = 1.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Списание в день",
                            style = Headline2MobStyle.copy(color = GreyText),
                            modifier = Modifier.weight(1f)
                        )
                        val rate = if (promoState == PromoState.SUCCESS) PROMO_RATE else BASE_RATE
                        if (promoState == PromoState.SUCCESS) {
                            Text(
                                "${propertyCount * BASE_RATE.toInt()} ₽",
                                style = Headline2MobStyle.copy(color = GreyText),
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text("${(propertyCount * rate).toInt()} ₽", style = Headline2MobStyle)
                    }
                }
            }

            // ================= Кнопки =================
            // Главная CTA — градиентная (2991:41535): при нуле объектов
            // это «Добавить объект» (+ иконка), иначе градиент переносится
            // на «Пополнить баланс», а вторая остаётся контурной
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (propertyCount == 0) {
                    GradientCtaButton(text = "Добавить объект", onClick = onAddProperty)
                    // Пополнение баланса — платёжные API подключим позже
                    OutlineCtaButton(
                        text = "Пополнить баланс",
                        borderColor = Graphite,
                        onClick = { }
                    )
                } else {
                    GradientCtaButton(text = "Пополнить баланс", onClick = { })
                }
            }

            // ================= Промокод =================
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Промокод", style = SectionTitleStyle)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.height(55.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Поле 196×50: рамка зелёная (успех) / красная (ошибка)
                        val borderModifier = when (promoState) {
                            PromoState.SUCCESS -> Modifier.border(1.5.dp, GreenOk, RoundedCornerShape(20.dp))
                            PromoState.ERROR -> Modifier.border(1.5.dp, RedError, RoundedCornerShape(20.dp))
                            else -> Modifier
                        }
                        Box(
                            modifier = Modifier
                                .width(196.dp)
                                .height(50.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(CardGrey)
                                .then(borderModifier)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = promoInput,
                                onValueChange = {
                                    promoInput = it.take(20)
                                    if (promoState != PromoState.CHECKING) promoState = PromoState.IDLE
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }),
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily,
                                    letterSpacing = (-0.4).sp,
                                    color = Graphite
                                ),
                                cursorBrush = SolidColor(Graphite),
                                decorationBox = { inner ->
                                    Box(Modifier.fillMaxWidth()) {
                                        if (promoInput.isEmpty()) {
                                            Text(
                                                "Введите промокод",
                                                style = Headline2MobStyle.copy(color = GreyText)
                                            )
                                        }
                                        Box(Modifier.fillMaxWidth()) { inner() }
                                    }
                                }
                            )
                        }
                        // Кнопка 164×55: пусто/ошибка — контурная «Применить»,
                        // ввод — чёрная «Применить», проверка — «Проверяем»,
                        // успех — «Применён»
                        val btnLabel = when (promoState) {
                            PromoState.CHECKING -> "Проверяем"
                            PromoState.SUCCESS -> "Применён"
                            else -> "Применить"
                        }
                        val filled = promoInput.isNotBlank() &&
                            promoState != PromoState.ERROR
                        if (filled) {
                            BlackCtaButton(
                                text = btnLabel,
                                modifier = Modifier.weight(1f),
                                enabled = promoState != PromoState.SUCCESS,
                                onClick = { promoState = PromoState.CHECKING }
                            )
                        } else {
                            OutlineCtaButton(
                                text = btnLabel,
                                modifier = Modifier.weight(1f),
                                enabled = promoInput.isNotBlank(),
                                borderColor = Graphite,
                                onClick = { promoState = PromoState.CHECKING }
                            )
                        }
                    }
                    // Подсказка результата — с отступом поля (pad start 20)
                    when (promoState) {
                        PromoState.SUCCESS -> Text(
                            "✓ Промокод применен",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp,
                            color = GreenOk
                        )
                        PromoState.ERROR -> Text(
                            "⚠ Промокод не найден. Проверьте написание",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp,
                            color = RedError
                        )
                        else -> Unit
                    }
                }
            }

            // ================= Ссылки =================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinkRow("История операций")
                LinkRow("Условия подписки")
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Строка баланса: подпись серым слева, значение графитом справа. */
@Composable
private fun BalanceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = Headline2MobStyle.copy(color = GreyText))
        Text(value, style = Headline2MobStyle)
    }
}

/** Поле-ссылка 55: серый фон r20, текст 15/600, шеврон справа. */
@Composable
private fun LinkRow(label: String, onClick: () -> Unit = { }) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardGrey)
            .clickable { onClick() }
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = Headline2MobStyle, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified
        )
    }
}
