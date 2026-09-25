package com.rentmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle

/**
 * Канонический диалог проекта (одобрен Денисом 25.09; эталон 3696:34110).
 * ВСЕ диалоги приложения — только через него.
 *
 * Геометрия: платформенное окно Dialog на всю ширину (decorView-отступы MIUI
 * обнулены), системное затемнение 45%, карточка = экран−40 (20 от краёв),
 * белая r20, андроидовская тень; верх/низ 20; тексты на 20 от края карточки,
 * кнопки на 10; заголовок 20/600/−0.3, через 6 текст 15/600/−0.4 #727272,
 * 20 от текста до кнопок; кнопки 55 r100 через 6. БЕЗ scale-функций.
 */
@Composable
fun CanonicalDialog(
    onDismiss: () -> Unit,
    title: String,
    text: String? = null,
    icon: Int? = null,
    buttons: (@Composable () -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.apply {
                setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                decorView.setPadding(0, 0, 0, 0)
                setDimAmount(0.45f)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 20.dp)
        ) {
            if (icon != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color.Unspecified
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    title,
                    fontSize = 20.sp,
                    lineHeight = 24.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    color = Graphite,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (buttons != null) Spacer(Modifier.height(20.dp))
            } else {
                Column(Modifier.padding(horizontal = 10.dp)) {
                    Text(
                        title,
                        fontSize = 20.sp,
                        lineHeight = 24.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        color = Graphite
                    )
                    if (text != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text,
                            fontSize = 15.sp,
                            lineHeight = 18.2.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.4).sp,
                            color = GreyText
                        )
                    }
                }
                if (buttons != null) Spacer(Modifier.height(20.dp))
            }
            if (buttons != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    buttons()
                }
            }
        }
    }
}

/** Кнопка 55 r100 канонического диалога: заливка или контур, текст 15/600/−0.4. */
@Composable
fun CanonicalDialogButton(
    text: String,
    container: Color? = null,
    stroke: Color? = null,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(container ?: Color.White)
            .then(
                if (stroke != null) Modifier.border(1.dp, stroke, RoundedCornerShape(100.dp))
                else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = Headline2MobStyle.copy(
                color = textColor,
                lineHeight = 18.2.sp
            )
        )
    }
}

/**
 * Канонический диалог с произвольным контентом (поля/списки внутри):
 * та же карточка/тень/платформенное окно, но содержимое передаётся снаружи.
 */
@Composable
fun CanonicalContentDialog(
    onDismiss: () -> Unit,
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.apply {
                setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                decorView.setPadding(0, 0, 0, 0)
                setDimAmount(0.45f)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                title,
                fontSize = 20.sp,
                lineHeight = 24.2.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = Graphite
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}
