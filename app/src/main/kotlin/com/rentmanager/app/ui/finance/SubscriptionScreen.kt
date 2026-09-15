package com.rentmanager.app.ui.finance

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.GradientCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.SectionTitleStyle

private val CardGrey = Color(0xFFEFEFEF)
private val GreenOk = Color(0xFF2F7D4D)
private val RedError = Color(0xFFFF4249)
private val DividerSoft = Graphite.copy(alpha = 0.4f)

/** Формат суммы баланса: «500 ₽» / «1 250 ₽» */
private fun Double.toRub(): String =
    String.format(java.util.Locale.US, "%,.0f ₽", this).replace(',', ' ')

/**
 * «Управление подпиской» (канвас 2990:50668): баланс, тариф и промокод —
 * с сервера; «Пополнить баланс» — демо-зачисление 30 ₽ (ЮKassa позже);
 * промокод проверяется на сервере («Проверяем» → успех/ошибка).
 */
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onAddProperty: () -> Unit = {},
    onHistory: () -> Unit = {},
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var promoInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Уже применённый промокод — заполняем поле и показываем «Применён»
    LaunchedEffect(state.appliedPromo) {
        if (state.appliedPromo != null) promoInput = state.appliedPromo!!
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            // Статус-бар сверху (эталон 2990:49817: System Bar 53 → Toolbar)
            .statusBarsPadding()
    ) {
        // Шапка по шаблону экрана «Арендодатель»: часы → заголовок = 42dp
        Spacer(Modifier.height(27.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_landlord_back),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Управление подпиской",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Graphite,
                    letterSpacing = (-0.3).sp
                )
            }
        }
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
                        Text(
                            state.balance.toRub(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp,
                            color = Graphite
                        )
                    }
                    HorizontalDivider(color = DividerSoft, thickness = 1.dp)
                    BalanceRow("Объектов в управлении", state.objects.toString())
                    HorizontalDivider(color = DividerSoft, thickness = 1.dp)
                    // Тариф: промокод применён → старая цена зачёркнута
                    // впритык к новой (зазор 4, 3005-56664)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Тариф",
                            style = Headline2MobStyle.copy(color = GreyText),
                            modifier = Modifier.weight(1f)
                        )
                        if (state.appliedPromo != null) {
                            Text(
                                "${state.baseRate.toInt()} ₽",
                                style = Headline2MobStyle.copy(color = GreyText),
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            if (state.appliedPromo != null)
                                "${state.rate.toInt()} ₽ / объект / день"
                            else "${state.baseRate.toInt()} ₽ / объект / день",
                            style = Headline2MobStyle
                        )
                    }
                    HorizontalDivider(color = DividerSoft, thickness = 1.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Списание в день",
                            style = Headline2MobStyle.copy(color = GreyText),
                            modifier = Modifier.weight(1f)
                        )
                        if (state.appliedPromo != null) {
                            Text(
                                (state.objects * state.baseRate).toRub(),
                                style = Headline2MobStyle.copy(color = GreyText),
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(state.dailyCharge.toRub(), style = Headline2MobStyle)
                    }
                }
            }

            // ================= Кнопки =================
            // Главная CTA — градиентная. «Добавить объект» появляется
            // только в состоянии «деньги на балансе есть, объекта ещё нет»
            // (2991:41594: 500 ₽ + 0 объектов); иначе — только «Пополнить
            // баланс» (2991:41505)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.objects == 0 && state.balance > 0) {
                    GradientCtaButton(text = "Добавить объект", onClick = onAddProperty)
                    // Демо-пополнение: сервер зачисляет 30 ₽
                    OutlineCtaButton(
                        text = "Пополнить баланс",
                        borderColor = Graphite,
                        onClick = { viewModel.topUp() }
                    )
                } else {
                    GradientCtaButton(
                        text = "Пополнить баланс",
                        onClick = { viewModel.topUp() }
                    )
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
                        val borderModifier = when {
                            state.appliedPromo != null -> Modifier.border(1.5.dp, GreenOk, RoundedCornerShape(20.dp))
                            state.promoFailed -> Modifier.border(1.5.dp, RedError, RoundedCornerShape(20.dp))
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
                                    // Правка ввода сбрасывает ошибку
                                    if (state.promoFailed) viewModel.resetPromoFailure()
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
                        val btnLabel = when {
                            state.promoChecking -> "Проверяем"
                            state.appliedPromo != null -> "Применён"
                            else -> "Применить"
                        }
                        val filled = promoInput.isNotBlank() && !state.promoFailed
                        if (filled) {
                            BlackCtaButton(
                                text = btnLabel,
                                modifier = Modifier.weight(1f),
                                enabled = state.appliedPromo == null,
                                onClick = { viewModel.applyPromo(promoInput) }
                            )
                        } else {
                            OutlineCtaButton(
                                text = btnLabel,
                                modifier = Modifier.weight(1f),
                                enabled = promoInput.isNotBlank(),
                                borderColor = Graphite,
                                onClick = { viewModel.applyPromo(promoInput) }
                            )
                        }
                    }
                    // Подсказка результата
                    when {
                        state.appliedPromo != null -> Text(
                            "✓ Промокод применен",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp,
                            color = GreenOk
                        )
                        state.promoFailed -> Text(
                            "⚠ Промокод не найден. Проверьте написание",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp,
                            color = RedError
                        )
                    }
                }
            }

            // ================= Ссылки =================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinkRow("История операций", onClick = onHistory)
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
