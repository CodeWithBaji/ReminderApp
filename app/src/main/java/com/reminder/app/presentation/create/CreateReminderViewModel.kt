package com.reminder.app.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.domain.usecase.CreateReminderResult
import com.reminder.app.domain.usecase.CreateReminderUseCase
import com.reminder.app.domain.usecase.UpdateReminderResult
import com.reminder.app.domain.usecase.UpdateReminderUseCase
import com.reminder.app.domain.repository.ReminderRepository
import com.reminder.app.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Holds create/edit form state. Loads an existing row when [ARG_REMINDER_ID] is set
 * (navigation from a PENDING card). Save goes through create or update use cases so
 * Room is written before any alarm change.
 */
@HiltViewModel
class CreateReminderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createReminder: CreateReminderUseCase,
    private val updateReminder: UpdateReminderUseCase,
    private val repository: ReminderRepository
) : ViewModel() {

    private val reminderId: Long = savedStateHandle.get<Long>(ARG_REMINDER_ID) ?: NEW_REMINDER_ID

    private val _uiState = MutableStateFlow(defaultState())
    val uiState = _uiState.asStateFlow()

    init {
        if (reminderId != NEW_REMINDER_ID) {
            viewModelScope.launch {
                val existing = repository.getReminder(reminderId) ?: return@launch
                val cal = Calendar.getInstance().apply { timeInMillis = existing.triggerTimeMillis }
                _uiState.update {
                    it.copy(
                        title = existing.title,
                        dateMillisUtc = DateTimeUtils.utcDateMillisOf(existing.triggerTimeMillis),
                        hour = cal.get(Calendar.HOUR_OF_DAY),
                        minute = cal.get(Calendar.MINUTE),
                        isEdit = true
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, error = null) }
    }

    /** [utcMillis] is the DatePicker selection (UTC midnight for that calendar day). */
    fun onDateSelected(utcMillis: Long) {
        _uiState.update { it.copy(dateMillisUtc = utcMillis, error = null) }
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        _uiState.update { it.copy(hour = hour, minute = minute, error = null) }
    }

    /**
     * Persists then schedules. Rejects blank titles and times that are not in the future.
     * On success sets [CreateReminderUiState.saved] so the screen can pop.
     */
    fun save() {
        val state = _uiState.value
        val dateMillis = state.dateMillisUtc
        if (dateMillis == null) {
            _uiState.update { it.copy(error = "Pick a date") }
            return
        }
        val trigger = DateTimeUtils.combineUtcDateAndLocalTime(dateMillis, state.hour, state.minute)
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val error = if (state.isEdit) {
                when (updateReminder(reminderId, state.title, trigger)) {
                    is UpdateReminderResult.Success -> null
                    UpdateReminderResult.BlankTitle -> "Title cannot be blank"
                    UpdateReminderResult.TimeInPast -> "Time must be in the future"
                    UpdateReminderResult.NotFound -> "Reminder not found"
                    UpdateReminderResult.NotPending -> "Only pending reminders can be edited"
                }
            } else {
                when (createReminder(state.title, trigger)) {
                    is CreateReminderResult.Success -> null
                    CreateReminderResult.BlankTitle -> "Title cannot be blank"
                    CreateReminderResult.TimeInPast -> "Time must be in the future"
                }
            }
            _uiState.update {
                it.copy(isSaving = false, error = error, saved = error == null)
            }
        }
    }

    private fun defaultState(): CreateReminderUiState {
        val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        return CreateReminderUiState(
            dateMillisUtc = DateTimeUtils.utcDateMillisOf(cal.timeInMillis),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE)
        )
    }

    companion object {
        /** Navigation argument. [NEW_REMINDER_ID] means create, any other value means edit. */
        const val ARG_REMINDER_ID = "reminderId"
        const val NEW_REMINDER_ID = -1L
    }
}
