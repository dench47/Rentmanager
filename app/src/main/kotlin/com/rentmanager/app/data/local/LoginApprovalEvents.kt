package com.rentmanager.app.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * События запросов подтверждения входа (Device Trust).
 * Push type=login_request приходит на доверенное устройство —
 * сервис кладёт событие сюда, MainActivity показывает диалог «Подтвердить/Отклонить».
 *
 * requests — StateFlow: хранит последний pending-запрос, чтобы диалог
 * появлялся даже при холодном старте (когда push пришёл, пока приложение было в фоне/убито).
 *
 * devicesChanged — сигнал об изменении списка доверенных устройств:
 * эмитится локально после approve/deny и по серверному push type=devices_changed.
 * Экран «Безопасность» слушает его и мгновенно перезагружает список.
 */
@Singleton
class LoginApprovalEvents @Inject constructor() {
    data class LoginRequest(
        val requestId: String,
        val deviceName: String?
    )

    private val _requests = MutableStateFlow<LoginRequest?>(null)
    val requests: StateFlow<LoginRequest?> = _requests.asStateFlow()

    private val _devicesChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val devicesChanged: SharedFlow<Unit> = _devicesChanged

    fun emit(requestId: String?, deviceName: String?) {
        if (!requestId.isNullOrBlank()) {
            _requests.value = LoginRequest(requestId, deviceName)
        }
    }

    fun emitDevicesChanged() {
        _requests.value = null
        _devicesChanged.tryEmit(Unit)
    }

    /** Сбрасывает ожидающий запрос (после approve/deny). */
    fun clearPending() {
        _requests.value = null
    }
}
