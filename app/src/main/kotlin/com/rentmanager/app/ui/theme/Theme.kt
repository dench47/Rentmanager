package com.rentmanager.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
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

@Composable
fun RentManagerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}