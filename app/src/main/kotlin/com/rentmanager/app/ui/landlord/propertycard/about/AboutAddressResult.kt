package com.rentmanager.app.ui.landlord.propertycard.about

/**
 * Результат выбора адреса с карты (экран «Об объекте» → полноэкранный выбор,
 * как на шаге 3 создания, но без прогресс-бара). Одноразовый: чтение очищает.
 */
object AboutAddressResult {
    var address: String? = null
        private set
    var latitude: Double? = null
        private set
    var longitude: Double? = null
        private set

    fun set(address: String, latitude: Double?, longitude: Double?) {
        this.address = address
        this.latitude = latitude
        this.longitude = longitude
    }

    /** Вернуть и сбросить; null — результата нет. */
    fun consume(): Triple<String, Double?, Double?>? {
        val a = address ?: return null
        val result = Triple(a, latitude, longitude)
        address = null
        latitude = null
        longitude = null
        return result
    }
}
