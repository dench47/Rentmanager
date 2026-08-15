package com.rentmanager.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rentmanager.app.R

/**
 * Inter — основной шрифт приложения (по дизайну).
 * Статические TTF с кириллицей: Regular 400 / Medium 500 / SemiBold 600 / Bold 700.
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)
