package com.rentmanager.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.CardShape
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle

/** Диалог дизайнера (Figma 2872-34877/34888/34890): карточка r20, опционально
 *  иконка 50 сверху, заголовок 20/600, тело 15/600 #717171, чёрная + контурная
 *  кнопки; вариант «Платёж был удален» — зелёная рамка и без «Назад». */
@Composable
fun ScheduleDialog(
    iconRes: Int?,
    title: String,
    body: String?,
    confirmText: String,
    confirmColor: Color = Graphite,
    borderColor: Color? = null,
    showBack: Boolean = true,
    backText: String = "Назад",
    onBackAction: (() -> Unit)? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DesignWidthDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            color = Color.White,
            border = borderColor?.let { BorderStroke(1.dp, it) }
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                iconRes?.let {
                    Image(
                        painter = painterResource(it),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    title,
                    style = Headline2MobStyle,
                    textAlign = TextAlign.Center
                )
                body?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        style = Headline2MobStyle.copy(color = GreyText),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(20.dp))
                BlackCtaButton(
                    text = confirmText,
                    containerColor = confirmColor,
                    onClick = onConfirm
                )
                if (showBack) {
                    Spacer(Modifier.height(6.dp))
                    OutlineCtaButton(
                        text = backText,
                        borderColor = Graphite,
                        onClick = { onBackAction?.invoke() ?: onDismiss() }
                    )
                }
            }
        }
    }
}
