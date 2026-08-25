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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat


/**
 * Дизайнерская ширина макета Figma (фреймы экранов — 412px).
 * Все dp/sp вёрстки масштабируются так, чтобы 412dp всегда занимали всю ширину экрана:
 * пропорции, размеры и переносы строк совпадают с макетом на любом устройстве.
 */
private const val DESIGN_WIDTH_DP = 412f

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
 * Контент темы: масштабирование плотности под дизайнерскую ширину 412dp
 * и фон системной навигации #F5F5F5 (компонент «Android Navigation Bar» из макета).
 */
@Composable
private fun DesignScaledContent(content: @Composable () -> Unit) {
    val currentDensity = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val designScale = if (screenWidthDp > 0) screenWidthDp / DESIGN_WIDTH_DP else 1f
    val designDensity = Density(
        density = currentDensity.density * designScale,
        fontScale = currentDensity.fontScale
    )

    CompositionLocalProvider(LocalDensity provides designDensity) {
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
