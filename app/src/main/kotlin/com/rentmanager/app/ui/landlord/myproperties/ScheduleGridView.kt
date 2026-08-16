package com.rentmanager.app.ui.landlord.myproperties

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/**
 * Сетка занятости (шахматка), рисуется одним View в onDraw.
 * Ячейки фиксированные 44x39dp, gap 4dp, радиус 4dp, текст 11sp/9sp/14sp.
 */
class ScheduleGridView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val cellW = 44f * density
    private val cellH = 39f * density
    private val gap = 4f * density
    private val radius = 4f * density

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = 0xFF007AFF.toInt()
    }
    private val monthPaint = textPaint(11f)
    private val dayPaint = textPaint(14f)
    private val weekdayPaint = textPaint(9f)

    private var labels: List<String> = emptyList()
    private var topLabels: List<String?> = emptyList()
    private var states: List<String> = emptyList()
    private var twoLine = false

    private var selectionStart: Int? = null
    private var selectionEnd: Int? = null

    private fun textPaint(sp: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp * scaledDensity
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    fun setData(
        labels: List<String>,
        topLabels: List<String?>,
        states: List<String>,
        twoLine: Boolean
    ) {
        this.labels = labels
        this.topLabels = topLabels
        this.states = states
        this.twoLine = twoLine
        requestLayout()
        invalidate()
    }

    fun setSelection(start: Int?, end: Int?) {
        selectionStart = start
        selectionEnd = end
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val count = labels.size
        val w = (count * cellW + (count - 1).coerceAtLeast(0) * gap).toInt()
        setMeasuredDimension(w, cellH.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val selLo = selectionStart?.let { s -> selectionEnd?.let { e -> minOf(s, e) } ?: s }
        val selHi = selectionStart?.let { s -> selectionEnd?.let { e -> maxOf(s, e) } ?: s }
        var x = 0f
        for (i in labels.indices) {
            val (bg, stroke, text) = colorsFor(states.getOrElse(i) { "" })
            val rect = RectF(x, 0f, x + cellW, cellH)
            bgPaint.color = bg
            strokePaint.color = stroke
            canvas.drawRoundRect(rect, radius, radius, bgPaint)
            canvas.drawRoundRect(rect, radius, radius, strokePaint)

            if (selLo != null && selHi != null && i in selLo..selHi) {
                canvas.drawRoundRect(rect, radius, radius, highlightPaint)
            }

            val top = topLabels.getOrNull(i)
            if (twoLine && top != null) {
                drawTwoLine(canvas, rect, top, labels[i], text)
            } else {
                val paint = if (twoLine) dayPaint else monthPaint
                paint.color = text
                val fm = paint.fontMetrics
                val baseline = rect.top + (rect.height() - (fm.descent - fm.ascent)) / 2f - fm.ascent
                canvas.drawText(labels[i], rect.centerX(), baseline, paint)
            }
            x += cellW + gap
        }
    }

    private fun drawTwoLine(canvas: Canvas, rect: RectF, weekday: String, day: String, color: Int) {
        weekdayPaint.color = color
        dayPaint.color = color
        val overlap = 2f * density
        val fm1 = weekdayPaint.fontMetrics
        val fm2 = dayPaint.fontMetrics
        val h1 = fm1.descent - fm1.ascent
        val h2 = fm2.descent - fm2.ascent
        val totalH = h1 + h2 - overlap
        val startY = rect.top + (rect.height() - totalH) / 2f
        canvas.drawText(weekday, rect.centerX(), startY - fm1.ascent, weekdayPaint)
        canvas.drawText(day, rect.centerX(), startY + h1 - overlap - fm2.ascent, dayPaint)
    }

    private fun colorsFor(state: String): Triple<Int, Int, Int> = when (state) {
        "expired" -> Triple(0xFFCFDECB.toInt(), 0xFFFF4249.toInt(), 0xFFFF4249.toInt())
        "free" -> Triple(0xFFEFEFEF.toInt(), 0xFF727272.toInt(), 0xFF727272.toInt())
        else -> Triple(0xFFCFDECB.toInt(), 0xFF66A256.toInt(), 0xFF212121.toInt())
    }
}
