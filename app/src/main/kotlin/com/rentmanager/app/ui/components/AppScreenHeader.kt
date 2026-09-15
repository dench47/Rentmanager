package com.rentmanager.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.InterFontFamily

/**
 * ============================================================================
 * ЕДИНЫЙ ШАБЛОН ШАПКИ ЭКРАНА — обязателен для ВСЕХ экранов приложения.
 * Эталон — экран «Арендодатель» (Figma 2596-22499).
 * ============================================================================
 *
 * ПРАВИЛА (замеры с эталона):
 *  1. Статус-бар (часы) → заголовок = 42dp: статус-инсет + Spacer(27dp)
 *     (высота статус-бара берётся системным инсетом экрана, 27 — поверх).
 *  2. Строка заголовка: стрелка «назад» 24×24 + 8dp + текст.
 *  3. Заголовок: Inter SemiBold 20sp, letterSpacing −0.3, #212121.
 *  4. Боковые поля строки: 20dp; снизу до контента: 13dp.
 *  5. Крестик «закрыть» (если нужен) — 24×24 у правого края на той же строке.
 *
 * Использовать ЭТОТ компонент вместо самодельных шапок. Инсет статус-бара
 * остаётся на экране (statusBarsPadding / Scaffold insets), компонент
 * начинается уже ПОД статус-баром.
 */
@Composable
fun AppScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showClose: Boolean = false,
    onClose: (() -> Unit)? = null
) {
    Spacer(Modifier.height(27.dp))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.clickable(onClickLabel = "Назад") { onBack() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_landlord_back),
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp)
            )
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Graphite,
                letterSpacing = (-0.3).sp
            )
        }
        if (showClose) {
            Image(
                painter = painterResource(R.drawable.ic_toolbar_close),
                contentDescription = "Закрыть",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClickLabel = "Закрыть") { onClose?.invoke() }
            )
        }
    }
}
