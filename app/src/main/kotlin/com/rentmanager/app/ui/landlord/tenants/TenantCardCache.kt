package com.rentmanager.app.ui.landlord.tenants

import com.rentmanager.app.data.model.TenantDto

/**
 * Кеш карточки арендатора — правило «отображение сразу» для ВСЕГО экрана:
 *  • список при тапе кладёт [tenant] (имя/телефон/ава мгновенно на первом входе);
 *  • после успешного /tenants/{id}/card кладётся [card] — при повторном входе
 *    первый кадр содержит всё: и арендатора, и «Арендует», и историю.
 */
object TenantCardCache {
    var tenant: TenantDto? = null
        private set

    /** Последний полный ответ /card (арендатор + брони) */
    var card: TenantDto? = null
        private set

    fun put(t: TenantDto) {
        tenant = t
    }

    fun putCard(full: TenantDto) {
        card = full
        tenant = full
    }

    fun takeCardIfMatches(tenantId: String): TenantDto? =
        card?.takeIf { it.id == tenantId }

    fun takeIfMatches(tenantId: String): TenantDto? =
        tenant?.takeIf { it.id == tenantId }
}
