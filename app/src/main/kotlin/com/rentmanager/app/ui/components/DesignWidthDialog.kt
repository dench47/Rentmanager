package com.rentmanager.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Диалог дизайнерской ширины: карточка занимает экран минус поля 16dp
 * (макеты Figma: 380dp на 412dp) — на любом устройстве и в масштабе темы.
 *
 * Платформа ограничивает окно Dialog ~80% ширины экрана, поэтому окно
 * растягивается на весь экран (usePlatformDefaultWidth=false + setLayout),
 * а контент заполняет его по ширине минус поля 16dp. Никакой математики
 * «пиксели экрана → dp»: displayMetrics.widthPixels масштабированного
 * контекста на части прошивок (MIUI) возвращает НЕ физическую ширину,
 * из-за чего карточка рисовалась шире окна и обрезалась справа.
 * Использовать этот компонент вместо голого Dialog для всех диалогов-карточек.
 */
@Composable
fun DesignWidthDialog(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        val dialogWindow = (LocalView.current as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            content()
        }
    }
}
