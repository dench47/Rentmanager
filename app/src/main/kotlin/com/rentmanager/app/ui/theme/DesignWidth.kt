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
 *
 * Ширину берём из configuration.screenWidthDp, а не из displayMetrics: на части
 * холодных стартов (например, сразу после установки) метрики приходят
 * полуинициализированными — density=1 при реальном densityDpi — и экран
 * выглядел «шире 412dp», из-за чего масштаб молча отключался и всё становилось крупнее.
 */
private const val DESIGN_WIDTH_DP = 412f

fun Context.createDesignWidthContext(): Context {
    val configuration = resources.configuration
    val metrics = resources.displayMetrics
    val screenWidthDp = configuration.screenWidthDp.takeIf { it > 0 }?.toFloat()
        ?: (if (metrics.density > 0f) metrics.widthPixels / metrics.density else 0f)
    val scale = if (screenWidthDp > 0f) (screenWidthDp / DESIGN_WIDTH_DP).coerceAtMost(1f) else 1f
    if (scale >= 0.999f) return this
    val scaled = Configuration(configuration)
    scaled.densityDpi = (configuration.densityDpi * scale).toInt()
    return createConfigurationContext(scaled)
}
