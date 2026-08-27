package com.reminder.app.presentation.reminderlist

import com.reminder.app.domain.model.Reminder

/**
 * Immutable list state. The ViewModel exposes this through StateFlow; Compose never
 * mutates it directly.
 */
data class ReminderListUiState(
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
