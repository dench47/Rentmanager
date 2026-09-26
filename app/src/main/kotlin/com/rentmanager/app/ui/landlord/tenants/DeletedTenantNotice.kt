package com.rentmanager.app.ui.landlord.tenants

/**
 * Уведомление «Арендатор был удален» через экраны: карточка после успешного
 * удаления уходит назад, а диалог с «Отменить удаление» (3014:22433)
 * показывается уже НА ФОНЕ списка «Арендаторы».
 */
object DeletedTenantNotice {
    var tenantId: String? = null
        private set

    fun set(id: String) {
        tenantId = id
    }

    fun take(): String? {
        val id = tenantId
        tenantId = null
        return id
    }
}
