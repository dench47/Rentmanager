package com.rentmanager.app.ui.theme

import android.content.Context
import android.content.res.Configuration

/**
 * Дизайнерская ширина макетов Figma (фреймы экранов — 412dp).
 *
 * Масштабирование выполняется на уровне контекста Activity (densityDpi), а не
 * CompositionLocal: тогда масштаб автоматически наследуют ВСЕ окна — экраны,
 * bottom sheet'ы, диалоги, попапы, включая будущие, — потому что их контексты
 * строятся от контекста Activity. На экранах шире 412dp масштаб 1:1 (не увеличиваем).
 */
private const val DESIGN_WIDTH_DP = 412f

fun Context.createDesignWidthContext(): Context {
    val metrics = resources.displayMetrics
    if (metrics.density <= 0f) return this
    val screenWidthDp = metrics.widthPixels / metrics.density
    val scale = if (screenWidthDp > 0f) (screenWidthDp / DESIGN_WIDTH_DP).coerceAtMost(1f) else 1f
    if (scale >= 0.999f) return this
    val configuration = Configuration(resources.configuration)
    configuration.densityDpi = (resources.configuration.densityDpi * scale).toInt()
    return createConfigurationContext(configuration)
}
