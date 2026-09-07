package com.rentmanager.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.Headline2MobStyle
import kotlinx.coroutines.delay

/** Данные тоста в стиле дизайнера (Figma 2872-34864/34877/34883):
 *  графитовая карточка r20, иконка 50 сверху, белый текст, опциональная кнопка. */
data class DesignToastData(
    val text: String,
    val iconRes: Int,
    /** Текст кнопки; null — тост без кнопки */
    val actionText: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * Тост дизайнера: графитовая карточка по центру, показывается поверх контента.
 * Автоскрытие: 3.5 с без кнопки, 6 с с кнопкой.
 */
@Composable
fun DesignToast(
    data: DesignToastData,
    onDismiss: () -> Unit
) {
    LaunchedEffect(data) {
        delay(if (data.actionText != null) 6_000L else 3_500L)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Graphite)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(data.iconRes),
                contentDescription = null,
                modifier = Modifier.size(50.dp)
            )
            Text(
                data.text,
                style = Headline2MobStyle.copy(color = Color.White),
                textAlign = TextAlign.Center
            )
            if (data.actionText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF000000))
                        .clickable {
                            data.onAction?.invoke()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        data.actionText,
                        style = Headline2MobStyle.copy(color = Color.White),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
