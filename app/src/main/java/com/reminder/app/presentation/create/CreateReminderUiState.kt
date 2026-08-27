package com.reminder.app.presentation.create

/**
 * Immutable form state for create/edit. [saved] is a one-shot navigation signal:
 * the screen pops back when it becomes true. [dateMillisUtc] is DatePicker's UTC midnight.
 */
data class CreateReminderUiState(
    val title: String = "",
    val dateMillisUtc: Long? = null,
    val hour: Int = 9,
    val minute: Int = 0,
    val isEdit: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)
