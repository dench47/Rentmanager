package com.rentmanager.app.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * События арендатора: сигнал обновить список объектов при push-уведомлениях
 * («вас добавили в объект» / «вас удалили из объекта»).
 *
 * FCM-сервис инкрементирует [refreshTick], а экран/ViewModel арендатора
 * слушают его и перезапрашивают данные.
 */
@Singleton
class TenantEvents @Inject constructor() {

    private val _refreshTick = MutableStateFlow(0L)
    val refreshTick: StateFlow<Long> = _refreshTick.asStateFlow()

    fun notifyChanged() {
        _refreshTick.value = _refreshTick.value + 1
    }
}
