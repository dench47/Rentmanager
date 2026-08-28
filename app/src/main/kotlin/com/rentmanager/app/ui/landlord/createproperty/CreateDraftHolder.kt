package com.rentmanager.app.ui.landlord.createproperty

/**
 * Черновик флоу создания объекта.
 *
 * Сохраняется при навигации назад/вперёд по шагам 1–4 и при выходе из флоу
 * через крестик. При следующем входе в «Создать объект» шаг 1 показывает
 * диалог «Продолжить создание?» (восстановить черновик) или «Начать заново».
 */
object CreateDraftHolder {
    var propertyType: String = "Квартира"
    var rentType: String = "посуточно"
    var address: String = ""
    var latitude: Double? = null
    var longitude: Double? = null

    var name: String = ""
    var area: String = ""
    var price: String = ""
    var description: String = ""
    var rooms: String? = null
    var sleepingPlaces: String? = null
    var floor: String? = null
    var floorsInHouse: String? = null
    var photoUris: List<String> = emptyList()
    var phoneNumber: String = ""
    var wifiPassword: String = ""
    var rulesText: String = ""
    var serviceInfo: String = ""

    private var entryRequested = false
    private var autoContinue = false

    /** Черновик существует, если заполнено хотя бы одно поле шага 4 */
    fun hasDraft(): Boolean =
        name.isNotBlank() || area.isNotBlank() || price.isNotBlank() || description.isNotBlank() ||
            rooms != null || sleepingPlaces != null || floor != null || floorsInHouse != null ||
            photoUris.isNotEmpty() || phoneNumber.isNotBlank() || wifiPassword.isNotBlank() ||
            rulesText.isNotBlank() || serviceInfo.isNotBlank()

    /** Пометить вход в флоу извне (кнопка «Создать»), чтобы шаг 1 знал о проверке черновика */
    fun markEntryRequested() {
        entryRequested = true
    }

    /** Вернуть и сбросить флаг входа (одноразовое потребление) */
    fun consumeEntryRequested(): Boolean {
        val wasRequested = entryRequested
        entryRequested = false
        return wasRequested
    }

    /** Шаг 3 должен сразу продолжить на шаг 4 (после диалога «Продолжить создание?») */
    fun markAutoContinue() {
        autoContinue = true
    }

    fun consumeAutoContinue(): Boolean {
        val wasRequested = autoContinue
        autoContinue = false
        return wasRequested
    }

    fun clear() {
        propertyType = "Квартира"
        rentType = "посуточно"
        address = ""
        latitude = null
        longitude = null
        name = ""
        area = ""
        price = ""
        description = ""
        rooms = null
        sleepingPlaces = null
        floor = null
        floorsInHouse = null
        photoUris = emptyList()
        phoneNumber = ""
        wifiPassword = ""
        rulesText = ""
        serviceInfo = ""
        entryRequested = false
        autoContinue = false
    }

    /** Снимок полей шага 4 — чтобы режим редактирования объекта не портил черновик создания. */
    fun snapshot(): Map<String, Any?> = mapOf(
        "name" to name,
        "area" to area,
        "price" to price,
        "description" to description,
        "rooms" to rooms,
        "sleepingPlaces" to sleepingPlaces,
        "floor" to floor,
        "floorsInHouse" to floorsInHouse,
        "photoUris" to photoUris,
        "phoneNumber" to phoneNumber,
        "wifiPassword" to wifiPassword,
        "rulesText" to rulesText,
        "serviceInfo" to serviceInfo
    )

    /** Восстановление полей шага 4 из снимка [snapshot]. */
    fun restore(snapshot: Map<String, Any?>) {
        name = snapshot["name"] as? String ?: ""
        area = snapshot["area"] as? String ?: ""
        price = snapshot["price"] as? String ?: ""
        description = snapshot["description"] as? String ?: ""
        rooms = snapshot["rooms"] as? String
        sleepingPlaces = snapshot["sleepingPlaces"] as? String
        floor = snapshot["floor"] as? String
        floorsInHouse = snapshot["floorsInHouse"] as? String
        photoUris = (snapshot["photoUris"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        phoneNumber = snapshot["phoneNumber"] as? String ?: ""
        wifiPassword = snapshot["wifiPassword"] as? String ?: ""
        rulesText = snapshot["rulesText"] as? String ?: ""
        serviceInfo = snapshot["serviceInfo"] as? String ?: ""
    }
}
