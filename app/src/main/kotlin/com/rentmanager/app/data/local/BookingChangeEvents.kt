package com.rentmanager.app.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Правки броней, сделанные на других экранах.
 *
 * Прикрепление арендатора создаёт бронь на сервере, а шахматка «Моя
 * недвижимость» узнавала об этом только по ON_RESUME: возврат на экран ждал
 * полного refresh (запрос списка объектов + брони каждого объекта), поэтому
 * размеченный период оставался серым ещё секунду-две. Экран прикрепления
 * эмитит событие — шахматка сразу дорисовывает период у себя, а refresh
 * следом сверяется с сервером (тот склеивает периоды по своим правилам).
 */
@Singleton
class BookingChangeEvents @Inject constructor() {

    /** На объекте появился период [start]..[end] с арендатором [tenantId]. */
    data class BookingAdded(
        val propertyId: String,
        val start: LocalDate,
        val end: LocalDate,
        val tenantId: String?
    )

    private val _added = MutableSharedFlow<BookingAdded>(extraBufferCapacity = 8)
    val added: SharedFlow<BookingAdded> = _added

    fun emitAdded(propertyId: String, start: LocalDate, end: LocalDate, tenantId: String?) {
        _added.tryEmit(BookingAdded(propertyId, start, end, tenantId))
    }
}
