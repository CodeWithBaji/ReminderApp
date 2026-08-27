package com.reminder.app.presentation.reminderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.domain.usecase.CompleteReminderUseCase
import com.reminder.app.domain.usecase.DeleteReminderUseCase
import com.reminder.app.domain.usecase.GetRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Observes Room via Flow. Survives configuration change (ViewModel) and process
 * death (Room is re-collected on the next process). Mutable state stays private.
 */
@HiltViewModel
class ReminderListViewModel @Inject constructor(
    getReminders: GetRemindersUseCase,
    private val completeReminder: CompleteReminderUseCase,
    private val deleteReminder: DeleteReminderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getReminders()
                .catch { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.message) }
                }
                .collect { reminders ->
                    _uiState.update {
                        it.copy(reminders = reminders, isLoading = false, error = null)
                    }
                }
        }
    }

    /** PENDING → COMPLETED and cancel the alarm (safe if it already fired). */
    fun complete(id: Long) {
        viewModelScope.launch { completeReminder(id) }
    }

    /** PENDING → CANCELLED and cancel the alarm. The Room row is kept for history. */
    fun cancel(id: Long) {
        viewModelScope.launch { deleteReminder(id) }
    }
}
