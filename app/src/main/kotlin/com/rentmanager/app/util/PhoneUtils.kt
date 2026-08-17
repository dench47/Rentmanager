package com.rentmanager.app.util

/**
 * Нормализация и валидация номеров телефонов.
 * Поддерживаемые страны: Россия (+7, 10 цифр) и Китай (+86, 11 цифр).
 * Принимает любой формат написания: 89009999999 == +79009999999 == 9009999999.
 */
object PhoneUtils {

    /**
     * Приводит произвольный ввод к каноническому виду: "+7XXXXXXXXXX" (RU) или "+86XXXXXXXXXXX" (CN).
     * Возвращает null, если формат не удалось распознать.
     */
    fun normalize(raw: String): String? {
        val d = raw.filter { it.isDigit() }
        if (d.isEmpty()) return null
        return when {
            d.length == 11 && d.startsWith("7") -> "+7" + d.substring(1)
            d.length == 11 && d.startsWith("8") -> "+7" + d.substring(1)
            d.length == 10 && d.startsWith("9") -> "+7" + d
            d.length == 11 && d.startsWith("1") -> "+86" + d
            d.length == 13 && d.startsWith("86") -> "+86" + d.substring(2)
            else -> null
        }
    }

    /** Возвращает текст ошибки либо null, если номер валиден. */
    fun validate(raw: String): String? {
        val d = raw.filter { it.isDigit() }
        if (d.isEmpty()) return "Введите номер телефона"
        val normalized = normalize(raw)
            ?: return "Неверный номер: проверьте количество цифр (Россия — 10, Китай — 11)"
        return when {
            normalized.startsWith("+7") -> validateRu(normalized.removePrefix("+7"))
            normalized.startsWith("+86") -> validateCn(normalized.removePrefix("+86"))
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

    private fun validateCn(national: String): String? {
        if (national.length != 11) return "Неверный номер: в китайском номере 11 цифр"
        if (national.first() != '1') return "Неверный номер: китайский номер начинается с 1"
        return null
    }
}
