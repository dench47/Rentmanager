package com.rentmanager.app.util

import com.google.gson.Gson
import com.rentmanager.app.data.model.PaymentDto
import com.rentmanager.app.data.model.PaymentScheduleDto
import java.time.LocalDate

/**
 * Расчёт просрочки по графику платежей и фактическим платежам.
 * Просрочка = дата платежа наступила, а оплаченного платежа за период нет.
 */
object PaymentOverdue {

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

    private fun hasOverdueCustom(customDatesJson: String, payments: List<PaymentDto>, today: LocalDate): Boolean {
        val dueDates = try {
            val arr = Gson().fromJson(customDatesJson, Array<CustomPaymentDate>::class.java)
            arr.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        } catch (_: Exception) {
            emptyList()
        }
        val paidDates = payments.filter { it.status == "paid" }.map { it.date }
        return dueDates.any { due ->
            !due.isAfter(today) && paidDates.none { it >= due.toString() }
        }
    }

    private data class CustomPaymentDate(val date: String, val amount: String)
}
