package com.reminder.app.domain.usecase

import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.domain.repository.ReminderRepository
import com.reminder.app.domain.time.TimeProvider
import com.reminder.app.scheduler.ReminderScheduler
import javax.inject.Inject

/**
 * Edits title and/or trigger time of a PENDING reminder.
 *
 * If the time changes we must replace the alarm:
 * 1. Update Room (source of truth — reboot recovery will use the new time).
 * 2. Cancel the old PendingIntent.
 * 3. Schedule a new one with the same requestCode (the reminder id).
 *
 * FLAG_UPDATE_CURRENT would also overwrite extras, but setExact* uses the new
 * trigger timestamp only when we call schedule() again. Cancel-then-schedule is
 * explicit and matches the mental model.
 */
class UpdateReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val timeProvider: TimeProvider
) {
    /**
     * @param id existing Room id
     * @param title new title
     * @param triggerTimeMillis new local wall-clock epoch millis; must be in the future
     */
    suspend operator fun invoke(
        id: Long,
        title: String,
        triggerTimeMillis: Long
    ): UpdateReminderResult {
        val existing = repository.getReminder(id) ?: return UpdateReminderResult.NotFound
        if (existing.status != ReminderStatus.PENDING) {
            return UpdateReminderResult.NotPending
        }

        val trimmed = title.trim()
        if (trimmed.isBlank()) return UpdateReminderResult.BlankTitle
        if (triggerTimeMillis <= timeProvider.now()) return UpdateReminderResult.TimeInPast

        val updated = existing.copy(title = trimmed, triggerTimeMillis = triggerTimeMillis)
        repository.update(updated)
        scheduler.cancel(existing)
        scheduler.schedule(updated)
        return UpdateReminderResult.Success(updated)
    }
}

/** Outcome of edit. Failures leave Room and the existing alarm unchanged. */
sealed interface UpdateReminderResult {
    data class Success(val reminder: Reminder) : UpdateReminderResult
    data object NotFound : UpdateReminderResult
    data object NotPending : UpdateReminderResult
    data object BlankTitle : UpdateReminderResult
    data object TimeInPast : UpdateReminderResult
}
