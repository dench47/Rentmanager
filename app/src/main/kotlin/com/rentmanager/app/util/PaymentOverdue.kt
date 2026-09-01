package com.rentmanager.app.util

import com.google.gson.Gson
import com.rentmanager.app.data.model.PaymentDto
import com.rentmanager.app.data.model.PaymentScheduleDto
import java.time.LocalDate
import java.time.YearMonth

/**
 * Расчёт платежей: просрочка, ближайший платёж, суммарный месячный доход.
 */
object PaymentOverdue {

    data class NextPayment(val date: LocalDate?, val amount: Double?)

    fun isOverdue(
        schedule: PaymentScheduleDto?,
        payments: List<PaymentDto>,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        val s = schedule ?: return false
        val monthPrefix = "%d-%02d".format(today.year, today.monthValue)
        val paidThisMonth = payments.any { it.status == "paid" && it.date.startsWith(monthPrefix) }
        return when {
            s.dayOfMonth != null -> today.dayOfMonth >= s.dayOfMonth && !paidThisMonth
            s.customDates != null -> hasOverdueCustom(s.customDates, payments, today)
            else -> false
        }
    }

    /** Ближайшая предстоящая дата платежа и сумма. */
    fun nextPayment(schedule: PaymentScheduleDto?, today: LocalDate = LocalDate.now()): NextPayment {
        val s = schedule ?: return NextPayment(null, null)
        return when {
            s.dayOfMonth != null -> {
                val ym = YearMonth.of(today.year, today.monthValue)
                val day = s.dayOfMonth.coerceIn(1, ym.lengthOfMonth())
                var candidate = ym.atDay(day)
                if (candidate.isBefore(today)) candidate = candidate.plusMonths(1)
                NextPayment(candidate, s.amount)
            }
            s.customDates != null -> {
                // Даты в custom_dates хранятся в формате дд.ММ.гггг
                val ddMMyyyy = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
                val items = parseCustomDates(s.customDates)
                    .mapNotNull { entry ->
                        runCatching { LocalDate.parse(entry.date, ddMMyyyy) }.getOrNull()
                            ?.let { it to entry.amount }
                    }
                // Ближайшая — только относительно сегодняшней даты (прошедшие не берём)
                val upcoming = items.filter { !it.first.isBefore(today) }.minByOrNull { it.first }
                NextPayment(upcoming?.first, upcoming?.second?.toDoubleOrNull())
            }
            else -> NextPayment(null, s.amount)
        }
    }

    /** Месячная сумма по графику (фикс — amount, переменный — сумма дат). */
    fun monthlyAmount(schedule: PaymentScheduleDto): Double = when {
        schedule.amount != null -> schedule.amount
        schedule.customDates != null -> parseCustomDates(schedule.customDates).sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        else -> 0.0
    }

    /** Сумма просроченного к оплате (для плашки «Задолженность X ₽» на дашборде). */
    fun overdueAmount(
        schedule: PaymentScheduleDto?,
        payments: List<PaymentDto>,
        today: LocalDate = LocalDate.now()
    ): Double {
        val s = schedule ?: return 0.0
        return when {
            s.dayOfMonth != null -> if (isOverdue(s, payments, today)) s.amount ?: 0.0 else 0.0
            s.customDates != null -> {
                val ddMMyyyy = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
                val dueDates = parseCustomDates(s.customDates)
                    .mapNotNull { entry ->
                        runCatching { LocalDate.parse(entry.date, ddMMyyyy) }.getOrNull()
                            ?.let { it to (entry.amount.toDoubleOrNull() ?: 0.0) }
                    }
                val paidDates = payments.filter { it.status == "paid" }.map { it.date }
                dueDates.filter { (due, _) ->
                    !due.isAfter(today) && paidDates.none { it >= due.toString() }
                }.sumOf { it.second }
            }
            else -> 0.0
        }
    }

    private fun hasOverdueCustom(customDatesJson: String, payments: List<PaymentDto>, today: LocalDate): Boolean {
        val ddMMyyyy = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val dueDates = parseCustomDates(customDatesJson)
            .mapNotNull { runCatching { LocalDate.parse(it.date, ddMMyyyy) }.getOrNull() }
        val paidDates = payments.filter { it.status == "paid" }.map { it.date }
        return dueDates.any { due ->
            !due.isAfter(today) && paidDates.none { it >= due.toString() }
        }
    }

    private fun parseCustomDates(json: String): List<CustomPaymentDate> = try {
        Gson().fromJson(json, Array<CustomPaymentDate>::class.java).toList()
    } catch (_: Exception) {
        emptyList()
    }

    private data class CustomPaymentDate(val date: String, val amount: String)
}
