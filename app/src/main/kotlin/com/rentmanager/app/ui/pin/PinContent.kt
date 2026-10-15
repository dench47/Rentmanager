package com.rentmanager.app.ui.pin

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.AppScreenHeader
import com.rentmanager.app.ui.theme.InterFontFamily

internal val PinInk = Color(0xFF212121)
internal val PinRed = Color(0xFFFF4249)

// ---- Замеры макета 18 «после правок» (кадр 412×917) ----
private val KeySize = 84.dp            // клавиша пин-пэда
private val KeyGap = 20.dp             // зазор между клавишами (шаг колонок 104)
private val KeyRadius = 30.dp
private val DotSize = 20.dp
private val DotGap = 12.dp
private val TextBlockWidth = 268.dp    // ширина блока заголовка (переносится на 2 строки)
private val ErrorBlockWidth = 260.dp
private val ShieldWidth = 84.dp
private val ShieldHeight = 81.dp
private val ErrorGap = 10.dp           // точки → строка ошибки
private val ErrorHeight = 12.dp
private val ActionHeight = 44.dp       // строка «Не помню код»
private val ActionPad = 58.dp
private val KeyboardToAction = 32.dp   // клавиатура → «Не помню код»
private val TopOffsetNoToolbar = 36.dp // контент без шапки начинается на 36 под статус-баром
private val TopOffsetToolbar = 21.dp   // после канонной шапки (27 + 24 + 13) + 21 = 85
private val GapEntry = 82.dp           // верхний блок → клавиатура на экранах входа
private val GapCompact = 60.dp         // то же на создании/подтверждении
private val ErrorShift = 22.dp         // ошибка: +10 зазор +12 строка — клавиатура не съезжает

/**
 * PIN-экраны по макету 18 (ветка «после правок»). Одна клавиатура на все шаги:
 * создание / подтверждение / вход / ошибка / блокировка.
 *
 * Каркас: шапка (только создание/подтверждение) -> верхний блок (щит 84x81,
 * заголовок, подстрока, точки 20x20 или блок ошибки) -> клавиатура 4x84 с шагом
 * 104 -> строка «Не помню код». Позиция клавиатуры зафиксирована макетом, поэтому
 * при появлении строки ошибки сжимается зазор, а не съезжает клавиатура.
 */
@Composable
internal fun PinContent(
    title: String,
    // 20.sp — вход и блокировка; 18.sp — создание и подтверждение (замеры макета)
    titleSize: TextUnit,
    pin: String,
    errorMessage: String?,
    errorId: Int,
    isErrorAnimating: Boolean,
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    subtitle: String? = null,
    toolbarTitle: String? = null,
    onBack: (() -> Unit)? = null,
    onBiometric: (() -> Unit)? = null,
    onForgot: (() -> Unit)? = null,
    loading: Boolean = false
) {
    val shake = remember { Animatable(0f) }
    val hapticView = LocalView.current
    LaunchedEffect(errorId) {
        if (isErrorAnimating) {
            // Вибрация отказа вместе с тряской: REJECT с API 30, иначе LONG_PRESS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hapticView.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                hapticView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            shake.animateTo(0f, keyframes {
                durationMillis = PIN_SHAKE_MS.toInt()
                0f at 0
                -8f at 50
                8f at 100
                -6f at 160
                6f at 220
                -3f at 290
                3f at 340
                0f at 400
            })
        }
    }
    val hasToolbar = toolbarTitle != null
    // Ошибка добавляет 10 (зазор) + 12 (строка) и столько же снимается с зазора
    // до клавиатуры: её позиция остаётся макетной (303 / 330 от низа статус-бара)
    val gap = (if (hasToolbar) GapCompact else GapEntry) -
        (if (errorMessage != null) ErrorShift else 0.dp)
    val compactHeight = LocalConfiguration.current.screenHeightDp < 640
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Страховка для низких экранов — на «эталонной» высоте не срабатывает
        val topOffset = when {
            compactHeight -> 8.dp
            hasToolbar -> TopOffsetToolbar
            else -> TopOffsetNoToolbar
        }
        Column(Modifier.fillMaxWidth()) {
            if (toolbarTitle != null) {
                AppScreenHeader(title = toolbarTitle, onBack = { onBack?.invoke() })
            }
            Spacer(Modifier.height(topOffset))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_pin_shield),
                    contentDescription = null,
                    modifier = Modifier.size(ShieldWidth, ShieldHeight)
                )
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier.width(TextBlockWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        title,
                        fontSize = titleSize,
                        lineHeight = if (titleSize == 20.sp) 24.sp else 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = PinInk,
                        letterSpacing = (-0.3).sp,
                        textAlign = TextAlign.Center
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            subtitle,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontFamily = InterFontFamily,
                            color = PinInk,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                // Точки: пустая — обводка, заполненная — заливка, ошибка — красная заливка
                Row(
                    Modifier.graphicsLayer { translationX = shake.value },
                    horizontalArrangement = Arrangement.spacedBy(DotGap)
                ) {
                    repeat(4) { index ->
                        val filled = index < pin.length
                        Box(
                            Modifier
                                .size(DotSize)
                                .then(
                                    when {
                                        isErrorAnimating -> Modifier.background(PinRed, CircleShape)
                                        filled -> Modifier.background(PinInk, CircleShape)
                                        else -> Modifier.border(1.dp, PinInk, CircleShape)
                                    }
                                )
                        )
                    }
                }
                // Строка ошибки / прогресс живёт внутри верхнего блока, клавиатура не съезжает
                if (errorMessage != null || loading) {
                    Spacer(Modifier.height(ErrorGap))
                    Box(
                        Modifier
                            .width(ErrorBlockWidth)
                            .height(ErrorHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading && errorMessage == null) {
                            CircularProgressIndicator(
                                Modifier.size(12.dp),
                                color = PinInk,
                                strokeWidth = 1.5.dp
                            )
                        } else if (errorMessage != null) {
                            Text(
                                errorMessage,
                                color = PinRed,
                                fontSize = 9.5.sp,
                                lineHeight = 12.sp,
                                fontFamily = InterFontFamily,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(gap))
                PinKeypad(
                    enabled = enabled,
                    showBiometric = onBiometric != null,
                    onDigit = onDigit,
                    onDelete = onDelete,
                    onBiometric = { onBiometric?.invoke() }
                )
                if (onForgot != null) {
                    Spacer(Modifier.height(KeyboardToAction))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(ActionHeight)
                            .padding(horizontal = ActionPad)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onForgot() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Не помню код",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = PinInk,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** Пин-пэд: 4 ряда по 84 с зазором 20 (шаг колонок 104); 4-й ряд — [отпечаток] 0 [удалить]. */
@Composable
private fun PinKeypad(
    enabled: Boolean,
    showBiometric: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onBiometric: () -> Unit
) {
    val view = LocalView.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showBiometric) "biometric" else "", "0", "delete")
    )
    Column(verticalArrangement = Arrangement.spacedBy(KeyGap)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                row.forEach { key ->
                    PinKey(
                        key = key,
                        enabled = enabled,
                        onClick = {
                            // VIRTUAL_KEY — системная вибрация нажатия клавиши
                            // (раньше был почти незаметный TextHandleMove)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            when (key) {
                                "biometric" -> onBiometric()
                                "delete" -> onDelete()
                                else -> onDigit(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Клавиша 84x84: белая, r30, тень 0/1/3; цифра 36/400 Inter. */
@Composable
private fun PinKey(key: String, enabled: Boolean, onClick: () -> Unit) {
    if (key.isEmpty()) {
        Spacer(Modifier.size(KeySize))
        return
    }
    Box(
        modifier = Modifier
            .size(KeySize)
            .shadow(3.dp, RoundedCornerShape(KeyRadius), clip = false)
            .clip(RoundedCornerShape(KeyRadius))
            .background(Color.White)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "biometric" -> Icon(
                Icons.Outlined.Fingerprint,
                contentDescription = "Войти по отпечатку",
                modifier = Modifier.size(28.dp),
                tint = PinInk
            )
            "delete" -> Image(
                painter = painterResource(R.drawable.ic_pin_delete),
                contentDescription = "Удалить цифру",
                modifier = Modifier.size(34.dp, 24.dp)
            )
            else -> Text(
                key,
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = InterFontFamily,
                color = PinInk
            )
        }
    }
}