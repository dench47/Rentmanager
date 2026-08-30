package com.rentmanager.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Диалог дизайнерской ширины: карточка занимает экран минус поля 16dp
 * (макеты Figma: 380dp на 412dp) — на любом устройстве и в масштабе темы.
 *
 * Платформа ограничивает окно Dialog ~80% ширины экрана, а Configuration
 * .screenWidthDp не пересчитывается при densityDpi-масштабировании — поэтому
 * ширина считается из пикселей экрана через текущую (масштабированную) плотность,
 * окно растягивается на весь экран, и контенту требуется точная ширина.
 * Использовать этот компонент вместо голого Dialog для всех диалогов-карточек.
 */
@Composable
fun DesignWidthDialog(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable () -> Unit
) {
    val screenPx = LocalContext.current.resources.displayMetrics.widthPixels
    val dialogWidth = with(LocalDensity.current) { screenPx.toDp() } - 32.dp
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
        Box(Modifier.requiredWidth(dialogWidth)) {
            content()
        }
    }
}
