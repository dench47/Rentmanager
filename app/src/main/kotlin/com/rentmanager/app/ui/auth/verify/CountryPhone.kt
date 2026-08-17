package com.rentmanager.app.ui.auth.verify

/**
 * Модель страны с телефонным префиксом и параметрами маски.
 *
 * @param countryCode ISO-код страны (RU, CN, ...)
 * @param flagEmoji эмодзи флага
 * @param phonePrefix международный префикс (например "+7")
 * @param maxDigits максимальное количество цифр номера (без префикса)
 * @param maskPattern описание групп маски для PhoneMaskTransformation:
 *        список длин групп цифр; например listOf(3, 3, 2, 2) → (XXX) XXX-XX-XX
 */
data class CountryPhone(
    val countryCode: String,
    val displayName: String,
    val flagEmoji: String,
    val phonePrefix: String,
    val maxDigits: Int,
    val maskPattern: List<Int>
) {
    companion object {
        val Russia = CountryPhone(
            countryCode = "RU",
            displayName = "Россия",
            flagEmoji = "🇷🇺",
            phonePrefix = "+7",
            maxDigits = 10,
            maskPattern = listOf(3, 3, 2, 2)
        )

        val China = CountryPhone(
            countryCode = "CN",
            displayName = "Китай",
            flagEmoji = "🇨🇳",
            phonePrefix = "+86",
            maxDigits = 11,
            maskPattern = listOf(3, 4, 4)
        )

        val Kazakhstan = CountryPhone(
            countryCode = "KZ",
            displayName = "Казахстан",
            flagEmoji = "🇰🇿",
            phonePrefix = "+7",
            maxDigits = 10,
            maskPattern = listOf(3, 3, 2, 2)
        )

        val Belarus = CountryPhone(
            countryCode = "BY",
            displayName = "Беларусь",
            flagEmoji = "🇧🇾",
            phonePrefix = "+375",
            maxDigits = 9,
            maskPattern = listOf(2, 3, 2, 2)
        )

        val Georgia = CountryPhone(
            countryCode = "GE",
            displayName = "Грузия",
            flagEmoji = "🇬🇪",
            phonePrefix = "+995",
            maxDigits = 9,
            maskPattern = listOf(3, 2, 2, 2)
        )

        val availableCountries = listOf(Russia, Kazakhstan, Belarus, Georgia, China)

        val defaultCountry = Russia
    }
}