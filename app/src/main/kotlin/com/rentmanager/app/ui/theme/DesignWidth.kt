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
 * строятся от контекста Activity. На экранах шире 412dp масштаб 1:1 (не увеличиваем).
 *
 * Источники данных специально не зависят от resources этого контекста:
 * на части холодных стартов (сразу после установки, после перезагрузки)
 * Configuration.screenWidthDp приходит нулевым, а displayMetrics —
 * полуинициализированным (density=1 при реальном densityDpi), из-за чего
 * экран казался «шире 412dp» и масштаб молча отключался. Поэтому:
 * — пиксельная ширина берётся у DisplayManager'а (сервис, а не ресурсы);
 * — реальная плотность — из configuration.densityDpi (приходит корректной
 *   даже в тех случаях, когда displayMetrics.density — мусор).
 */
private const val DESIGN_WIDTH_DP = 412f

fun Context.createDesignWidthContext(): Context {
    val configuration = resources.configuration

    val realDensity = configuration.densityDpi / 160f
    val widthPx = realDisplayWidthPx()

    if (realDensity > 0f && widthPx > 0) {
        val naturalWidthDp = widthPx / realDensity
        if (naturalWidthDp >= DESIGN_WIDTH_DP) return this
        val targetDpi = (160f * widthPx / DESIGN_WIDTH_DP).toInt()
        if (targetDpi >= configuration.densityDpi) return this
        return scaledContext(configuration, targetDpi)
    }

    // Запасной путь (DisplayManager недоступен): старая логика по configuration
    val metrics = resources.displayMetrics
    val screenWidthDp = configuration.screenWidthDp.takeIf { it > 0 }?.toFloat()
        ?: (if (metrics.density > 0f) metrics.widthPixels / metrics.density else 0f)
    val scale = if (screenWidthDp > 0f) (screenWidthDp / DESIGN_WIDTH_DP).coerceAtMost(1f) else 1f
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
