package com.rentmanager.app.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Слияние пересекающихся/соприкасающихся периодов броней в непересекающиеся.
 * Перекраски шахматки могут создавать накладывающиеся брони — слияние
 * не даёт суткам задваиваться в списках графика и в суммах.
 */
fun mergeRanges(ranges: List<Pair<LocalDate, LocalDate>>): List<Pair<LocalDate, LocalDate>> {
    if (ranges.isEmpty()) return emptyList()
    val sorted = ranges.sortedBy { it.first }
    val result = mutableListOf(sorted.first())
    for ((s, e) in sorted.drop(1)) {
        val last = result.last()
        if (!s.isAfter(last.second.plusDays(1))) {
            result[result.lastIndex] = last.first to maxOf(last.second, e)
        } else {
            result.add(s to e)
        }
    }
    return result
}

/** Сутки периода, пересекающиеся с окном [from, to] (каждые сутки — один раз). */
fun overlapDays(start: LocalDate, end: LocalDate, from: LocalDate, to: LocalDate): Long {
    val f = maxOf(start, from)
    val t = minOf(end, to)
    return if (t.isBefore(f)) 0 else ChronoUnit.DAYS.between(f, t) + 1
}
