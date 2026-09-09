package com.rentmanager.app.ui.components

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.ui.theme.CardShape
import com.rentmanager.app.ui.theme.Headline2MobStyle

/** Окно-уведомление (Figma 2872-34864/34883): карточка r20 без обводки
 *  (по комментарию дизайнера), иконка 50 по центру, зазор, текст 15/600
 *  со строками 18; паддинги 21 сверху/снизу; закрытие тапом вне окна.
 *  34864 (галочка): зазор 10; 34883 (глобус): зазор 12. */
@Composable
fun IconNotificationDialog(
    iconRes: Int,
    text: String,
    iconGap: Dp = 12.dp,
    onDismiss: () -> Unit
) {
    DesignWidthDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(vertical = 21.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(Modifier.height(iconGap))
                Text(
                    text,
                    style = Headline2MobStyle.copy(lineHeight = 18.sp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
