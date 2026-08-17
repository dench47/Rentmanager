package com.rentmanager.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Персистентный кэш статистики экрана роли (арендодатель/арендатор).
 * Позволяет показывать «Ближайшее поступление/платёж» и «Доход по объектам»
 * мгновенно после полного перезапуска приложения, пока фоном грузятся свежие данные.
 */
@Singleton
class RoleStatsCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("role_stats", Context.MODE_PRIVATE)

    data class Stats(
        val hasDeals: Boolean,
        val hasDebt: Boolean,
        val nextPaymentDate: String,
        val nextPaymentAmount: String,
        val monthlyIncome: String
    )

    fun load(key: String): Stats? {
        if (!prefs.getBoolean("${key}_saved", false)) return null
        return Stats(
            hasDeals = prefs.getBoolean("${key}_has_deals", false),
            hasDebt = prefs.getBoolean("${key}_has_debt", false),
            nextPaymentDate = prefs.getString("${key}_next_date", "") ?: "",
            nextPaymentAmount = prefs.getString("${key}_next_amount", "") ?: "",
            monthlyIncome = prefs.getString("${key}_income", "") ?: ""
        )
    }

    fun save(key: String, stats: Stats) {
        prefs.edit()
            .putBoolean("${key}_saved", true)
            .putBoolean("${key}_has_deals", stats.hasDeals)
            .putBoolean("${key}_has_debt", stats.hasDebt)
            .putString("${key}_next_date", stats.nextPaymentDate)
            .putString("${key}_next_amount", stats.nextPaymentAmount)
            .putString("${key}_income", stats.monthlyIncome)
            .apply()
    }
}
