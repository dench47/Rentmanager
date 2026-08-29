package com.rentmanager.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat


private val LightColorScheme = lightColorScheme(
    primary = AlmostBlack,
    onPrimary = White,
    primaryContainer = DarkText,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = CardGray,
    onSurfaceVariant = AlmostBlack,
    error = Error,
    onError = White,
    outline = InactiveGray,
    outlineVariant = DividerColor,
    inverseSurface = AlmostBlack,
    inverseOnSurface = White,
    inversePrimary = White
)

/**
 * Контент темы: плотность масштабируется на уровне контекста Activity
 * (см. DesignWidth.kt — наследуется всеми окнами, включая шиты и диалоги),
 * здесь остаётся только фон системной навигации #EFEFEF («Android Navigation Bar» из макета).
 */
@Composable
private fun DesignScaledContent(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        // Подложка под системную навигацию
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFFEFEFEF))
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
        )
    }
}

@Composable
fun RentManagerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = { DesignScaledContent(content) }
    )
}
