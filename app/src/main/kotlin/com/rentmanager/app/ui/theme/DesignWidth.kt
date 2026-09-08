package com.rentmanager.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager

/**
 * Дизайнерская ширина макетов Figma (фреймы экранов — 412dp).
 *
 * Масштабирование выполняется на уровне контекста Activity (densityDpi), а не
 * CompositionLocal: тогда масштаб автоматически наследуют ВСЕ окна — экраны,
 * bottom sheet'ы, диалоги, попапы, включая будущие, — потому что их контексты
 * строятся от контекста Activity.
 *
 * ГАРАНТИЯ СХОДИМОСТИ (почему «гигантский экран» больше не может застрять):
 * при создании контекста ширина берётся из DisplayManager, но как только окно
 * существует, авторитетная ширина — его собственные метрики. ensureDesignWidth
 * при несоответствии запоминает ширину окна (forcedWidthPx), и следующий
 * recreate применяет плотность РОВНО под неё — проверка сходится с первой
 * попытки. Мусорные метрики отсекаются клэмпом dpi, а не «доказываются»
 * вторым источником: на части холодных стартов (например, поверх активного
 * звонка) ЛЮБОЙ источник может отдать мусор.
 */
private const val DESIGN_WIDTH_DP = 412f
private const val MIN_DPI = 120
private const val MAX_DPI = 600
private const val MAX_RECREATES_PER_WIDTH = 3

/** Ширина окна, измеренная ensureDesignWidth: приоритетнее любых метрик дисплея. */
private var forcedWidthPx = 0

fun Context.createDesignWidthContext(): Context {
    val widthPx = if (forcedWidthPx > 0) forcedWidthPx else displayWidthPixels()
    val targetDpi = designDpiFor(widthPx) ?: return this
    return scaledContext(resources.configuration, targetDpi)
}

/** Целевой dpi под дизайн-ширину; null — ширина мусорная, не масштабируем. */
private fun designDpiFor(widthPx: Int): Int? {
    if (widthPx <= 0) return null
    val dpi = (160f * widthPx / DESIGN_WIDTH_DP).toInt()
    return if (dpi in MIN_DPI..MAX_DPI) dpi else null
}

private fun Context.scaledContext(configuration: Configuration, targetDpi: Int): Context {
    val scaled = Configuration(configuration)
    scaled.densityDpi = targetDpi
    return createConfigurationContext(scaled)
}

/** Пиксельная ширина экрана: DisplayManager → WindowManager → ресурсы. */
private fun Context.displayWidthPixels(): Int {
    displayMetricsFromDisplayManager()?.let { if (it.widthPixels > 0) return it.widthPixels }
    displayMetricsFromWindowManager()?.let { if (it.widthPixels > 0) return it.widthPixels }
    return try {
        resources.displayMetrics.widthPixels
    } catch (_: Exception) {
        0
    }
}

private fun Context.displayMetricsFromDisplayManager(): DisplayMetrics? = try {
    val context = appContext()
    val dm = context.getSystemService(DisplayManager::class.java)
        ?: (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
    val display: Display? = dm?.getDisplay(Display.DEFAULT_DISPLAY)
    if (display != null) {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        metrics
    } else {
        null
    }
} catch (_: Exception) {
    null
}

private fun Context.displayMetricsFromWindowManager(): DisplayMetrics? = try {
    val context = appContext()
    val wm = context.getSystemService(WindowManager::class.java)
        ?: (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
    @Suppress("DEPRECATION")
    val display = wm?.defaultDisplay
    if (display != null) {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        metrics
    } else {
        null
    }
} catch (_: Exception) {
    null
}

/** Application context живёт дольше всех и инициализирован к attachBaseContext. */
private fun Context.appContext(): Context = try {
    applicationContext ?: this
} catch (_: Exception) {
    this
}

/**
 * Страховка после создания окна: плотность контекста должна совпадать с
 * целевой для ТЕКУЩЕЙ ширины окна. Счётчик recreate — на конкретную ширину:
 * при её смене (звонок закончился, сплит-скрин) сбрасывается и даются свежие
 * попытки. Бесконечный цикл невозможен: при неизменной ширине максимум
 * MAX_RECREATES_PER_WIDTH пересозданий, при успешной проверке счётчик
 * обнуляется, а forcedWidthPx очищается.
 * Вызывается из onResume и по возврату фокуса окном — см. MainActivity.
 */
private var lastCheckedWindowWidthPx = 0
private var recreateAttemptsForWidth = 0

fun Activity.ensureDesignWidth() {
    val widthPx = windowRealWidthPx()
    if (widthPx <= 0) return // измерить не смогли — не трогаем
    val targetDpi = designDpiFor(widthPx) ?: return
    if (resources.displayMetrics.densityDpi == targetDpi) {
        lastCheckedWindowWidthPx = widthPx
        recreateAttemptsForWidth = 0
        forcedWidthPx = 0
        return
    }
    // Ширина окна изменилась с прошлой неудачной проверки — это новое
    // состояние, даём свежие попытки
    if (widthPx != lastCheckedWindowWidthPx) {
        lastCheckedWindowWidthPx = widthPx
        recreateAttemptsForWidth = 0
    }
    if (recreateAttemptsForWidth < MAX_RECREATES_PER_WIDTH) {
        recreateAttemptsForWidth++
        // Ключ сходимости: следующий attachBaseContext применит плотность
        // ровно под измеренную ширину окна, а не под метрики дисплея,
        // которые могут быть мусорными — проверка пройдёт с первого раза
        forcedWidthPx = widthPx
        recreate()
    }
}

/** Физическая ширина окна Activity (надёжно начиная с onCreate). */
private fun Activity.windowRealWidthPx(): Int = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds.width()
    } else {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        metrics.widthPixels
    }
} catch (_: Exception) {
    0
}
