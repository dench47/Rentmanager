package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

/** Реквизиты арендодателя для приёма платежей (общие на аккаунт). */
data class PaymentRequisiteDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("account") val account: String = "",
    @SerializedName("bank") val bank: String = "",
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    /** Подпись второй строки в шите выбора и поле «Реквизиты»: «Счёт ••4081 · Точка» */
    val accountCaption: String
        get() = buildList {
            if (account.isNotBlank()) add("Счёт ••${account.takeLast(4)}")
            if (bank.isNotBlank()) add(bank)
        }.joinToString(" · ").ifEmpty { "Личные реквизиты" }
}
