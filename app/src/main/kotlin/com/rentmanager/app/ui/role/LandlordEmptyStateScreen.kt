package com.rentmanager.app.ui.role

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.InterFontFamily

/**
 * Пустое состояние разделов арендодателя без объектов (Figma 2533-17817):
 * тулбар «Арендодатель» + карточка с иллюстрацией 160×160, заголовком
 * «Добавьте первый объект», текстом и кнопками «Добавить первый объект»
 * (чёрная) и «Управление подпиской» (контурная).
 * Пометка дизайнера (2533-17834): карточки дашборда не «мёртвые» — по тапу
 * каждая открывает своё пустое состояние.
 */
@Composable
fun LandlordEmptyStateScreen(
    onBack: () -> Unit,
    onAddFirstObject: () -> Unit,
    onSubscription: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(13.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Клик по всей зоне «стрелка + название» = назад (единый стандарт приложения)
            Row(
                modifier = Modifier.clickable(onClickLabel = "Назад") { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_landlord_back),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Арендодатель",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF212121),
                    letterSpacing = (-0.3).sp
                )
            }
        }

        // Карточка по центру оставшейся области (Figma: card 372×435)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_empty_add_property),
                contentDescription = null,
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Добавьте первый объект",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Color(0xFF212121),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ведите аренду, платежи, договоры и показания счётчиков в одном месте",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Color(0xFF212121),
                letterSpacing = (-0.4).sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            BlackPillButton(
                text = "Добавить первый объект",
                iconRes = R.drawable.ic_cta_plus,
                onClick = onAddFirstObject
            )
            Spacer(Modifier.height(12.dp))
            // Градиентная кнопка (Figma 2533-17817: #F6D85E → #E89B5A → #D97D5D, тёмный текст)
            GradientCtaButton(
                text = "Управление подпиской",
                iconRes = R.drawable.ic_cta_diamond,
                onClick = onSubscription
            )
        }
    }
}

// Чёрная pill-кнопка 55dp r100 с иконкой (белые иконка и текст)
@Composable
private fun BlackPillButton(
    text: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFF212121))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = Color.White,
            letterSpacing = (-0.4).sp,
            maxLines = 1
        )
    }
}
