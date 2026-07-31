package com.rentmanager.app.ui.components

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Прогресс-бар из трёх сегментов (для онбординга).
 */
@Composable
fun StepProgressBar(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(3) { index ->
            val step = index + 1
            val isActive = step <= currentStep
            Box(
                modifier = Modifier
                    .width(if (index == 1) 115.dp else 116.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) Color(0xFF151515)
                        else Color(0xFFD3D3D3)
                    )
            )
            if (index < 2) {
                Spacer(modifier = Modifier.width(3.dp))
            }
        }
    }
}

@Composable
fun BackButton(
    text: String = "Назад",
    onClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = text,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFFFFFFFF)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .width(311.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(
                if (enabled && !isLoading) Color(0xFF212121)
                else Color(0xFF212121).copy(alpha = 0.5f)
            )
            .then(
                if (enabled && !isLoading) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun RoleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(172.5.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(
                if (isSelected) Color(0xFF212121) else Color.Transparent
            )
            .then(
                if (!isSelected) {
                    Modifier.border(
                        width = 0.5.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(100.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 32.dp, vertical = 10.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color(0xFF151515),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.4).sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun IconCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Настройки",
                modifier = Modifier.size(24.dp),
                tint = Color(0xFFFFFFFF)
            )
        }
    }
}

@Composable
fun BlackPaymentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(353.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.4).sp
            )
        }
    }
}

/**
 * Dashboard card — Figma: 174×120dp, cornerRadius 30, #EFEFEF background.
 * @param twoLines If true, reduces top/bottom padding to fit two lines of text (e.g. "Недвижимость\nв пользовании")
 */
@Composable
fun DashboardCard(
    @DrawableRes iconRes: Int,
    title: String,
    showBadge: Boolean = false,
    twoLines: Boolean = false,
    onClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(174.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFFEFEFEF))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = if (twoLines) 10.dp else 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon (50dp) with optional badge
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(50.dp),
                    contentScale = ContentScale.Fit
                )
                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9ED091))
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Spacer(modifier = Modifier.height(if (twoLines) 6.dp else 8.dp))
            // Label — Lato Bold 12sp, #151515E5
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xE5151515),
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Premium banner — Figma: Frame 140, 353×153dp, cornerRadius 30, #FEFFBB background.
 */
@Composable
fun PremiumBanner(
    onConnectClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(353.dp)
            .height(153.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFFFEFFBB))
    ) {
        Image(
            painter = painterResource(id = com.rentmanager.app.R.drawable.img_premium),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(169.dp)
                .height(153.dp),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 14.dp)
        ) {
            Text(
                text = "Премиум",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xE5151515),
                letterSpacing = (-0.4).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\u2022 Условие\n\u2022 Условие",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xCC151515),
                letterSpacing = (-0.4).sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF212121))
                    .clickable { onConnectClick() }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Подключить",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    letterSpacing = (-0.4).sp
                )
            }
        }
    }
}

@Composable
fun HomeTabButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(110.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Image(
            painter = painterResource(com.rentmanager.app.R.drawable.ic_tab_home),
            contentDescription = "На Главную",
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = "На Главную",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF404040),
            letterSpacing = (-0.4).sp
        )
    }
}