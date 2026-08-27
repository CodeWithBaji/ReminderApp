package com.reminder.app.domain.usecase

import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.domain.repository.ReminderRepository
import com.reminder.app.scheduler.ReminderScheduler
import javax.inject.Inject

/**
 * Marks a reminder completed and cancels any still-pending alarm.
 *
 * Completing before the trigger time should not leave a live alarm behind.
 * Completing after the notification has already shown is also safe: cancel() is
 * idempotent if the PendingIntent no longer exists.
 */
class CompleteReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler
) {
    /** No-op if the id is unknown. */
    suspend operator fun invoke(id: Long) {
        val reminder = repository.getReminder(id) ?: return
        repository.updateStatus(id, ReminderStatus.COMPLETED)
        scheduler.cancel(reminder)
    }
}
