package com.reminder.app.domain.usecase

import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Thin wrapper so the UI depends on a use case, not the repository directly. */
class GetRemindersUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    operator fun invoke(): Flow<List<Reminder>> = repository.observeReminders()
}
