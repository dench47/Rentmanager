package com.rentmanager.app.ui.counter.add

/**
 * Брошенная форма «Добавить счетчик» (не сохранили и вышли).
 * При повторном входе на экран показывается шит «Добавление счетчика —
 * Продолжить / Начать заново» (Figma 2761-43441).
 */
object CounterFormDraft {
    var fields: AddCounterUiState? = null
        private set

    /** Запоминает форму, если в ней хоть что-то заполнено. */
    fun save(state: AddCounterUiState) {
        val dirty = state.counterType.isNotBlank() ||
            state.counterNumber.isNotBlank() ||
            state.initialValue.isNotBlank() ||
            state.nextVerificationDate.isNotBlank() ||
            state.submitReadingsBy.isNotBlank() ||
            state.remindVerification ||
            state.remindReadings
        fields = if (dirty) state.copy(isTypeDropdownOpen = false, isSaving = false) else null
    }

    /** Забрать и сбросить; null — черновика нет. */
    fun consume(): AddCounterUiState? {
        val f = fields
        fields = null
        return f
    }
}
