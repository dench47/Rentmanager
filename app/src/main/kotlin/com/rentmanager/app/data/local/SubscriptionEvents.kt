package com.rentmanager.app.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * События подписки: FCM-сигнал «баланс/операции изменились» (тип
 * subscription_changed). Сервер шлёт его в момент списания, блокировки
 * и разблокировки — вместо поллинга: открытые экраны подписки и истории
 * перечитывают данные по событию (1–2 пуша в день, ноль фоновой нагрузки).
 */
@Singleton
class SubscriptionEvents @Inject constructor() {

    private val _refreshTick = MutableStateFlow(0L)
    val refreshTick: StateFlow<Long> = _refreshTick.asStateFlow()

    fun notifyChanged() {
        _refreshTick.value = _refreshTick.value + 1
    }
}
