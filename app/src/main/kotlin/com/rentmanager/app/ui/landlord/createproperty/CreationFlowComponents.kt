package com.rentmanager.app.ui.landlord.createproperty

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Toolbar экранов флоу создания объекта (Figma: padding 13/20, иконка 24, зазор 8, крестик справа)
@Composable
fun ScreenToolbar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showClose: Boolean = false,
    onClose: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.clickable(onClickLabel = "Назад") { onBack() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_landlord_back),
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.size(8.dp))
            Text(title, style = ToolbarTitleStyle)
        }
        if (showClose) {
            Image(
                painter = painterResource(R.drawable.ic_toolbar_close),
                contentDescription = "Закрыть",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClickLabel = "Закрыть") { onClose?.invoke() },
                contentScale = ContentScale.Fit
            )
        }
    }
}

// Прогресс-индикатор из 4 сегментов (Figma: линия 88x4, скругление 2, активная #212121, неактивная 40%)
@Composable
fun CreationProgressBar(currentStep: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < currentStep) Graphite else Graphite.copy(alpha = 0.4f))
                )
            }
        }
    }
}

// Заголовок секции (Figma: Headline 1 mob — 20 SemiBold)
@Composable
fun FlowSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = ToolbarTitleStyle, modifier = modifier)
}

// Карточка выбора (Figma: 124, радиус 30, #EFEFEF, padding 15/31, иконка 74, зазор 20)
@Composable
fun ChoiceCard(
    label: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(horizontal = 31.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(74.dp)
        )
        Text(label, style = Headline2MobStyle)
    }
}

// Градиент CTA (Figma: linear-gradient 136deg, #F6D85E 19% -> #E89B5A 60% -> #D97D5D 100%)
internal fun ctaGradientBrush(widthPx: Float, heightPx: Float): Brush {
    val angleRad = Math.toRadians(136.0)
    val dirX = sin(angleRad).toFloat()
    val dirY = (-cos(angleRad)).toFloat()
    val length = abs(widthPx * dirX) + abs(heightPx * dirY)
    val center = Offset(widthPx / 2f, heightPx / 2f)
    val half = Offset(dirX * length / 2f, dirY * length / 2f)
    return Brush.linearGradient(
        colorStops = arrayOf(
            0.19f to Color(0xFFF6D85E),
            0.60f to Color(0xFFE89B5A),
            1.00f to Color(0xFFD97D5D)
        ),
        start = center - half,
        end = center + half
    )
}

@Composable
internal fun rememberCtaGradient(): Brush {
    val density = LocalDensity.current
    return remember(density) {
        with(density) { ctaGradientBrush(372.dp.toPx(), 55.dp.toPx()) }
    }
}

// Чёрная CTA-кнопка (Figma: 55, радиус 100, #212121, белый текст)
@Composable
fun BlackCtaButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Graphite)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(text, style = Headline2MobStyle.copy(color = Color.White))
        }
    }
}

// Контурная CTA-кнопка (Figma: 55, радиус 100, белая, рамка 1px, иконка 24 опционально, зазор 10)
@Composable
fun OutlineCtaButton(
    text: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    borderColor: Color = GreyText,
    iconSpacing: Dp = 10.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(100.dp))
            .background(Color.White)
            .border(1.dp, borderColor, RoundedCornerShape(100.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(iconSpacing)
        ) {
            iconRes?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(text, style = Headline2MobStyle)
        }
    }
}

// Градиентная CTA-кнопка (Figma: 55, радиус 100, градиент 136deg, текст #212121)
@Composable
fun GradientCtaButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val brush = rememberCtaGradient()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(brush)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = Headline2MobStyle)
    }
}
