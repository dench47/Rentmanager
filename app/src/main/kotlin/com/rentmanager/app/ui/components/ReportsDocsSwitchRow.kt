package com.rentmanager.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText

/**
 * Тумблер «Предоставляю отчётные документы» (канвас «17», 3970:83640 + кадр
 * секции 3970:83458): строка 372×60 r20 с рамкой 1 #DBDBDB (inside, без
 * заливки), паддинги 20/10/10/10; слева заголовок 15/600 + 4 + подпись 13/400,
 * справа свитч 52×32. Кликабелен ТОЛЬКО свитч — строка нет.
 */
@Composable
fun ReportsDocsSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val borderColor = Color(0xFFDBDBDB)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .drawBehind {
                // Рамка 1dp inside, как strokeAlign INSIDE в макете
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(20.dp.toPx() - stroke / 2f),
                    style = Stroke(stroke)
                )
            }
            .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Предоставляю отчётные документы",
                fontSize = 15.sp,
                lineHeight = 18.2.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = Graphite
            )
            Text(
                "Гости увидят эту информацию в объявлении",
                fontSize = 13.sp,
                lineHeight = 15.7.sp,
                letterSpacing = (-0.4).sp,
                color = GreyText
            )
        }
        DesignSwitch(checked = checked, onToggle = onCheckedChange)
    }
}

/**
 * Свитч 52×32 по макету (OFF 3970:83640, ON 3970:82975):
 * OFF — трек #EFEFEF + рамка 2 #727272 inside, ручка 16 #727272 слева (8);
 * ON — трек #212121 без рамки, ручка 24 белая справа (4) с галочкой #212121.
 */
@Composable
fun DesignSwitch(
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val trackColor = if (checked) Graphite else CardBackground
    val outlineColor = GreyText
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .drawBehind {
                if (!checked) {
                    // Рамка 2dp inside (strokeAlign INSIDE)
                    val stroke = 2.dp.toPx()
                    drawRoundRect(
                        color = outlineColor,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(size.height / 2f - stroke / 2f),
                        style = Stroke(stroke)
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle(!checked) }
    ) {
        if (checked) {
            // Ручка 24×24 белая у правого края (отступ 4), галочка 16 по центру
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_switch_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            // Ручка 16×16 #727272 у левого края (отступ 8)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(GreyText)
            )
        }
    }
}

/**
 * Блок в карточке объекта при включённом тумблере (3980:87127, кадр 3801:67317):
 * карточка 372×58 r20 #EFEFEF без рамки, паддинги 20/10/10/10, те же тексты.
 */
@Composable
fun ReportsDocsInfoCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Предоставляю отчётные документы",
            fontSize = 15.sp,
            lineHeight = 18.2.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = Graphite
        )
        Text(
            "Гости увидят эту информацию в объявлении",
            fontSize = 13.sp,
            lineHeight = 15.7.sp,
            letterSpacing = (-0.4).sp,
            color = GreyText
        )
    }
}
