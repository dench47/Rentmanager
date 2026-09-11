package com.rentmanager.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Типографика на основе Figma-дизайна. Основной шрифт — Inter.
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    // "Добро пожаловать" — 28sp
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    // Заголовки экранов — 24sp
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.4).sp
    ),
    // "Выберите свою роль" — 20sp
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.4).sp
    ),
    // Заголовки секций/карточек — 18sp
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.4).sp
    ),
    // Кнопки — 16sp
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.4).sp
    ),
    // Цены — 14sp
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.4).sp
    ),
    // Основной текст — 16sp
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.4).sp
    ),
    // Подписи, даты — 14sp
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.4).sp
    ),
    // Мелкий текст — 12sp
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.4).sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

// Стили текста экрана создания объекта (Figma)
val ToolbarTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    letterSpacing = (-0.3).sp,
    color = Graphite
)
// Заголовок секции внутри экрана (правка дизайнера: 20 → 18, файл «8»,
// зелёные звёзды): «Аренда и платежи», «О квартире» и т.п.
val SectionTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    letterSpacing = (-0.3).sp,
    color = Graphite
)
val FieldTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = (-0.4).sp,
    color = Graphite
)
val FieldLabelStyle = FieldTextStyle.copy(color = GreyText)
val FieldLabelErrorStyle = FieldTextStyle.copy(color = ErrorRed)
val CardSubtitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = (-0.4).sp,
    color = GreyText
)
val ButtonTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = (-0.4).sp
)
// Figma "Headline 2 mob" — 15 SemiBold, -0.4sp, #212121
val Headline2MobStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    letterSpacing = (-0.4).sp,
    color = Graphite
)

// Плейсхолдер полей (Headline 2 mob в цвете Grey/Text)
val Headline2MobPlaceholderStyle = Headline2MobStyle.copy(color = GreyText)

// Название объекта на карточке (Figma "Headline 1mob" 22 SemiBold, -0.3sp)
val PropertyNameStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    letterSpacing = (-0.3).sp,
    color = Graphite
)

// Микроподписи под иконками таббара (Figma "Text Icone" — 9.5 Regular, -0.2sp)
val TextIconeStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 9.5.sp,
    letterSpacing = (-0.2).sp,
    color = Graphite
)
