package com.rentmanager.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Единственный формат даты в интерфейсе — ДД.ММ.ГГГГ.
 * ISO («гггг-мм-дд») в UI не показываем никогда: сервер отдаёт даты в ISO,
 * а поля, которые правит пользователь, хранятся как «21.11.2026».
 */
val DD_MM_YYYY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private val ISO_DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
private val DOT_DATE = Regex("^\\d{2}\\.\\d{2}\\.\\d{4}$")

/** Дата из любого источника: ISO («2026-11-21») или «21.11.2026». */
fun parseAnyDate(raw: String?): LocalDate? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    if (DOT_DATE.matches(value)) return runCatching { LocalDate.parse(value, DD_MM_YYYY) }.getOrNull()
    if (ISO_DATE.matches(value)) return runCatching { LocalDate.parse(value) }.getOrNull()
    return null
}

/**
 * Дата для показа: всегда ДД.ММ.ГГГГ. Если значение не распознано как дата
 * (например, пользователь ввёл произвольный текст) — возвращаем как есть.
 */
fun formatDdMmYyyy(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return ""
    return parseAnyDate(value)?.format(DD_MM_YYYY) ?: value
}

/**
 * «Срок аренды» объекта — один источник для карточки объекта, списка
 * «Моя недвижимость» и карточки арендатора: поле объекта (его правит шит
 * «Аренда и платежи»), а если оно пустое — последняя бронь объекта.
 * Именно поэтому срок одинаков везде и не остаётся пустым там, где есть бронь.
 */
fun leaseEnd(rentEndDate: String?, bookingEnds: List<LocalDate>): LocalDate? =
    parseAnyDate(rentEndDate) ?: bookingEnds.maxOrNull()
