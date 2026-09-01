package com.rentmanager.app.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics

/**
 * Дизайнерская ширина макетов Figma (фреймы экранов — 412dp).
 *
 * Масштабирование выполняется на уровне контекста Activity (densityDpi), а не
 * CompositionLocal: тогда масштаб автоматически наследуют ВСЕ окна — экраны,
 * bottom sheet'ы, диалоги, попапы, включая будущие, — потому что их контексты
 * строятся от контекста Activity.
 *
 * НАДЁЖНОСТЬ: плотность из ресурсов (configuration.densityDpi, metrics.density)
 * и даже из display.getRealMetrics() на части холодных стартов приходит
 * мусорной (заниженной) — каждый раз экран «оказывался» шире 412dp, масштаб
 * молча отключался, и всё становилось крупно. Поэтому плотность НИГДЕ не
 * используется для решения о масштабе:
 * — целевая плотность считается ТОЛЬКО из пиксельной ширины дисплея
 *   (DisplayManager, ширина приходит корректной всегда): 160 × px / 412;
 * — исключение только для уже широких экранов (планшеты): screenWidthDp
 *   из конфигурации, и только когда он адекватен (> 0);
 * — страховка — shouldRecreateForDesignWidth(): проверяет уже ОТРИСОВАННУЮ
 *   ширину в dp и пересоздаёт Activity, если она меньше 412dp (см. MainActivity).
 */
private const val DESIGN_WIDTH_DP = 412f

fun Context.createDesignWidthContext(): Context {
    val widthPx = realDisplayWidthPx()
    if (widthPx > 0) {
        // Планшеты/широкие экраны не ужимаем — но верим screenWidthDp,
        // только когда он заполнен (мусор приходит нулём)
        val screenWidthDp = resources.configuration.screenWidthDp
        if (screenWidthDp > 0 && screenWidthDp >= DESIGN_WIDTH_DP) return this
        val targetDpi = (160f * widthPx / DESIGN_WIDTH_DP).toInt()
        return scaledContext(resources.configuration, targetDpi)
    }

    // Запасной путь (DisplayManager недоступен): старая логика по конфигурации
    val configuration = resources.configuration
    val metrics = resources.displayMetrics
    val fallbackWidthDp = configuration.screenWidthDp.takeIf { it > 0 }?.toFloat()
        ?: (if (metrics.density > 0f) metrics.widthPixels / metrics.density else 0f)
    val scale = if (fallbackWidthDp > 0f) (fallbackWidthDp / DESIGN_WIDTH_DP).coerceAtMost(1f) else 1f
    if (scale >= 0.999f) return this
    return scaledContext(configuration, (configuration.densityDpi * scale).toInt())
}

private fun Context.scaledContext(configuration: Configuration, targetDpi: Int): Context {
    val scaled = Configuration(configuration)
    scaled.densityDpi = targetDpi
    return createConfigurationContext(scaled)
}

/** Реальная ширина дисплея в пикселях через DisplayManager — работает в attachBaseContext. */
private fun Context.realDisplayWidthPx(): Int = try {
    val displayManager = getSystemService(DisplayManager::class.java) ?: return 0
    val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: return 0
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    display.getRealMetrics(metrics)
    metrics.widthPixels
} catch (_: Exception) {
    0
}

/**
 * Страховка после отрисовки: фактическая ширина экрана в dp МЕНЬШЕ 412dp —
 * масштаб не применился (полуинициализированный старт), Activity нужно
 * пересоздать один раз. Планшеты (>= 412dp от природы) не задевает.
 * Плотность дисплея не читает вообще — только сам факт отрисованной ширины.
 */
fun Context.shouldRecreateForDesignWidth(): Boolean {
    val metrics = resources.displayMetrics
    if (metrics.density <= 0f || metrics.widthPixels <= 0) return false
    val effectiveWidthDp = metrics.widthPixels / metrics.density
    return effectiveWidthDp < DESIGN_WIDTH_DP - 2f
}
