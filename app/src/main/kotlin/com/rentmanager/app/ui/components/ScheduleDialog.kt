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
    if (iconRes != null) {
        CanonicalDialog(
            onDismiss = onDismiss,
            icon = iconRes,
            title = title,
            text = body
        ) {
            CanonicalDialogButton(
                text = confirmText,
                container = confirmColor,
                textColor = Color.White,
                onClick = onConfirm
            )
            if (showBack) {
                CanonicalDialogButton(
                    text = backText,
                    stroke = Graphite,
                    textColor = Graphite,
                    onClick = { onBackAction?.invoke() ?: onDismiss() }
                )
            }
        }
    } else {
        CanonicalDialog(
            onDismiss = onDismiss,
            title = title,
            text = body
        ) {
            CanonicalDialogButton(
                text = confirmText,
                container = confirmColor,
                textColor = Color.White,
                onClick = onConfirm
            )
            if (showBack) {
                CanonicalDialogButton(
                    text = backText,
                    stroke = Graphite,
                    textColor = Graphite,
                    onClick = { onBackAction?.invoke() ?: onDismiss() }
                )
            }
        }
    }
}
