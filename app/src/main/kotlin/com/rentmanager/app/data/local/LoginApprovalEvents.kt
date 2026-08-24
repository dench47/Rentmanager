package com.rentmanager.app.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * События запросов подтверждения входа (Device Trust).
 * Push type=login_request приходит на доверенное устройство —
 * сервис кладёт событие сюда, MainActivity показывает диалог «Подтвердить/Отклонить».
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

    private val _requests = MutableSharedFlow<LoginRequest>(extraBufferCapacity = 4)
    val requests: SharedFlow<LoginRequest> = _requests

    private val _devicesChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val devicesChanged: SharedFlow<Unit> = _devicesChanged

    fun emit(requestId: String?, deviceName: String?) {
        if (!requestId.isNullOrBlank()) {
            _requests.tryEmit(LoginRequest(requestId, deviceName))
        }
    }

    fun emitDevicesChanged() {
        _devicesChanged.tryEmit(Unit)
    }
}
