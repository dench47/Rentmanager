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
 * НАДЁЖНОСТЬ: единственное число, нужное для расчёта, — пиксельная ширина
 * экрана. Плотность НЕ читается вообще: и configuration.densityDpi, и
 * metrics.density, и xdpi, и screenWidthDp на части холодных стартов приходят
 * мусорными (нулевыми или завышенными), и каждое из них «доказывало», что
 * масштаб не нужен. Целевая плотность = 160 × px / 412 — детерминированно.
 * Источники пикселей (по убыванию надёжности): DisplayManager, дефолтный
 * дисплей WindowManager, метрики ресурсов. Страховка на случай полного провала
 * — isDesignWidthApplied(): пересоздание Activity один раз.
 */
private const val DESIGN_WIDTH_DP = 412f

fun Context.createDesignWidthContext(): Context {
    val widthPx = displayWidthPixels()
    if (widthPx > 0) {
        val targetDpi = (160f * widthPx / DESIGN_WIDTH_DP).toInt()
        if (targetDpi > 0) {
            return scaledContext(resources.configuration, targetDpi)
        }
    }
    return this
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
 * целевой для ТЕКУЩЕЙ ширины окна. На части холодных стартов (например,
 * запуск поверх активного звонка) все источники метрик отдают мусор —
 * контекст создаётся с неверной плотностью, интерфейс «гигантский».
 *
 * Логика самолечения: не «одна попытка на процесс», а до 2 recreate на
 * каждую конкретную ширину окна. Как только ширина окна МЕНЯЕТСЯ (звонок
 * закончился, окно растянулось на весь дисплей) — счётчик сбрасывается и
 * даётся новая попытка. Бесконечный цикл невозможен: при неизменной ширине
 * максимум 2 пересоздания, при успешной проверке счётчик обнуляется.
 * Вызывается из onResume и по возврату фокуса окном — см. MainActivity.
 */
private var lastCheckedWindowWidthPx = 0
private var recreateAttemptsForWidth = 0

fun Activity.ensureDesignWidth() {
    val widthPx = windowRealWidthPx()
    if (widthPx <= 0) return // измерить не смогли — не трогаем
    val targetDpi = (160f * widthPx / DESIGN_WIDTH_DP).toInt()
    if (resources.displayMetrics.densityDpi == targetDpi) {
        lastCheckedWindowWidthPx = widthPx
        recreateAttemptsForWidth = 0
        return
    }
    // Ширина окна изменилась с прошлой неудачной проверки — дисплей
    // «проснулся» — это новое состояние, даём свежие попытки
    if (widthPx != lastCheckedWindowWidthPx) {
        lastCheckedWindowWidthPx = widthPx
        recreateAttemptsForWidth = 0
    }
    if (recreateAttemptsForWidth < 2) {
        recreateAttemptsForWidth++
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
