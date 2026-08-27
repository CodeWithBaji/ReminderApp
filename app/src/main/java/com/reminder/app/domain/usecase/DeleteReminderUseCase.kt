package com.reminder.app.domain.usecase

import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.domain.repository.ReminderRepository
import com.reminder.app.scheduler.ReminderScheduler
import javax.inject.Inject

/**
 * Cancels a reminder: Room status becomes [ReminderStatus.CANCELLED], then the
 * matching AlarmManager PendingIntent is cancelled.
 *
 * Room is updated first so a late-firing alarm (already in the OS queue) will see
 * CANCELLED in [com.reminder.app.receiver.ReminderReceiver] and skip the notification.
 *
 * Named "Delete" to match the architecture outline; the row is kept so the list can
 * show history. True deletion is unnecessary for this learning app.
 */
class DeleteReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler
) {
    /** No-op if the id is unknown. */
    suspend operator fun invoke(id: Long) {
        val reminder = repository.getReminder(id) ?: return
        repository.updateStatus(id, ReminderStatus.CANCELLED)
        scheduler.cancel(reminder)
    }
}
