package com.rentmanager.app.util

/**
 * Нормализация и валидация номеров телефонов.
 * Поддерживаемые страны: Россия (+7, 10 цифр), Казахстан (+7, 10 цифр),
 * Беларусь (+375, 9 цифр), Грузия (+995, 9 цифр) и Китай (+86, 11 цифр).
 * Принимает любой формат написания: 89009999999 == +79009999999 == 9009999999.
 */
object PhoneUtils {

    /**
     * Приводит произвольный ввод к каноническому виду:
     * "+7XXXXXXXXXX" (RU/KZ), "+375XXXXXXXXX" (BY), "+995XXXXXXXXX" (GE), "+86XXXXXXXXXXX" (CN).
     * Возвращает null, если формат не удалось распознать.
     */
    fun normalize(raw: String): String? {
        val d = raw.filter { it.isDigit() }
        if (d.isEmpty()) return null
        return when {
            // Беларусь: +375 XX XXX-XX-XX (12 цифр всего) → "+375XXXXXXXXX"
            d.length == 12 && d.startsWith("375") -> "+375" + d.substring(3)
            // Грузия: +995 XXX-XX-XX-XX (12 цифр всего) → "+995XXXXXXXXX"
            d.length == 12 && d.startsWith("995") -> "+995" + d.substring(3)
            // Россия / Казахстан: 7 или 8 + 10 цифр → "+7XXXXXXXXXX"
            d.length == 11 && (d.startsWith("7") || d.startsWith("8")) -> "+7" + d.substring(1)
            // Китай: 1 + 10 цифр → "+861XXXXXXXXXX"
            d.length == 11 && d.startsWith("1") -> "+86" + d
            // Китай: 86 + 11 цифр → "+86XXXXXXXXXXX"
            d.length == 13 && d.startsWith("86") -> "+86" + d.substring(2)
            // Россия: национальный 9xx (10 цифр)
            d.length == 10 && d.startsWith("9") -> "+7" + d
            // Казахстан: национальный 7xx (10 цифр)
            d.length == 10 && d.startsWith("7") -> "+7" + d
            // Беларусь: национальный 9 цифр (мобильные коды 25/29/33/44)
            d.length == 9 && d.take(2) in setOf("25", "29", "33", "44") -> "+375" + d
            // Грузия: национальный 9 цифр (мобильные 5xx)
            d.length == 9 && d.startsWith("5") -> "+995" + d
            else -> null
        }
    }

    /** Возвращает текст ошибки либо null, если номер валиден. */
    fun validate(raw: String): String? {
        val d = raw.filter { it.isDigit() }
        if (d.isEmpty()) return "Введите номер телефона"
        val normalized = normalize(raw)
            ?: return "Неверный номер: проверьте формат (Россия/Казахстан — 10 цифр, Беларусь/Грузия — 9, Китай — 11)"
        return when {
            normalized.startsWith("+375") -> validateBy(normalized.removePrefix("+375"))
            normalized.startsWith("+995") -> validateGe(normalized.removePrefix("+995"))
            normalized.startsWith("+86") -> validateCn(normalized.removePrefix("+86"))
            normalized.startsWith("+7") -> {
                val national = normalized.removePrefix("+7")
                // Казахстан и Россия делят +7: KZ-мобильные начинаются с 7xx, RU — иначе.
                if (national.startsWith("7")) validateKz(national) else validateRu(national)
            }
            else -> null
        }
    }

    private fun validateRu(national: String): String? {
        if (national.length != 10) return "Неверный номер: в российском номере 10 цифр"
        val first = national.first()
        // RU: 9 — мобильный, 3/4 — география, 8 — 800. 5 и прочие недопустимы.
        if (first !in "3489") return "Неверный номер: после +7/8 не может быть цифра $first"
        return null
    }

    private fun validateKz(national: String): String? {
        if (national.length != 10) return "Неверный номер: в казахстанском номере 10 цифр"
        val first = national.first()
        // KZ: мобильные коды операторов — 7xx (700, 701, 702, 705, 707, 747, 771, 777 и т.д.).
        if (first != '7') return "Неверный номер: казахстанский мобильный начинается с 7"
        return null
    }

    private fun validateBy(national: String): String? {
        if (national.length != 9) return "Неверный номер: в белорусском номере 9 цифр"
        val code = national.take(2)
        // BY: коды мобильных операторов — 25, 29, 33, 44.
        if (code !in setOf("25", "29", "33", "44")) {
            return "Неверный номер: код оператора $code не поддерживается (ожидается 25, 29, 33 или 44)"
        }
        return null
    }

    private fun validateGe(national: String): String? {
        if (national.length != 9) return "Неверный номер: в грузинском номере 9 цифр"
        val first = national.first()
        // GE: современные мобильные начинаются с 5 (599, 577, 597 и т.д.).
        if (first != '5') return "Неверный номер: грузинский мобильный начинается с 5"
        return null
    }

    private fun validateCn(national: String): String? {
        if (national.length != 11) return "Неверный номер: в китайском номере 11 цифр"
        if (national.first() != '1') return "Неверный номер: китайский номер начинается с 1"
        return null
    }
}
